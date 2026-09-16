import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { JwtModule } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { UserCoins } from './entities/user-coins.entity';
import { CoinTransaction } from './entities/coin-transaction.entity';
import { CoinsService } from './coins.service';
import { CoinsController } from './coins.controller';

@Module({
  imports: [
    TypeOrmModule.forFeature([UserCoins, CoinTransaction]),
    JwtModule.registerAsync({
      inject: [ConfigService],
      useFactory: (c: ConfigService) => ({
        secret: c.get<string>('JWT_SECRET') ?? 'dev-jwt-secret-change-in-prod-32chars',
      }),
    }),
  ],
  controllers: [CoinsController],
  providers: [CoinsService],
  exports: [CoinsService],
})
export class CoinsModule {}
