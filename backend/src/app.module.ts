import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ScheduleModule } from '@nestjs/schedule';
import { ThrottlerModule, ThrottlerGuard } from '@nestjs/throttler';
import { APP_GUARD } from '@nestjs/core';
import { getTypeOrmConfig } from './database/typeorm.config';
import { RedisCacheModule } from './cache/redis.module';
import { BlocklistModule } from './blocklist/blocklist.module';
import { StatsModule } from './stats/stats.module';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    TypeOrmModule.forRootAsync(getTypeOrmConfig()),
    ScheduleModule.forRoot(),
    ThrottlerModule.forRoot([
      { ttl: 60_000, limit: 30 },
    ]),
    RedisCacheModule,
    BlocklistModule,
    StatsModule,
  ],
  providers: [
    { provide: APP_GUARD, useClass: ThrottlerGuard },
  ],
})
export class AppModule {}
