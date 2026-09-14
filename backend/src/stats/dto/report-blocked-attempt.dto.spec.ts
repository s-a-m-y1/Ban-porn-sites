import { validate } from 'class-validator';
import { plainToInstance } from 'class-transformer';
import { ReportBlockedAttemptDto } from './report-blocked-attempt.dto';

describe('ReportBlockedAttemptDto', () => {
  const valid = { deviceId: 'device-abc', domain: 'example.com' };

  it('accepts a fully valid payload', async () => {
    const dto = plainToInstance(ReportBlockedAttemptDto, valid);
    const errors = await validate(dto);
    expect(errors).toHaveLength(0);
  });

  describe('deviceId', () => {
    it('is required', async () => {
      const errors = await validate(
        plainToInstance(ReportBlockedAttemptDto, { domain: 'example.com' } as never),
      );
      expect(errors.map((e) => e.property)).toContain('deviceId');
    });

    it('rejects empty strings', async () => {
      const errors = await validate(
        plainToInstance(ReportBlockedAttemptDto, { ...valid, deviceId: '' }),
      );
      expect(errors.map((e) => e.property)).toContain('deviceId');
      expect(errors.find((e) => e.property === 'deviceId')?.constraints)
        .toHaveProperty('isNotEmpty');
    });

    it('rejects non-string values', async () => {
      const errors = await validate(
        plainToInstance(ReportBlockedAttemptDto, { ...valid, deviceId: 42 }),
      );
      expect(errors.map((e) => e.property)).toContain('deviceId');
      expect(errors.find((e) => e.property === 'deviceId')?.constraints)
        .toHaveProperty('isString');
    });

    it('rejects values longer than 64 characters', async () => {
      const errors = await validate(
        plainToInstance(ReportBlockedAttemptDto, { ...valid, deviceId: 'x'.repeat(65) }),
      );
      expect(errors.map((e) => e.property)).toContain('deviceId');
      expect(errors.find((e) => e.property === 'deviceId')?.constraints)
        .toHaveProperty('maxLength');
    });

    it('accepts exactly 64 characters', async () => {
      const errors = await validate(
        plainToInstance(ReportBlockedAttemptDto, { ...valid, deviceId: 'x'.repeat(64) }),
      );
      expect(errors.map((e) => e.property)).not.toContain('deviceId');
    });
  });

  describe('domain', () => {
    it('is required', async () => {
      const errors = await validate(
        plainToInstance(ReportBlockedAttemptDto, { deviceId: 'device-abc' } as never),
      );
      expect(errors.map((e) => e.property)).toContain('domain');
    });

    it('rejects empty strings', async () => {
      const errors = await validate(
        plainToInstance(ReportBlockedAttemptDto, { ...valid, domain: '' }),
      );
      expect(errors.map((e) => e.property)).toContain('domain');
      expect(errors.find((e) => e.property === 'domain')?.constraints)
        .toHaveProperty('isNotEmpty');
    });

    it('rejects non-string values', async () => {
      const errors = await validate(
        plainToInstance(ReportBlockedAttemptDto, { ...valid, domain: { host: 'x' } }),
      );
      expect(errors.map((e) => e.property)).toContain('domain');
      expect(errors.find((e) => e.property === 'domain')?.constraints)
        .toHaveProperty('isString');
    });
  });
});
