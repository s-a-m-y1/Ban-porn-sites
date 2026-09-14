import { StatsService } from './stats.service';
import { BlockedAttempt } from './entities/blocked-attempt.entity';
import { type Repository } from 'typeorm';

describe('StatsService', () => {
  let service: StatsService;
  let attemptRepo: jest.Mocked<Repository<BlockedAttempt>>;

  const makeQueryBuilder = (result: unknown) => ({
    select: jest.fn().mockReturnThis(),
    addSelect: jest.fn().mockReturnThis(),
    where: jest.fn().mockReturnThis(),
    groupBy: jest.fn().mockReturnThis(),
    orderBy: jest.fn().mockReturnThis(),
    limit: jest.fn().mockReturnThis(),
    getRawMany: jest.fn().mockResolvedValue(result),
  });

  beforeEach(() => {
    attemptRepo = {
      create: jest.fn(),
      save: jest.fn(),
      count: jest.fn(),
      createQueryBuilder: jest.fn(),
    } as unknown as jest.Mocked<Repository<BlockedAttempt>>;

    service = new StatsService(attemptRepo);
  });

  describe('recordBlockedAttempt', () => {
    it('persists the attempt with a lowercased domain', async () => {
      const created: BlockedAttempt = {
        deviceId: 'device-1',
        domain: 'example.com',
      } as BlockedAttempt;
      attemptRepo.create.mockReturnValue(created);
      attemptRepo.save.mockResolvedValue(created);

      const result = await service.recordBlockedAttempt({
        deviceId: 'device-1',
        domain: 'Example.COM',
      });

      expect(attemptRepo.create).toHaveBeenCalledWith({
        deviceId: 'device-1',
        domain: 'example.com',
      });
      expect(attemptRepo.save).toHaveBeenCalledWith(created);
      expect(result).toBe(created);
    });

    it('leaves already lowercase domains untouched', async () => {
      const created = { deviceId: 'd1', domain: 'lower.net' } as BlockedAttempt;
      attemptRepo.create.mockReturnValue(created);
      attemptRepo.save.mockResolvedValue(created);

      await service.recordBlockedAttempt({ deviceId: 'd1', domain: 'lower.net' });

      expect(attemptRepo.create).toHaveBeenCalledWith({
        deviceId: 'd1',
        domain: 'lower.net',
      });
    });
  });

  describe('getSummary', () => {
    it('combines the total count, the 24h count and the top blocked domains', async () => {
      const top = [
        { domain: 'a.com', count: 7 },
        { domain: 'b.com', count: 3 },
      ];
      const qb = makeQueryBuilder(top);
      attemptRepo.createQueryBuilder.mockReturnValue(qb as never);
      attemptRepo.count
        .mockResolvedValueOnce(42)
        .mockResolvedValueOnce(5);

      const before = Date.now();
      const result = await service.getSummary('device-9');
      const after = Date.now();

      expect(result).toEqual({
        deviceId: 'device-9',
        total: 42,
        last24h: 5,
        topBlocked: top,
      });

      // Total count query
      expect(attemptRepo.count).toHaveBeenNthCalledWith(1, {
        where: { deviceId: 'device-9' },
      });

      // 24h count query: deviceId plus a MoreThanOrEqual window ending ~now
      expect(attemptRepo.count).toHaveBeenNthCalledWith(2, {
        where: {
          deviceId: 'device-9',
          timestamp: expect.anything(),
        },
      });
      const secondCallArg = attemptRepo.count.mock.calls[1][0] as {
        where: { timestamp: { type: string; value: Date } };
      };
      expect(secondCallArg.where.timestamp.type).toBe('moreThanOrEqual');
      const threshold: Date = secondCallArg.where.timestamp.value;
      expect(threshold.getTime()).toBeGreaterThanOrEqual(before - 24 * 60 * 60 * 1000);
      expect(threshold.getTime()).toBeLessThanOrEqual(after - 24 * 60 * 60 * 1000);

      // Top blocked query builder chain
      expect(attemptRepo.createQueryBuilder).toHaveBeenCalledWith('a');
      expect(qb.select).toHaveBeenCalledWith('a.domain', 'domain');
      expect(qb.addSelect).toHaveBeenCalledWith('COUNT(*)', 'count');
      expect(qb.where).toHaveBeenCalledWith('a.deviceId = :deviceId', { deviceId: 'device-9' });
      expect(qb.groupBy).toHaveBeenCalledWith('a.domain');
      expect(qb.orderBy).toHaveBeenCalledWith('count', 'DESC');
      expect(qb.limit).toHaveBeenCalledWith(10);
      expect(qb.getRawMany).toHaveBeenCalledTimes(1);
    });

    it('returns zero counts and an empty leaderboard for a new device', async () => {
      const qb = makeQueryBuilder([]);
      attemptRepo.createQueryBuilder.mockReturnValue(qb as never);
      attemptRepo.count.mockResolvedValue(0);

      const result = await service.getSummary('fresh-device');

      expect(result).toEqual({
        deviceId: 'fresh-device',
        total: 0,
        last24h: 0,
        topBlocked: [],
      });
    });
  });
});
