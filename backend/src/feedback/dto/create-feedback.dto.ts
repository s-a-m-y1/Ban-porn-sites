import { IsInt, IsOptional, IsString, Max, MaxLength, Min } from 'class-validator';

export class CreateFeedbackDto {
  @IsString()
  @MaxLength(64)
  deviceId: string;

  @IsInt()
  @Min(1)
  @Max(5)
  rating: number;

  @IsOptional()
  @IsString()
  @MaxLength(32)
  context?: string;

  @IsOptional()
  @IsString()
  @MaxLength(256)
  reason?: string;

  @IsOptional()
  @IsString()
  @MaxLength(1024)
  comment?: string;
}
