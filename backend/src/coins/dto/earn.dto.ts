import { IsInt, IsNotEmpty, IsString, MaxLength, Min } from 'class-validator';

export class EarnDto {
  @IsInt()
  @Min(1)
  amount: number;

  @IsString()
  @IsNotEmpty()
  @MaxLength(120)
  reason: string;
}
