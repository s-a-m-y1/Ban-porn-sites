import {
  CanActivate,
  ExecutionContext,
  Injectable,
  UnauthorizedException,
} from '@nestjs/common';

@Injectable()
export class ApiKeyGuard implements CanActivate {
  canActivate(context: ExecutionContext): boolean {
    const request = context.switchToHttp().getRequest();
    if (request.headers['x-api-key'] === process.env.API_KEY) {
      return true;
    }
    throw new UnauthorizedException('Invalid API key');
  }
}
