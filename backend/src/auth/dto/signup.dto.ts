import { IsEmail, IsNotEmpty, IsString, Length, Matches, MaxLength, MinLength } from 'class-validator';

export class SignupDto {
  @IsString()
  @IsNotEmpty()
  @Length(2, 50)
  name: string;

  @IsEmail()
  @MaxLength(120)
  email: string;

  @IsString()
  @Matches(/^\+?[0-9]{7,15}$/, { message: 'phone must be 7-15 digits, optional leading +' })
  phone: string;

  @IsString()
  @MinLength(8)
  @MaxLength(64)
  password: string;

  @IsString()
  @IsNotEmpty()
  confirmPassword: string;
}
