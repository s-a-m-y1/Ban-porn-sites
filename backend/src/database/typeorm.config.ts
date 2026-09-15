import { TypeOrmModuleAsyncOptions } from '@nestjs/typeorm';

export function parseDatabaseUrl(url: string) {
  const parsed = new URL(url);
  return {
    type: 'postgres' as const,
    host: parsed.hostname,
    port: Number(parsed.port || 5432),
    username: decodeURIComponent(parsed.username),
    password: decodeURIComponent(parsed.password),
    database: parsed.pathname.replace(/^\//, ''),
  };
}

export function getTypeOrmConfig(): TypeOrmModuleAsyncOptions {
  return {
    useFactory: () => {
      const url = process.env.DATABASE_URL;
      if (!url) {
        if (process.env.NODE_ENV === 'production') {
          throw new Error('DATABASE_URL must be set in production');
        }
        // Dev/test fallback with warning — never in production (P0-1)
        console.warn('[typeorm] DATABASE_URL not set — using local dev fallback');
      }
      const effectiveUrl =
        url ?? 'postgresql://blocklist:blocklist@localhost:5432/blocklist_db';
      return {
        ...parseDatabaseUrl(effectiveUrl),
        autoLoadEntities: true,
        // P0-1: Never auto-synchronize — use migrations. Was `!== 'production'` which is unsafe.
        synchronize: false,
        migrationsRun: false,
      };
    },
  };
}
