import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { BlocklistController } from './blocklist.controller';
import { BlocklistService } from './blocklist.service';
import { BlocklistUpdateTask } from './blocklist-update.task';
import { Domain } from './entities/domain.entity';
import { BlocklistVersion } from './entities/blocklist-version.entity';

@Module({
  imports: [TypeOrmModule.forFeature([Domain, BlocklistVersion])],
  controllers: [BlocklistController],
  providers: [BlocklistService, BlocklistUpdateTask],
  exports: [BlocklistService],
})
export class BlocklistModule {}
