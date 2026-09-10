import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  Index,
} from 'typeorm';

@Entity('domains')
@Index('idx_domains_domain', ['domain'])
export class Domain {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ unique: true })
  domain: string;

  @Column({ default: 'adult-content' })
  category: string;

  @Column({ default: true })
  active: boolean;

  @CreateDateColumn()
  addedAt: Date;
}
