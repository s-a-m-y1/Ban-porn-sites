import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn, Index } from 'typeorm';

@Entity('coin_transactions')
@Index('idx_coin_tx_user', ['userId'])
export class CoinTransaction {
  @PrimaryGeneratedColumn()
  id: number;

  @Column('uuid')
  userId: string;

  @Column('int')
  amount: number;

  @Column({ length: 120 })
  reason: string;

  @CreateDateColumn()
  createdAt: Date;
}
