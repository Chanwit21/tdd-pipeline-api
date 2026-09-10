# TDD Pipeline API (Backend)

Backend ของ **TDD Pipeline Web Application** (Phase 1)

| | |
|---|---|
| Runtime | Java 17 + Spring Boot 3.3 |
| DB | PostgreSQL 16 + Flyway migrations |
| Auth | Spring Security + JWT (jjwt 0.12) |
| Frontend | [tdd-pipeline-web](https://git.g-able.com/amc/tdd-pipeline-web) |

## Dev

```bash
# ต้องมี PostgreSQL รันอยู่ (หรือ: docker run -d -p 5432:5432 -e POSTGRES_DB=tddpipeline \
#   -e POSTGRES_USER=tdd -e POSTGRES_PASSWORD=tdd_secret postgres:16-alpine)

mvn spring-boot:run          # http://localhost:8080
```

ทุกตาราง (รวม `flyway_schema_history`) อยู่ใน schema **`tddpipeline`** ไม่ใช่ `public`
Flyway สร้าง schema + รัน migration + seed ให้อัตโนมัติตอน boot (`flyway.schemas=tddpipeline`,
`hibernate.default_schema=tddpipeline`, JDBC URL มี `?currentSchema=tddpipeline`)
`DataInitializer` seed user: `admin / admin1234` (ADMIN), `manager.irm / manager1234` (MANAGER แผนก IRM)

### Env

| ตัวแปร | default |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/tddpipeline?currentSchema=tddpipeline` |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | `tdd` / `tdd_secret` |
| `APP_DB_SCHEMA` | `tddpipeline` (schema ที่ Flyway + Hibernate ใช้) |
| `JWT_SECRET` | (ต้องตั้งใน prod, ≥ 32 bytes) |
| `JWT_EXPIRATION_MS` | `28800000` (8 ชม.) |
| `APP_CORS_ORIGINS` | `http://localhost:3000` |

## Test

```bash
mvn test          # DealValidatorTest — ครอบคลุม error code ทุกตัวใน validation spec
```

## Docker

```bash
docker build -t tdd-pipeline-api .
docker run -p 8080:8080 -e SPRING_DATASOURCE_URL=... tdd-pipeline-api
```

## โครงสร้าง

```
src/main/java/com/gable/tddpipeline/
├── config/          SecurityConfig, DataInitializer
├── security/        JWT provider/filter, AppUserPrincipal, CurrentUser
├── auth/            POST /api/auth/login, GET /api/auth/me
├── domain/          JPA entities
├── repo/            Spring Data repositories
├── deal/            DealController, DealService, DealValidator (หัวใจ business rule),
│                    DealMapper, DealSpecifications, dto/
├── dashboard/       GET /api/dashboard/summary
├── report/          GET /api/reports/{pr-by-team,smt-qbr,pipeline-by-team}
├── masterconfig/    GET /api/master-config, admin CRUD
├── user/            /api/admin/users
└── web/             GlobalExceptionHandler, error types
src/main/resources/db/migration/   V1__init, V2__seed_master_config, V3__seed_sample_deals
```

## หลักการ validation

`DealValidator` เป็น logic บริสุทธิ์ (ไม่มี Spring/DB) — unit-test ได้ตรง ๆ
`DealService.validated()` ประกอบ context จาก master config แล้วเรียก validator; fail → throw `DealValidationException`
→ HTTP **422** body `{ errors: [{ field, code, message }] }` โดย `code` ตรงกับตารางใน `validation-business-rules-spec.md`

Situation และ Deal Owner **คำนวณ/lock ฝั่ง server เสมอ** ไม่เชื่อค่าจาก client
`is_legacy_migrated` deal: `PUT /api/deals/{id}` ใช้ validation เดียวกับ deal ปกติ (ไม่มี bypass)
