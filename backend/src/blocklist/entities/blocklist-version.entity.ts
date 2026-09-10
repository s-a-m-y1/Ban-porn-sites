import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  UpdateDateColumn,
} from 'typeorm';

@Entity('blocklist_versions')
export class BlocklistVersion {
  @PrimaryGeneratedColumn()
  id: number;

  @Column()
  version: string;

  @UpdateDateColumn()
  updatedAt: Date;
}
