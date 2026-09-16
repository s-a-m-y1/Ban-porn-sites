import { Body, Controller, Get, Post, Req, UseGuards } from '@nestjs/common';
import { CoinsService } from './coins.service';
import { JwtGuard } from '../auth/guards/jwt.guard';
import { EarnDto } from './dto/earn.dto';

@Controller('coins')
@UseGuards(JwtGuard)
export class CoinsController {
  constructor(private readonly coins: CoinsService) {}

  @Get('balance')
  balance(@Req() req: { user: { sub: string } }) {
    return this.coins.balance(req.user.sub).then(b => ({ balance: b }));
  }

  @Post('earn')
  earn(@Req() req: { user: { sub: string } }, @Body() dto: EarnDto) {
    return this.coins.earn(req.user.sub, dto.amount, dto.reason);
  }

  @Post('spend')
  spend(@Req() req: { user: { sub: string } }, @Body() dto: EarnDto) {
    return this.coins.spend(req.user.sub, dto.amount, dto.reason);
  }

  @Get('history')
  history(@Req() req: { user: { sub: string } }) {
    return this.coins.history(req.user.sub);
  }
}
