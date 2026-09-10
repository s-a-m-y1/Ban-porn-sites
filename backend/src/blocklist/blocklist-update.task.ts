import { Injectable, Logger } from '@nestjs/common';
import { Cron } from '@nestjs/schedule';
import { ConfigService } from '@nestjs/config';
import { BlocklistService } from './blocklist.service';

@Injectable()
export class BlocklistUpdateTask {
  private readonly logger = new Logger(BlocklistUpdateTask.name);

  constructor(
    private config: ConfigService,
    private blocklistService: BlocklistService,
  ) {}

  @Cron(process.env.BLOCKLIST_CRON || '0 0 * * *')
  async updateFromExternalSource() {
    try {
      const source = this.config.get<string>('EXTERNAL_BLOCKLIST_SOURCE');
      if (!source) return;
      const res = await fetch(source);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const text = await res.text();
      const domains = this.parseHostsFile(text);
      const { added, skipped } = await this.blocklistService.mergeDomains(domains);
      const version = await this.blocklistService.incrementVersion();
      await this.blocklistService.invalidateCache();
      this.logger.log(
        `External sync: +${added} new (skipped ${skipped}), version ${version}`,
      );
    } catch (err) {
      this.logger.error(`External blocklist sync failed: ${err instanceof Error ? err.message : err}`);
    }
  }

  private parseHostsFile(text: string): string[] {
    return text
      .split('\n')
      .map((line) => line.trim())
      .filter((line) => !line.startsWith('#') && line.length > 0)
      .map((line) => line.split(/\s+/)[1] ?? line.split(/\s+/)[0])
      .filter((d) => d && d.includes('.') && !/^\d+\.\d+\.\d+\.\d+$/.test(d));
  }
}
