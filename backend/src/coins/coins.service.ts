import { BadRequestException, Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { UserCoins } from './entities/user-coins.entity';
import { CoinTransaction } from './entities/coin-transaction.entity';

@Injectable()
export class CoinsService {
  constructor(
    @InjectRepository(UserCoins) private coins: Repository<UserCoins>,
    @InjectRepository(CoinTransaction) private txs: Repository<CoinTransaction>,
  ) {}

  async balance(userId: string): Promise<number> {
    const row = await this.coins.findOne({ where: { userId } });
    return row?.balance ?? 0;
  }

  async earn(userId: string, amount: number, reason: string): Promise<{ balance: number }> {
    if (amount <= 0) throw new BadRequestException('amount must be >0');
    // anti-replay: same reason within same day not double counted
    const todayStart = new Date(); todayStart.setHours(0,0,0,0);
    const existing = await this.txs.findOne({ where: { userId, reason } as never });
    // simple check: if same reason today exists, skip (idempotent)
    // For V1, allow 1 per reason per day
    const todayTx = await this.txs.createQueryBuilder('t')
      .where('t.userId = :uid AND t.reason = :reason AND t.createdAt >= :today', { uid: userId, reason, today: todayStart })
      .getOne();
    if (todayTx) return { balance: await this.balance(userId) };

    return this.mutate(userId, amount, reason);
  }

  async spend(userId: string, amount: number, reason: string): Promise<{ balance: number }> {
    if (amount <= 0) throw new BadRequestException('amount must be >0');
    const bal = await this.balance(userId);
    if (bal < amount) throw new BadRequestException('Insufficient coins');
    return this.mutate(userId, -amount, reason);
  }

  async history(userId: string): Promise<CoinTransaction[]> {
    return this.txs.find({ where: { userId }, order: { createdAt: 'DESC' }, take: 50 });
  }

  private async mutate(userId: string, amount: number, reason: string): Promise<{ balance: number }> {
    const coin = (await this.coins.findOne({ where: { userId } })) ?? this.coins.create({ userId, balance: 0 });
    coin.balance += amount;
    if (coin.balance < 0) throw new BadRequestException('Negative balance');
    await this.coins.save(coin);
    await this.txs.save(this.txs.create({ userId, amount, reason }));
    return { balance: coin.balance };
  }
}
