import { Body, Controller, Post } from '@nestjs/common';
import { Throttle } from '@nestjs/throttler';
import { FeedbackService } from './feedback.service';
import { CreateFeedbackDto } from './dto/create-feedback.dto';

@Controller('feedback')
export class FeedbackController {
  constructor(private readonly service: FeedbackService) {}

  @Post()
  @Throttle({ default: { limit: 30, ttl: 60_000 } })
  create(@Body() dto: CreateFeedbackDto) {
    return this.service.create(dto);
  }
}
