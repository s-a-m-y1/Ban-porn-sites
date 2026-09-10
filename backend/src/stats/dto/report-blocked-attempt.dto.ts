import { IsString, IsNotEmpty, MaxLength } from 'class-validator';

export class ReportBlockedAttemptDto {
  @IsString()
  @IsNotEmpty()
  @MaxLength(64)
  deviceId: string;

  @IsString()
  @IsNotEmpty()
  domain: string;
}
