import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  Index,
} from 'typeorm';

@Entity('blocked_attempts')
@Index('idx_attempts_device', ['deviceId'])
export class BlockedAttempt {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ length: 64 })
  deviceId: string;

  @Column()
  domain: string;

  @CreateDateColumn()
  timestamp: Date;
}
