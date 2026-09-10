import { Controller, Get, Param, Post, Body, UseGuards } from '@nestjs/common';
import { Throttle } from '@nestjs/throttler';
import { ApiKeyGuard } from '../common/guards/api-key.guard';
import { StatsService } from './stats.service';
import { ReportBlockedAttemptDto } from './dto/report-blocked-attempt.dto';

@Controller('stats')
export class StatsController {
  constructor(private readonly statsService: StatsService) {}

  @Post('blocked')
  @Throttle({ default: { limit: 120, ttl: 60_000 } })
  reportBlocked(@Body() dto: ReportBlockedAttemptDto) {
    return this.statsService.recordBlockedAttempt(dto);
  }

  @Get('summary/:deviceId')
  @UseGuards(ApiKeyGuard)
  getSummary(@Param('deviceId') deviceId: string) {
    return this.statsService.getSummary(deviceId);
  }
}
