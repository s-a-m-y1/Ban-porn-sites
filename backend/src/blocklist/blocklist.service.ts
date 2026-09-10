import { Inject, Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { CACHE_MANAGER } from '@nestjs/cache-manager';
import { Cache } from 'cache-manager';
import { Domain } from './entities/domain.entity';
import { BlocklistVersion } from './entities/blocklist-version.entity';

@Injectable()
export class BlocklistService {
  constructor(
    @InjectRepository(Domain)
    private domainRepo: Repository<Domain>,
    @InjectRepository(BlocklistVersion)
    private versionRepo: Repository<BlocklistVersion>,
    @Inject(CACHE_MANAGER) private cacheManager: Cache,
  ) {}

  async getAllActiveDomains(): Promise<string[]> {
    const domains = await this.domainRepo.find({
      where: { active: true },
      select: { domain: true },
    });
    return domains.map((d) => d.domain);
  }

  async getCurrentVersion(): Promise<string> {
    const latest = await this.versionRepo.findOne({
      order: { updatedAt: 'DESC' },
    });
    return latest?.version ?? '0.0.0';
  }

  async getDomainsAddedSince(version: string): Promise<string[]> {
    const since = await this.versionRepo.findOne({
      where: { version },
      order: { updatedAt: 'DESC' },
    });
    if (!since) {
      return this.getAllActiveDomains();
    }
    const domains = await this.domainRepo
      .createQueryBuilder('d')
      .where('d.addedAt > :since', { since: since.updatedAt })
      .andWhere('d.active = :active', { active: true })
      .getMany();
    return domains.map((d) => d.domain);
  }

  async mergeDomains(
    rawDomains: string[],
    category = 'adult-content',
  ): Promise<{ added: number; skipped: number }> {
    const cleaned = [...new Set(
      rawDomains
        .map((d) => d.trim().toLowerCase())
        .filter((d) => d.length > 0 && d.includes('.')),
    )];
    if (cleaned.length === 0) return { added: 0, skipped: 0 };

    const existing = await this.domainRepo.find({
      where: cleaned.map((domain) => ({ domain })),
    });
    const existingSet = new Set(existing.map((d) => d.domain));
    const toInsert = cleaned
      .filter((d) => !existingSet.has(d))
      .map((domain) => this.domainRepo.create({ domain, category }));

    if (toInsert.length > 0) {
      await this.domainRepo.save(toInsert);
    }
    return { added: toInsert.length, skipped: cleaned.length - toInsert.length };
  }

  async incrementVersion(): Promise<string> {
    const latest = await this.versionRepo.findOne({
      order: { updatedAt: 'DESC' },
    });
    const parts = (latest?.version ?? '0.0.0').split('.').map(Number);
    parts[2] = (parts[2] || 0) + 1;
    const version = parts.join('.');
    await this.versionRepo.save(this.versionRepo.create({ version }));
    return version;
  }

  async invalidateCache(): Promise<void> {
    await this.cacheManager.del('blocklist');
    await this.cacheManager.del('blocklist/version');
  }
}
