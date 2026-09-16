import { Body, Controller, Get, Post, Req, UseGuards } from '@nestjs/common';
import { AuthService } from './auth.service';
import { SignupDto } from './dto/signup.dto';
import { LoginDto } from './dto/login.dto';
import { JwtGuard } from './guards/jwt.guard';

@Controller('auth')
export class AuthController {
  constructor(private readonly auth: AuthService) {}

  @Post('signup')
  signup(@Body() dto: SignupDto) {
    return this.auth.signup(dto);
  }

  @Post('login')
  login(@Body() dto: LoginDto) {
    return this.auth.login(dto);
  }

  @Get('me')
  @UseGuards(JwtGuard)
  me(@Req() req: { user: { sub: string } }) {
    return this.auth.me(req.user.sub);
  }

  @Post('logout')
  @UseGuards(JwtGuard)
  logout() {
    // Stateless JWT — client clears token; server could blacklist if needed
    return { success: true };
  }
}
