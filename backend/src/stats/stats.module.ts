import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { StatsController } from './stats.controller';
import { StatsService } from './stats.service';
import { BlockedAttempt } from './entities/blocked-attempt.entity';

@Module({
  imports: [TypeOrmModule.forFeature([BlockedAttempt])],
  controllers: [StatsController],
  providers: [StatsService],
})
export class StatsModule {}
