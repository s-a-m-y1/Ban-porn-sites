import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { BlocklistService } from './blocklist.service';
import { BlocklistUpdateTask } from './blocklist-update.task';
import { Domain } from './entities/domain.entity';
import { BlocklistVersion } from './entities/blocklist-version.entity';

@Module({
  imports: [TypeOrmModule.forFeature([Domain, BlocklistVersion])],
  providers: [BlocklistService, BlocklistUpdateTask],
  exports: [BlocklistService],
})
export class BlocklistCronModule {}
