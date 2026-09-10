import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, MoreThanOrEqual } from 'typeorm';
import { BlockedAttempt } from './entities/blocked-attempt.entity';

@Injectable()
export class StatsService {
  constructor(
    @InjectRepository(BlockedAttempt)
    private attemptRepo: Repository<BlockedAttempt>,
  ) {}

  async recordBlockedAttempt(dto: { deviceId: string; domain: string }) {
    return this.attemptRepo.save(
      this.attemptRepo.create({
        deviceId: dto.deviceId,
        domain: dto.domain.toLowerCase(),
      }),
    );
  }

  async getSummary(deviceId: string) {
    const dayAgo = new Date(Date.now() - 24 * 60 * 60 * 1000);
    const [total, last24h, top] = await Promise.all([
      this.attemptRepo.count({ where: { deviceId } }),
      this.attemptRepo.count({
        where: { deviceId, timestamp: MoreThanOrEqual(dayAgo) },
      }),
      this.attemptRepo
        .createQueryBuilder('a')
        .select('a.domain', 'domain')
        .addSelect('COUNT(*)', 'count')
        .where('a.deviceId = :deviceId', { deviceId })
        .groupBy('a.domain')
        .orderBy('count', 'DESC')
        .limit(10)
        .getRawMany(),
    ]);
    return { deviceId, total, last24h, topBlocked: top };
  }
}
