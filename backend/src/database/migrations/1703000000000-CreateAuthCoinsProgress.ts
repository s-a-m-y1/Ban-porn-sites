import { MigrationInterface, QueryRunner } from 'typeorm';

export class CreateAuthCoinsProgress1703000000000 implements MigrationInterface {
  public async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`
      CREATE TABLE IF NOT EXISTS "users" (
        "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
        "name" varchar(50) NOT NULL,
        "email" varchar(120) NOT NULL UNIQUE,
        "phone" varchar(20) NOT NULL UNIQUE,
        "passwordHash" varchar NOT NULL,
        "createdAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
        "updatedAt" TIMESTAMPTZ NOT NULL DEFAULT now()
      );
    `);
    await queryRunner.query(`
      CREATE TABLE IF NOT EXISTS "user_coins" (
        "userId" uuid PRIMARY KEY REFERENCES "users"("id") ON DELETE CASCADE,
        "balance" integer NOT NULL DEFAULT 0,
        "updatedAt" TIMESTAMPTZ NOT NULL DEFAULT now()
      );
    `);
    await queryRunner.query(`
      CREATE TABLE IF NOT EXISTS "coin_transactions" (
        "id" SERIAL PRIMARY KEY,
        "userId" uuid NOT NULL REFERENCES "users"("id") ON DELETE CASCADE,
        "amount" integer NOT NULL,
        "reason" varchar(120) NOT NULL,
        "createdAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
        CONSTRAINT "CHK_amount_not_zero" CHECK (amount <> 0)
      );
    `);
    await queryRunner.query(`CREATE INDEX "idx_coin_tx_user" ON "coin_transactions"("userId");`);
    await queryRunner.query(`
      CREATE TABLE IF NOT EXISTS "feedbacks" (
        "id" SERIAL PRIMARY KEY,
        "deviceId" varchar(64) NOT NULL,
        "rating" integer NOT NULL CHECK (rating BETWEEN 1 AND 5),
        "context" varchar(32),
        "reason" varchar(256),
        "comment" varchar(1024),
        "createdAt" TIMESTAMPTZ NOT NULL DEFAULT now()
      );
    `);
  }
  public async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`DROP TABLE IF EXISTS "feedbacks";`);
    await queryRunner.query(`DROP TABLE IF EXISTS "coin_transactions";`);
    await queryRunner.query(`DROP TABLE IF EXISTS "user_coins";`);
    await queryRunner.query(`DROP TABLE IF EXISTS "users";`);
  }
}
