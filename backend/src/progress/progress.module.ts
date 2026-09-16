import { Module } from '@nestjs/common';
import { CoinsModule } from '../coins/coins.module';
import { ProgressService } from './progress.service';
import { ProgressController } from './progress.controller';

@Module({
  imports: [CoinsModule],
  controllers: [ProgressController],
  providers: [ProgressService],
})
export class ProgressModule {}
