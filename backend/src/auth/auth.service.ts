import { ConflictException, Injectable, UnauthorizedException, BadRequestException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { JwtService } from '@nestjs/jwt';
import * as bcrypt from 'bcryptjs';
import { User } from './entities/user.entity';
import { SignupDto } from './dto/signup.dto';
import { LoginDto } from './dto/login.dto';

@Injectable()
export class AuthService {
  constructor(
    @InjectRepository(User) private users: Repository<User>,
    private jwt: JwtService,
  ) {}

  async signup(dto: SignupDto): Promise<{ user: Omit<User, 'passwordHash'>; token: string }> {
    if (dto.password !== dto.confirmPassword) {
      throw new BadRequestException('Passwords do not match');
    }
    const email = dto.email.toLowerCase().trim();
    const existsEmail = await this.users.findOne({ where: { email } });
    if (existsEmail) throw new ConflictException('Email already registered');
    const existsPhone = await this.users.findOne({ where: { phone: dto.phone } });
    if (existsPhone) throw new ConflictException('Phone already registered');

    const hash = await bcrypt.hash(dto.password, 10);
    const user = this.users.create({
      name: dto.name.trim(),
      email,
      phone: dto.phone,
      passwordHash: hash,
    });
    const saved = await this.users.save(user);
    const token = this.sign(saved);
    const { passwordHash, ...safe } = saved as User & { passwordHash: string };
    return { user: safe as Omit<User, 'passwordHash'>, token };
  }

  async login(dto: LoginDto): Promise<{ user: Omit<User, 'passwordHash'>; token: string }> {
    const email = dto.email.toLowerCase().trim();
    const user = await this.users.findOne({ where: { email } });
    if (!user) throw new UnauthorizedException('Invalid credentials');
    const ok = await bcrypt.compare(dto.password, user.passwordHash);
    if (!ok) throw new UnauthorizedException('Invalid credentials');
    const token = this.sign(user);
    const { passwordHash, ...safe } = user as User & { passwordHash: string };
    return { user: safe as Omit<User, 'passwordHash'>, token };
  }

  async me(userId: string): Promise<Omit<User, 'passwordHash'>> {
    const user = await this.users.findOne({ where: { id: userId } });
    if (!user) throw new UnauthorizedException('User not found');
    const { passwordHash, ...safe } = user as User & { passwordHash: string };
    return safe as Omit<User, 'passwordHash'>;
  }

  private sign(user: User): string {
    return this.jwt.sign({ sub: user.id, email: user.email });
  }
}
