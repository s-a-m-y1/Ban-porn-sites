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
    const apiKey = process.env.API_KEY;
    // P0-1: Fail closed — missing API_KEY must NEVER allow access (was undefined===undefined → true)
    if (!apiKey || apiKey === 'change-me-in-production') {
      throw new UnauthorizedException(
        'Server misconfigured: API_KEY missing or insecure default',
      );
    }
    const provided = requestHeader(request.headers['x-api-key']);
    if (provided && provided === apiKey) {
      return true;
    }
    throw new UnauthorizedException('Invalid API key');
  }
}

function requestHeader(v: unknown): string | undefined {
  if (typeof v === 'string') return v.trim();
  // Arrays (e.g. duplicate headers) are rejected — must not accept ['key'] as valid
  return undefined;
}
