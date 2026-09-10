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
      const url = process.env.DATABASE_URL ?? 'postgresql://blocklist:blocklist@localhost:5432/blocklist_db';
      return {
        ...parseDatabaseUrl(url),
        autoLoadEntities: true,
        synchronize: process.env.NODE_ENV !== 'production',
      };
    },
  };
}
