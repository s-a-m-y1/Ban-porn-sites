import 'reflect-metadata';
import { CacheInterceptor } from '@nestjs/cache-manager';
import { BlocklistController } from './blocklist.controller';
import { BlocklistService } from './blocklist.service';

describe('BlocklistController', () => {
  let controller: BlocklistController;
  let blocklistService: { getAllActiveDomains: jest.Mock; getCurrentVersion: jest.Mock; getDomainsAddedSince: jest.Mock };

  beforeEach(() => {
    blocklistService = {
      getAllActiveDomains: jest.fn(),
      getCurrentVersion: jest.fn(),
      getDomainsAddedSince: jest.fn(),
    };
    controller = new BlocklistController(
      blocklistService as unknown as BlocklistService,
    );
  });

  describe('delegation', () => {
    it('getBlocklist proxies to getAllActiveDomains', async () => {
      blocklistService.getAllActiveDomains.mockResolvedValue(['a.com']);

      await expect(controller.getBlocklist()).resolves.toEqual(['a.com']);
      expect(blocklistService.getAllActiveDomains).toHaveBeenCalledTimes(1);
    });

    it('getVersion proxies to getCurrentVersion', async () => {
      blocklistService.getCurrentVersion.mockResolvedValue('1.2.3');

      await expect(controller.getVersion()).resolves.toBe('1.2.3');
      expect(blocklistService.getCurrentVersion).toHaveBeenCalledTimes(1);
    });

    it('getDiff forwards the since query param', async () => {
      blocklistService.getDomainsAddedSince.mockResolvedValue(['new.com']);

      await expect(controller.getDiff('2.0.0')).resolves.toEqual(['new.com']);
      expect(blocklistService.getDomainsAddedSince).toHaveBeenCalledWith('2.0.0');
    });

    it("getDiff defaults since to '0.0.0' when not provided", async () => {
      blocklistService.getDomainsAddedSince.mockResolvedValue([]);

      await expect(controller.getDiff(undefined as never)).resolves.toEqual([]);
      expect(blocklistService.getDomainsAddedSince).toHaveBeenCalledWith('0.0.0');
    });
  });

  describe('route metadata', () => {
    it('is mounted on the blocklist path', () => {
      expect(Reflect.getMetadata('path', BlocklistController)).toBe('blocklist');
    });

    it('exposes GET /blocklist (cacheable for 1h)', () => {
      expect(Reflect.getMetadata('method', BlocklistController.prototype.getBlocklist))
        .toBe(0); // RequestMethod.GET
      expect(Reflect.getMetadata('path', BlocklistController.prototype.getBlocklist))
        .toBe('/');
      expect(Reflect.getMetadata('cache_module:cache_ttl', BlocklistController.prototype.getBlocklist))
        .toBe(3600_000);
    });

    it('exposes GET /blocklist/version (cacheable for 5m)', () => {
      expect(Reflect.getMetadata('method', BlocklistController.prototype.getVersion))
        .toBe(0);
      expect(Reflect.getMetadata('path', BlocklistController.prototype.getVersion))
        .toBe('version');
      expect(Reflect.getMetadata('cache_module:cache_ttl', BlocklistController.prototype.getVersion))
        .toBe(300_000);
    });

    it('exposes GET /blocklist/diff without caching', () => {
      expect(Reflect.getMetadata('method', BlocklistController.prototype.getDiff))
        .toBe(0);
      expect(Reflect.getMetadata('path', BlocklistController.prototype.getDiff))
        .toBe('diff');
      expect(
        Reflect.getMetadata('cache_module:cache_ttl', BlocklistController.prototype.getDiff),
      ).toBeUndefined();
    });

    it('applies the cache interceptor to the cacheable routes only', () => {
      const getBlocklistInterceptors = Reflect.getMetadata(
        '__interceptors__',
        BlocklistController.prototype.getBlocklist,
      );
      const getVersionInterceptors = Reflect.getMetadata(
        '__interceptors__',
        BlocklistController.prototype.getVersion,
      );
      const getDiffInterceptors = Reflect.getMetadata(
        '__interceptors__',
        BlocklistController.prototype.getDiff,
      );

      expect(getBlocklistInterceptors).toEqual([CacheInterceptor]);
      expect(getVersionInterceptors).toEqual([CacheInterceptor]);
      expect(getDiffInterceptors).toBeUndefined();
    });
  });
});
