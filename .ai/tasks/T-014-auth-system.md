---
id: T-014
title: Auth system — signup/login with validation, session
type: feature
status: DONE
owner: agent/backend
dependencies: []
priority: high
estimate: M
---

# T-014 — Auth system

## Objective
Simple email/phone+password auth where backend is source of truth, extensible to OAuth.

## Context
No auth exists (only ApiKeyGuard, PinManager). Need Name/Email/Phone/Password/Confirm for signup, Email/Password for login, with validation, loading/error/success, logout, session persistence.

## Scope
### Files
- `backend/src/auth/` (create) — `auth.module.ts`, `auth.controller.ts`, `auth.service.ts`, `entities/user.entity.ts`, `dto/signup.dto.ts`, `dto/login.dto.ts`, `guards/jwt.guard.ts`
- `backend/src/app.module.ts` (modify) — import AuthModule
- `android/app/src/main/java/com/contentfilter/app/AuthRepository.kt` (create)
- `android/app/src/main/java/com/contentfilter/app/LoginActivity.kt` (create)
- `android/app/src/main/java/com/contentfilter/app/SignupActivity.kt` (create)
- `android/app/src/main/res/layout/activity_login.xml`, `activity_signup.xml` (create)

## Implementation Requirements
1. Backend: User {id, name, email UNIQUE, phone, passwordHash (bcrypt), createdAt}, signup hashes with bcrypt 10, login returns JWT (7d), guards via JwtGuard
2. Validation: name 2-50, email IsEmail, phone 7-15 digits, password 8-64, confirm must match, Coerce/whitelist
3. Android: Retrofit AuthRepository, SharedPreferences session (jwt, user json), loading/error/success UI, logout clears, session restore on splash

## Acceptance Criteria
- [ ] `POST /api/auth/signup` → 201 with user (no password), duplicate email → 409
- [ ] `POST /api/auth/login` → 200 with jwt, wrong → 401
- [ ] Android signup/login flows with validation errors shown, loading spinner, success → MainActivity, logout → Login

## Testing Requirements
- Backend: jest for signup/login, duplicate, validation, hash
- Android: manual login/signup on RMX3760

## Handoff
→ Next: T-015 coins, needs: Auth DONE
