import { Entity, PrimaryColumn, Column, UpdateDateColumn } from 'typeorm';

@Entity('user_coins')
export class UserCoins {
  @PrimaryColumn('uuid')
  userId: string;

  @Column({ type: 'int', default: 0 })
  balance: number;

  @UpdateDateColumn()
  updatedAt: Date;
}
