import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn, Index } from 'typeorm';

@Entity('feedbacks')
@Index('idx_feedback_device', ['deviceId'])
export class Feedback {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ length: 64 })
  deviceId: string;

  @Column({ type: 'int' })
  rating: number; // 1-5

  @Column({ nullable: true, length: 32 })
  context?: string; // protection | stop | settings | general

  @Column({ nullable: true, length: 256 })
  reason?: string;

  @Column({ nullable: true, length: 1024 })
  comment?: string;

  @CreateDateColumn()
  createdAt: Date;
}
