import { Injectable } from '@nestjs/common';
import { CoinsService } from '../coins/coins.service';

@Injectable()
export class ProgressService {
  constructor(private coins: CoinsService) {}

  async get(userId: string) {
    const balance = await this.coins.balance(userId);
    const level = Math.floor(balance / 100) + 1;
    const nextXp = level * 100;
    const xp = balance;
    const percent = balance % 100;
    // streak placeholder — uses coins history length as proxy for now
    const history = await this.coins.history(userId);
    const streak = Math.min(history.length, 7);
    const achievements: string[] = [];
    if (balance >= 1) achievements.push('first_block');
    if (streak >= 7) achievements.push('7_streak');
    if (balance >= 100) achievements.push('100_coins');
    return { level, xp, nextXp, percent, streak, achievements, history: history.slice(0, 10) };
  }
}
