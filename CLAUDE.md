# tdd-pipeline-api

Backend ของ TDD Pipeline. **ขยายจาก** workspace `CLAUDE.md` ที่ repo
`tdd-support-workspace` (clone ไว้ระดับเดียวกัน) — อ่านอันนั้นก่อนสำหรับภาพรวม
ทั้งโปรแกรม, git/MR conventions, human/AI split, และ ADR ทั้งหมด.

## Stack

| | |
|---|---|
| Runtime | Java 17 + Spring Boot 3.3 |
| DB | PostgreSQL 16 + Flyway — ทุกตารางอยู่ใน schema **`tddpipeline`** ([ADR-0004](../tdd-support-workspace/docs/adr/0004-dedicated-postgres-schema.md)) |
| Auth | Spring Security + JWT (jjwt 0.12), stateless ([ADR-0003](../tdd-support-workspace/docs/adr/0003-jwt-localstorage-client-auth.md)) |

## Dev

```bash
# ต้องมี PostgreSQL (หรือ docker run postgres:16-alpine ... POSTGRES_DB=tddpipeline)
mvn spring-boot:run          # http://localhost:8080
mvn test                     # DealValidatorTest — ต้องผ่านก่อนเปิด MR
```

Flyway สร้าง schema `tddpipeline` + รัน migration + seed ให้ตอน boot.
`DataInitializer` seed: `admin/admin1234` (ADMIN), `manager.irm/manager1234` (MANAGER แผนก IRM).

### Env

| ตัวแปร | default |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/tddpipeline?currentSchema=tddpipeline` |
| `APP_DB_SCHEMA` | `tddpipeline` |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | `tdd` / `tdd_secret` |
| `JWT_SECRET` | (ตั้งใน prod, ≥ 32 bytes) · `JWT_EXPIRATION_MS` = `28800000` |
| `APP_CORS_ORIGINS` | `http://localhost:3000` |

## โครงสร้าง

```
src/main/java/com/gable/tddpipeline/
├── config/          SecurityConfig, DataInitializer
├── security/        JwtTokenProvider/Filter, AppUserPrincipal, CurrentUser
├── auth/            POST /api/auth/login, GET /api/auth/me
├── domain/          JPA entities   ├── repo/  Spring Data repositories
├── deal/            DealController, DealService, DealValidator (หัวใจ), DealMapper,
│                    DealSpecifications, dto/
├── dashboard/       GET /api/dashboard/summary
├── report/          GET /api/reports/{pr-by-team,smt-qbr,pipeline-by-team}  (pivot)
├── masterconfig/    GET /api/master-config + admin CRUD
├── user/            /api/admin/users
└── web/             GlobalExceptionHandler, error types
src/main/resources/db/migration/   V1__init · V2__seed_master_config · V3__seed_sample_deals
```

## กฎเฉพาะ repo นี้ ([ADR-0005](../tdd-support-workspace/docs/adr/0005-validation-mirrored-error-code-contract.md))

- **`DealValidator` = pure logic** — ไม่มี Spring/DB, รับ context ผ่าน record →
  unit-test ตรง ๆ. `DealValidatorTest` มี 1 test case ต่อ 1 error code, ชื่อ test =
  error code
- validation fail → `DealValidationException` → **HTTP 422** `{ errors: [{field, code, message}] }`.
  `code` ต้องตรงกับ `../tdd-support-workspace/detailed-spec-webapp/validation-business-rules-spec.md` —
  ไม่ผูกกับข้อความ
- **Situation + Deal Owner คำนวณ/lock ฝั่ง server เสมอ** — ไม่เชื่อค่าจาก client
- `is_legacy_migrated` deal: `PUT /api/deals/{id}` ใช้ validation เดียวกับ deal ปกติ — ไม่มี bypass
- Migration SQL **ไม่ qualify schema** (Flyway ตั้ง search_path ให้). ตาราง master config +
  seed ทั้งหมดผ่าน Flyway ไม่ใช่ `ddl-auto`
- เพิ่ม/แก้ business rule = แตะ `DealValidator.java` + `DealValidatorTest` + spec table +
  `tdd-pipeline-web` `src/lib/validation.ts`

## Deploy

`Dockerfile` (multi-stage maven → jre-alpine). Compose อยู่ที่
`tdd-support-workspace/docker-compose.yml`.
