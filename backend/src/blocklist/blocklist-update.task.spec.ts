import { BlocklistUpdateTask } from './blocklist-update.task';
import { BlocklistService } from './blocklist.service';
import type { ConfigService } from '@nestjs/config';

describe('BlocklistUpdateTask', () => {
  let task: BlocklistUpdateTask;
  let config: { get: jest.Mock };
  let blocklistService: {
    mergeDomains: jest.Mock;
    incrementVersion: jest.Mock;
    invalidateCache: jest.Mock;
  };

  const HOSTS_FILE = [
    '# Comment line',
    '0.0.0.0 ads.example.com',
    '0.0.0.0 tracker.net',
    'plain-domain.org',
    '127.0.0.1 localhost',
    '#',
    '',
    '   ',
    '  0.0.0.0    spaced.io  ',
  ].join('\n');

  const parse = (text: string): string[] =>
    (task as unknown as { parseHostsFile: (t: string) => string[] }).parseHostsFile(text);

  beforeEach(() => {
    config = { get: jest.fn() };
    blocklistService = {
      mergeDomains: jest.fn(),
      incrementVersion: jest.fn(),
      invalidateCache: jest.fn(),
    };
    task = new BlocklistUpdateTask(
      config as unknown as ConfigService,
      blocklistService as unknown as BlocklistService,
    );
  });

  describe('parseHostsFile', () => {
    it('extracts hostnames, drops comments, empty lines, non-domains and raw IPs', () => {
      expect(parse(HOSTS_FILE)).toEqual([
        'ads.example.com',
        'tracker.net',
        'plain-domain.org',
        'spaced.io',
      ]);
    });

    it('keeps the domain token when a line has a single entry', () => {
      expect(parse('single.io')).toEqual(['single.io']);
    });

    it('drops pure IP entries', () => {
      expect(parse('10.0.0.1')).toEqual([]);
    });

    it('drops entries without a dot', () => {
      expect(parse('localhost')).toEqual([]);
    });

    it('returns an empty array for empty input', () => {
      expect(parse('')).toEqual([]);
    });
  });

  describe('updateFromExternalSource', () => {
    let fetchMock: jest.Mock;
    let loggerErrorSpy: jest.SpyInstance;
    let loggerLogSpy: jest.SpyInstance;

    const response = (ok: boolean, status: number, text: string) =>
      ({ ok, status, text: async () => text }) as unknown as Response;

    beforeEach(() => {
      fetchMock = jest.fn();
      global.fetch = fetchMock as unknown as typeof fetch;
      // The task holds its own Logger instance; intercept console output at the
      // Logger prototype level so nothing leaks to the console.
      loggerErrorSpy = jest
        .spyOn(require('@nestjs/common').Logger.prototype, 'error')
        .mockImplementation(() => undefined);
      loggerLogSpy = jest
        .spyOn(require('@nestjs/common').Logger.prototype, 'log')
        .mockImplementation(() => undefined);
    });

    afterEach(() => {
      loggerErrorSpy.mockRestore();
      loggerLogSpy.mockRestore();
      jest.restoreAllMocks();
    });

    it('does nothing when no source is configured', async () => {
      config.get.mockReturnValue(undefined);

      await task.updateFromExternalSource();

      expect(fetchMock).not.toHaveBeenCalled();
      expect(blocklistService.mergeDomains).not.toHaveBeenCalled();
    });

    it('fetches, parses, merges, bumps the version and invalidates the cache', async () => {
      config.get.mockReturnValue('https://example.test/hosts');
      fetchMock.mockResolvedValue(response(true, 200, '0.0.0.0 fresh.example.com'));
      blocklistService.mergeDomains.mockResolvedValue({ added: 1, skipped: 0 });
      blocklistService.incrementVersion.mockResolvedValue('0.0.2');
      blocklistService.invalidateCache.mockResolvedValue(undefined);

      await task.updateFromExternalSource();

      expect(fetchMock).toHaveBeenCalledWith('https://example.test/hosts');
      expect(blocklistService.mergeDomains).toHaveBeenCalledWith(['fresh.example.com']);
      expect(blocklistService.incrementVersion).toHaveBeenCalledTimes(1);
      expect(blocklistService.invalidateCache).toHaveBeenCalledTimes(1);
      expect(loggerLogSpy).toHaveBeenCalled();
    });

    it('logs an error instead of throwing when the HTTP request fails', async () => {
      config.get.mockReturnValue('https://example.test/hosts');
      fetchMock.mockResolvedValue(response(false, 503, ''));

      await expect(task.updateFromExternalSource()).resolves.toBeUndefined();

      expect(blocklistService.mergeDomains).not.toHaveBeenCalled();
      expect(loggerErrorSpy).toHaveBeenCalledWith(
        expect.stringContaining('HTTP 503'),
      );
    });

    it('logs an error when fetch itself rejects', async () => {
      config.get.mockReturnValue('https://example.test/hosts');
      fetchMock.mockRejectedValue(new Error('network down'));

      await expect(task.updateFromExternalSource()).resolves.toBeUndefined();

      expect(loggerErrorSpy).toHaveBeenCalledWith(
        expect.stringContaining('network down'),
      );
    });

    it('logs an error when mergeDomains rejects', async () => {
      config.get.mockReturnValue('https://example.test/hosts');
      fetchMock.mockResolvedValue(response(true, 200, '0.0.0.0 x.com'));
      blocklistService.mergeDomains.mockRejectedValue(new Error('db unavailable'));

      await expect(task.updateFromExternalSource()).resolves.toBeUndefined();

      expect(blocklistService.incrementVersion).not.toHaveBeenCalled();
      expect(loggerErrorSpy).toHaveBeenCalledWith(
        expect.stringContaining('db unavailable'),
      );
    });
  });
});
