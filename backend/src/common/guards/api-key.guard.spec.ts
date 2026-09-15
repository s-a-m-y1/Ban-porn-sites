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

  it('FAILS CLOSED when API_KEY is unset — missing server config must not authorize (P0-1 fix)', () => {
    delete process.env.API_KEY;
    expect(() => guard.canActivate(makeContext({}))).toThrow(UnauthorizedException);
    expect(() => guard.canActivate(makeContext({}))).toThrow(
      'Server misconfigured',
    );
  });

  it('FAILS CLOSED when API_KEY is the insecure placeholder', () => {
    process.env.API_KEY = 'change-me-in-production';
    expect(() => guard.canActivate(makeContext({ 'x-api-key': 'change-me-in-production' }))).toThrow(
      'Server misconfigured',
    );
  });

  it('rejects a non-string header value (array) even when it contains the key', () => {
    expect(() =>
      guard.canActivate(makeContext({ 'x-api-key': ['secret-key'] })),
    ).toThrow(UnauthorizedException);
  });
});
