import { Controller, Get, Req, UseGuards } from '@nestjs/common';
import { JwtGuard } from '../auth/guards/jwt.guard';
import { ProgressService } from './progress.service';

@Controller('progress')
@UseGuards(JwtGuard)
export class ProgressController {
  constructor(private readonly progress: ProgressService) {}
  @Get()
  get(@Req() req: { user: { sub: string } }) {
    return this.progress.get(req.user.sub);
  }
}
