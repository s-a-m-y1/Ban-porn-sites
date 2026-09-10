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
  getBlocklist(): Promise<string[]> {
    return this.blocklistService.getAllActiveDomains();
  }

  @Get('version')
  @UseInterceptors(CacheInterceptor)
  @CacheTTL(300_000)
  getVersion(): Promise<string> {
    return this.blocklistService.getCurrentVersion();
  }

  @Get('diff')
  getDiff(@Query('since') since: string): Promise<string[]> {
    return this.blocklistService.getDomainsAddedSince(since ?? '0.0.0');
  }
}
