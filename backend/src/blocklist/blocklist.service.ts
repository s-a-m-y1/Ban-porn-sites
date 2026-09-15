import { Inject, Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { In, Repository } from 'typeorm';
import { CACHE_MANAGER } from '@nestjs/cache-manager';
import { Cache } from 'cache-manager';
import { Domain } from './entities/domain.entity';
import { BlocklistVersion } from './entities/blocklist-version.entity';

// P0-3: V1 active categories — shipped locally, backend filters per category.
// extensible for future violence/social/custom
export const ACTIVE_CATEGORIES = ['adult-content', 'porn', 'gambling'] as const;
export type BlockCategory = (typeof ACTIVE_CATEGORIES)[number];

@Injectable()
export class BlocklistService {
  constructor(
    @InjectRepository(Domain)
    private domainRepo: Repository<Domain>,
    @InjectRepository(BlocklistVersion)
    private versionRepo: Repository<BlocklistVersion>,
    @Inject(CACHE_MANAGER) private cacheManager: Cache,
  ) {}

  async getAllActiveDomains(categories?: string[]): Promise<string[]> {
    const cats = normalizeCategories(categories);
    const where: Record<string, unknown> = { active: true };
    if (cats) (where as Record<string, unknown>)['category'] = In(cats);
    const domains = await this.domainRepo.find({
      where: where as never,
      select: { domain: true },
    });
    return domains.map((d) => d.domain);
  }

  async getCurrentVersion(): Promise<string> {
    const latest = await this.versionRepo.find({
      order: { updatedAt: 'DESC' },
      take: 1,
    });
    return latest[0]?.version ?? '0.0.0';
  }

  async getDomainsAddedSince(version: string, categories?: string[]): Promise<string[]> {
    const since = await this.versionRepo.find({
      where: { version },
      order: { updatedAt: 'DESC' },
      take: 1,
    });
    if (since.length === 0) {
      return this.getAllActiveDomains(categories);
    }
    const qb = this.domainRepo
      .createQueryBuilder('d')
      .where('d.addedAt > :since', { since: since[0].updatedAt })
      .andWhere('d.active = :active', { active: true });
    const cats = normalizeCategories(categories);
    if (cats) qb.andWhere('d.category IN (:...cats)', { cats });
    const domains = await qb.getMany();
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
    const latest = await this.versionRepo.find({
      order: { updatedAt: 'DESC' },
      take: 1,
    });
    const parts = (latest[0]?.version ?? '0.0.0').split('.').map(Number);
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

function normalizeCategories(cats?: string[]): string[] | undefined {
  if (!cats || cats.length === 0) return undefined;
  const allowed = new Set(ACTIVE_CATEGORIES as readonly string[]);
  const filtered = cats.map((c) => c.trim().toLowerCase()).filter((c) => allowed.has(c));
  return filtered.length > 0 ? filtered : undefined;
}
