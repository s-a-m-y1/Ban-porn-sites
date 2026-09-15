import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { AppModule } from './app.module';

async function bootstrap() {
  // P0-1: Fail fast if critical secrets missing in production
  if (process.env.NODE_ENV === 'production' && !process.env.DATABASE_URL) {
    throw new Error('DATABASE_URL must be set in production (no hardcoded fallback)');
  }
  if (process.env.NODE_ENV === 'production' && !process.env.API_KEY) {
    throw new Error('API_KEY must be set in production');
  }
  const app = await NestFactory.create(AppModule);
  app.setGlobalPrefix('api');
  app.useGlobalPipes(
    new ValidationPipe({
      transform: true,
      whitelist: true,
      forbidNonWhitelisted: true,
      transformOptions: { enableImplicitConversion: false },
    }),
  );
  // P0-1: Restrict CORS — allow only HISN origins (env CORS_ORIGIN comma-separated, default localhost + github pages)
  const corsOrigin = (process.env.CORS_ORIGIN ?? 'http://localhost:3000,http://localhost:8080,https://s-a-m-y1.github.io')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);
  app.enableCors({ origin: corsOrigin, credentials: true });
  const port = process.env.PORT ?? 3000;
  await app.listen(port);
  console.log(`Backend listening on :${port}`);
}
bootstrap();
