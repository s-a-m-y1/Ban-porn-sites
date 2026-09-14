import 'reflect-metadata';
import { StatsController } from './stats.controller';
import { StatsService } from './stats.service';
import { ApiKeyGuard } from '../common/guards/api-key.guard';

describe('StatsController', () => {
  let controller: StatsController;
  let statsService: { recordBlockedAttempt: jest.Mock; getSummary: jest.Mock };

  beforeEach(() => {
    statsService = {
      recordBlockedAttempt: jest.fn(),
      getSummary: jest.fn(),
    };
    controller = new StatsController(statsService as unknown as StatsService);
  });

  describe('delegation', () => {
    it('reportBlocked forwards the validated body to the service', async () => {
      const dto = { deviceId: 'device-1', domain: 'example.com' };
      statsService.recordBlockedAttempt.mockResolvedValue(undefined);

      await controller.reportBlocked(dto);

      expect(statsService.recordBlockedAttempt).toHaveBeenCalledWith(dto);
    });

    it('getSummary forwards the deviceId route param', async () => {
      const summary = {
        deviceId: 'device-1',
        total: 10,
        last24h: 3,
        topBlocked: [],
      };
      statsService.getSummary.mockResolvedValue(summary);

      await expect(controller.getSummary('device-1')).resolves.toBe(summary);
      expect(statsService.getSummary).toHaveBeenCalledWith('device-1');
    });
  });

  describe('route metadata', () => {
    it('is mounted on the stats path', () => {
      expect(Reflect.getMetadata('path', StatsController)).toBe('stats');
    });

    it('exposes POST /stats/blocked with a rate limit of 120 per minute', () => {
      expect(Reflect.getMetadata('method', StatsController.prototype.reportBlocked))
        .toBe(1); // RequestMethod.POST
      expect(Reflect.getMetadata('path', StatsController.prototype.reportBlocked))
        .toBe('blocked');
      expect(Reflect.getMetadata('THROTTLER:LIMITdefault', StatsController.prototype.reportBlocked))
        .toBe(120);
      expect(Reflect.getMetadata('THROTTLER:TTLdefault', StatsController.prototype.reportBlocked))
        .toBe(60_000);
    });

    it('protects the summary endpoint with ApiKeyGuard', () => {
      expect(Reflect.getMetadata('method', StatsController.prototype.getSummary))
        .toBe(0); // RequestMethod.GET
      expect(Reflect.getMetadata('path', StatsController.prototype.getSummary))
        .toBe('summary/:deviceId');
      expect(Reflect.getMetadata('__guards__', StatsController.prototype.getSummary))
        .toEqual([ApiKeyGuard]);
    });

    it('does not require an api key to report blocked attempts', () => {
      expect(
        Reflect.getMetadata('__guards__', StatsController.prototype.reportBlocked),
      ).toBeUndefined();
    });
  });
});
