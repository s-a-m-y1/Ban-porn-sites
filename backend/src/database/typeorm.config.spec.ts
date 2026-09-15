import { parseDatabaseUrl, getTypeOrmConfig } from './typeorm.config';

describe('parseDatabaseUrl', () => {
  it('extracts every component from a full postgres URL', () => {
    const result = parseDatabaseUrl('postgresql://user:pass@db.host:6543/my_database');

    expect(result).toEqual({
      type: 'postgres',
      host: 'db.host',
      port: 6543,
      username: 'user',
      password: 'pass',
      database: 'my_database',
    });
  });

  it('falls back to port 5432 when the URL omits the port', () => {
    const result = parseDatabaseUrl('postgresql://u:p@localhost/db');
    expect(result.port).toBe(5432);
  });

  it('decodes percent-encoded credentials', () => {
    const result = parseDatabaseUrl(
      'postgresql://my%40user:p%40ss%2Fword@localhost/db',
    );
    expect(result.username).toBe('my@user');
    expect(result.password).toBe('p@ss/word');
  });

  it('strips the leading slash from the database path', () => {
    const result = parseDatabaseUrl('postgresql://u:p@localhost/prod_db');
    expect(result.database).toBe('prod_db');
  });

  it('defaults missing credentials to empty strings', () => {
    const result = parseDatabaseUrl('postgresql://localhost/db');
    expect(result.username).toBe('');
    expect(result.password).toBe('');
  });
});

describe('getTypeOrmConfig', () => {
  const originalDatabaseUrl = process.env.DATABASE_URL;
  const originalNodeEnv = process.env.NODE_ENV;

  afterEach(() => {
    if (originalDatabaseUrl === undefined) {
      delete process.env.DATABASE_URL;
    } else {
      process.env.DATABASE_URL = originalDatabaseUrl;
    }
    if (originalNodeEnv === undefined) {
      delete process.env.NODE_ENV;
    } else {
      process.env.NODE_ENV = originalNodeEnv;
    }
  });

  it('returns a dynamic config that reads DATABASE_URL at factory time', () => {
    const config = getTypeOrmConfig();

    expect(typeof config.useFactory).toBe('function');

    process.env.DATABASE_URL = 'postgresql://u:p@myhost:5433/from_env';
    const resolved = (config.useFactory as () => Record<string, unknown>)();

    expect(resolved).toMatchObject({
      host: 'myhost',
      port: 5433,
      database: 'from_env',
    });
  });

  it("falls back to the default URL when DATABASE_URL isn't set", () => {
    delete process.env.DATABASE_URL;

    const resolved = (getTypeOrmConfig().useFactory as () => Record<string, unknown>)();

    expect(resolved).toMatchObject({
      host: 'localhost',
      port: 5432,
      username: 'blocklist',
      password: 'blocklist',
      database: 'blocklist_db',
    });
  });

  it('disables synchronize outside production as well (P0-1: never auto-sync, use migrations)', () => {
    delete process.env.DATABASE_URL;
    process.env.NODE_ENV = 'development';

    const resolved = (getTypeOrmConfig().useFactory as () => Record<string, unknown>)();

    expect(resolved).toMatchObject({ synchronize: false, autoLoadEntities: true });
  });

  it('throws in production when DATABASE_URL is missing (P0-1 fail-fast)', () => {
    delete process.env.DATABASE_URL;
    process.env.NODE_ENV = 'production';

    expect(() => (getTypeOrmConfig().useFactory as () => Record<string, unknown>)()).toThrow(
      'DATABASE_URL must be set',
    );
  });

  it('disables synchronize in production with URL set', () => {
    process.env.DATABASE_URL = 'postgresql://u:p@prod:5432/db';
    process.env.NODE_ENV = 'production';

    const resolved = (getTypeOrmConfig().useFactory as () => Record<string, unknown>)();

    expect(resolved).toMatchObject({ synchronize: false });
  });

  it('uses the production URL when provided via env', () => {
    process.env.DATABASE_URL = 'postgresql://prod:prod@prod.example:5432/hisn';

    const resolved = (getTypeOrmConfig().useFactory as () => Record<string, unknown>)();

    expect(resolved).toMatchObject({
      host: 'prod.example',
      port: 5432,
      database: 'hisn',
    });
  });
});
