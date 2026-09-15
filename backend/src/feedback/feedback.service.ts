import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Feedback } from './entities/feedback.entity';
import { CreateFeedbackDto } from './dto/create-feedback.dto';

@Injectable()
export class FeedbackService {
  constructor(@InjectRepository(Feedback) private repo: Repository<Feedback>) {}

  async create(dto: CreateFeedbackDto): Promise<Feedback> {
    return this.repo.save(this.repo.create(dto));
  }

  async count(): Promise<number> {
    return this.repo.count();
  }
}
