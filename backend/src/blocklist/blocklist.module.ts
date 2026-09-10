import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { BlocklistController } from './blocklist.controller';
import { BlocklistService } from './blocklist.service';
import { Domain } from './entities/domain.entity';
import { BlocklistVersion } from './entities/blocklist-version.entity';

@Module({
  imports: [TypeOrmModule.forFeature([Domain, BlocklistVersion])],
  controllers: [BlocklistController],
  providers: [BlocklistService],
  exports: [BlocklistService],
})
export class BlocklistModule {}
