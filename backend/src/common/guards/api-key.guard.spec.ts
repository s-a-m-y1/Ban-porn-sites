import { ExecutionContext, UnauthorizedException } from '@nestjs/common';
import { ApiKeyGuard } from './api-key.guard';

describe('ApiKeyGuard', () => {
  let guard: ApiKeyGuard;
  let originalApiKey: string | undefined;

  const makeContext = (headers: Record<string, unknown>): ExecutionContext => {
    const request = { headers };
    return {
      switchToHttp: () => ({ getRequest: () => request }),
    } as unknown as ExecutionContext;
  };

  beforeEach(() => {
    guard = new ApiKeyGuard();
    originalApiKey = process.env.API_KEY;
    process.env.API_KEY = 'secret-key';
  });

  afterEach(() => {
    if (originalApiKey === undefined) {
      delete process.env.API_KEY;
    } else {
      process.env.API_KEY = originalApiKey;
    }
  });

  it('allows the request when x-api-key matches the configured API_KEY', () => {
    expect(guard.canActivate(makeContext({ 'x-api-key': 'secret-key' }))).toBe(true);
  });

  it('rejects a wrong api key with UnauthorizedException', () => {
    expect(() =>
      guard.canActivate(makeContext({ 'x-api-key': 'wrong' })),
    ).toThrow(UnauthorizedException);
    expect(() =>
      guard.canActivate(makeContext({ 'x-api-key': 'wrong' })),
    ).toThrow('Invalid API key');
  });

  it('rejects a missing x-api-key header', () => {
    expect(() => guard.canActivate(makeContext({}))).toThrow(UnauthorizedException);
  });

  it('is case-sensitive on the header value', () => {
    expect(() =>
      guard.canActivate(makeContext({ 'x-api-key': 'SECRET-KEY' })),
    ).toThrow(UnauthorizedException);
  });

  it('does not accept a different header name carrying the correct key', () => {
    expect(() =>
      guard.canActivate(makeContext({ authorization: 'secret-key' })),
    ).toThrow(UnauthorizedException);
  });

  it('currently ALLOWS the request when API_KEY is unset and no header is sent (known production bug — see handoff)', () => {
    // KNOWN PRODUCTION BUG (documented, not fixed — specs may not touch production code):
    // `request.headers['x-api-key'] === process.env.API_KEY` evaluates to
    // `undefined === undefined` -> true, so an unconfigured server authorizes
    // requests that carry no API key at all.
    delete process.env.API_KEY;
    expect(guard.canActivate(makeContext({}))).toBe(true);
  });

  it('rejects a non-string header value (array) even when it contains the key', () => {
    expect(() =>
      guard.canActivate(makeContext({ 'x-api-key': ['secret-key'] })),
    ).toThrow(UnauthorizedException);
  });
});
