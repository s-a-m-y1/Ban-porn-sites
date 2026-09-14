import { BlocklistService } from './blocklist.service';
import { Domain } from './entities/domain.entity';
import { BlocklistVersion } from './entities/blocklist-version.entity';
import type { Repository } from 'typeorm';
import type { Cache } from 'cache-manager';

describe('BlocklistService', () => {
  let service: BlocklistService;
  let domainRepo: jest.Mocked<Repository<Domain>>;
  let versionRepo: jest.Mocked<Repository<BlocklistVersion>>;
  let cacheManager: { del: jest.Mock };

  const makeDomain = (domain: string): Domain =>
    ({ domain } as unknown as Domain);

  const makeVersion = (version: string): BlocklistVersion =>
    ({ version } as unknown as BlocklistVersion);

  const makeQueryBuilder = (result: unknown) => ({
    where: jest.fn().mockReturnThis(),
    andWhere: jest.fn().mockReturnThis(),
    getMany: jest.fn().mockResolvedValue(result),
  });

  beforeEach(() => {
    domainRepo = {
      find: jest.fn(),
      create: jest.fn(),
      save: jest.fn(),
      createQueryBuilder: jest.fn(),
    } as unknown as jest.Mocked<Repository<Domain>>;

    versionRepo = {
      find: jest.fn(),
      create: jest.fn(),
      save: jest.fn(),
    } as unknown as jest.Mocked<Repository<BlocklistVersion>>;

    cacheManager = { del: jest.fn().mockResolvedValue(undefined) };

    service = new BlocklistService(domainRepo, versionRepo, cacheManager as unknown as Cache);
  });

  describe('getAllActiveDomains', () => {
    it('queries only active domains and selects only the domain column', async () => {
      domainRepo.find.mockResolvedValue([makeDomain('example.com'), makeDomain('other.org')]);

      const result = await service.getAllActiveDomains();

      expect(domainRepo.find).toHaveBeenCalledWith({
        where: { active: true },
        select: { domain: true },
      });
      expect(result).toEqual(['example.com', 'other.org']);
    });

    it('returns an empty array when the table has no active domains', async () => {
      domainRepo.find.mockResolvedValue([]);

      const result = await service.getAllActiveDomains();

      expect(result).toEqual([]);
    });
  });

  describe('getCurrentVersion', () => {
    it('returns the most recently updated version', async () => {
      versionRepo.find.mockResolvedValue([makeVersion('1.2.3')]);

      const result = await service.getCurrentVersion();

      expect(versionRepo.find).toHaveBeenCalledWith({
        order: { updatedAt: 'DESC' },
        take: 1,
      });
      expect(result).toBe('1.2.3');
    });

    it("defaults to '0.0.0' when no version exists yet", async () => {
      versionRepo.find.mockResolvedValue([]);

      const result = await service.getCurrentVersion();

      expect(result).toBe('0.0.0');
    });
  });

  describe('getDomainsAddedSince', () => {
    it('returns all active domains when the given version is unknown', async () => {
      versionRepo.find.mockResolvedValue([]);
      domainRepo.find.mockResolvedValue([makeDomain('fallback.com')]);

      const result = await service.getDomainsAddedSince('9.9.9');

      expect(versionRepo.find).toHaveBeenCalledWith({
        where: { version: '9.9.9' },
        order: { updatedAt: 'DESC' },
        take: 1,
      });
      expect(domainRepo.createQueryBuilder).not.toHaveBeenCalled();
      expect(domainRepo.find).toHaveBeenCalledWith({
        where: { active: true },
        select: { domain: true },
      });
      expect(result).toEqual(['fallback.com']);
    });

    it('queries domains added after the timestamp of the known version', async () => {
      const updatedAt = new Date('2026-01-15T10:00:00Z');
      const versionWithDate = { version: '1.2.3', updatedAt } as unknown as BlocklistVersion;
      versionRepo.find.mockResolvedValue([versionWithDate]);

      const qb = makeQueryBuilder([makeDomain('new-a.com'), makeDomain('new-b.net')]);
      domainRepo.createQueryBuilder.mockReturnValue(qb as never);

      const result = await service.getDomainsAddedSince('1.2.3');

      expect(domainRepo.createQueryBuilder).toHaveBeenCalledWith('d');
      expect(qb.where).toHaveBeenCalledWith('d.addedAt > :since', { since: updatedAt });
      expect(qb.andWhere).toHaveBeenCalledWith('d.active = :active', { active: true });
      expect(result).toEqual(['new-a.com', 'new-b.net']);
    });
  });

  describe('mergeDomains', () => {
    it('cleans, deduplicates, and inserts only new domains', async () => {
      // Cleaned input: ['example.com', 'newsite.net']; 'example.com' already exists.
      domainRepo.find.mockResolvedValue([makeDomain('example.com')]);
      domainRepo.create.mockImplementation((data: Partial<Domain>) => data as Domain);
      // save's return value is discarded by the service; default mock suffices.

      const result = await service.mergeDomains([
        '  Example.COM  ',
        'example.com',
        'newsite.net',
        'notadomain',
        '   ',
        '',
      ]);

      expect(domainRepo.find).toHaveBeenCalledWith({
        where: [
          { domain: 'example.com' },
          { domain: 'newsite.net' },
        ],
      });
      expect(domainRepo.create).toHaveBeenCalledWith({ domain: 'newsite.net', category: 'adult-content' });
      expect(domainRepo.save).toHaveBeenCalledTimes(1);
      expect(result).toEqual({ added: 1, skipped: 1 });
    });

    it('supports a custom category', async () => {
      domainRepo.find.mockResolvedValue([]);
      domainRepo.create.mockImplementation((data: Partial<Domain>) => data as Domain);

      await service.mergeDomains(['custom.cat'], 'gambling');

      expect(domainRepo.create).toHaveBeenCalledWith({ domain: 'custom.cat', category: 'gambling' });
    });

    it('skips the repository entirely when nothing survives cleaning', async () => {
      const result = await service.mergeDomains(['nodot', '   ', '# comment']);

      expect(domainRepo.find).not.toHaveBeenCalled();
      expect(domainRepo.save).not.toHaveBeenCalled();
      expect(result).toEqual({ added: 0, skipped: 0 });
    });

    it('reports every existing domain as skipped and saves nothing', async () => {
      domainRepo.find.mockResolvedValue([makeDomain('old.com'), makeDomain('old.net')]);

      const result = await service.mergeDomains(['old.com', 'old.net']);

      expect(domainRepo.save).not.toHaveBeenCalled();
      expect(result).toEqual({ added: 0, skipped: 2 });
    });
  });

  describe('incrementVersion', () => {
    it('increments the patch segment of the latest version', async () => {
      versionRepo.find.mockResolvedValue([makeVersion('1.2.3')]);
      versionRepo.create.mockImplementation((data: Partial<BlocklistVersion>) => data as BlocklistVersion);

      const result = await service.incrementVersion();

      expect(versionRepo.create).toHaveBeenCalledWith({ version: '1.2.4' });
      expect(versionRepo.save).toHaveBeenCalledTimes(1);
      expect(result).toBe('1.2.4');
    });

    it("starts at '0.0.1' when there is no previous version", async () => {
      versionRepo.find.mockResolvedValue([]);
      versionRepo.create.mockImplementation((data: Partial<BlocklistVersion>) => data as BlocklistVersion);

      const result = await service.incrementVersion();

      expect(versionRepo.create).toHaveBeenCalledWith({ version: '0.0.1' });
      expect(result).toBe('0.0.1');
    });

    it('treats a missing patch segment as zero before incrementing', async () => {
      // '1.2' splits to [1, 2, undefined] -> parts[2] = 0 + 1 = 1 => '1.2.1'
      versionRepo.find.mockResolvedValue([makeVersion('1.2')]);
      versionRepo.create.mockImplementation((data: Partial<BlocklistVersion>) => data as BlocklistVersion);

      const result = await service.incrementVersion();

      expect(result).toBe('1.2.1');
    });
  });

  describe('invalidateCache', () => {
    it('deletes both blocklist cache keys', async () => {
      await service.invalidateCache();

      expect(cacheManager.del).toHaveBeenCalledWith('blocklist');
      expect(cacheManager.del).toHaveBeenCalledWith('blocklist/version');
      expect(cacheManager.del).toHaveBeenCalledTimes(2);
    });
  });
});
