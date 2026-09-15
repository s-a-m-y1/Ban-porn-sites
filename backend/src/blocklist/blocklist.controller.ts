import {
  Controller,
  Get,
  Query,
  UseInterceptors,
} from '@nestjs/common';
import { CacheInterceptor, CacheTTL } from '@nestjs/cache-manager';
import { BlocklistService } from './blocklist.service';

@Controller('blocklist')
export class BlocklistController {
  constructor(private readonly blocklistService: BlocklistService) {}

  @Get()
  @UseInterceptors(CacheInterceptor)
  @CacheTTL(3600_000)
  getBlocklist(@Query('categories') categories?: string): Promise<string[]> {
    const cats = categories?.split(',').map((s) => s.trim()).filter(Boolean);
    return this.blocklistService.getAllActiveDomains(cats);
  }

  @Get('version')
  @UseInterceptors(CacheInterceptor)
  @CacheTTL(300_000)
  getVersion(): Promise<string> {
    return this.blocklistService.getCurrentVersion();
  }

  @Get('diff')
  getDiff(
    @Query('since') since: string,
    @Query('categories') categories?: string,
  ): Promise<string[]> {
    const cats = categories?.split(',').map((s) => s.trim()).filter(Boolean);
    return this.blocklistService.getDomainsAddedSince(since ?? '0.0.0', cats);
  }
}
