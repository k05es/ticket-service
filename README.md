# Ticket Service — лабораторная работа № 4 (Spring Security, роли и логирование)

REST-приложение для учёта пользовательских заявок, выполненное на базе кода лабораторной работы № 3 с добавлением Spring Security, разграничения прав доступа по ролям и логирования в консоль и файл.

## 1. Технологический стек

- Java 21 / 17+
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA / Hibernate
- Spring Security (JWT + HTTP Basic, BCrypt)
- Springdoc OpenAPI / Swagger UI
- PostgreSQL 17
- Logback / SLF4J
- JUnit 5, Mockito, Spring Boot Test, MockMvc

## 2. Что добавлено в лабораторной работе № 4

1. **Подключение Spring Security и хэширование паролей:**
   - Подключены `spring-boot-starter-security`, `spring-boot-starter-security-test` и `jjwt` (`0.12.6`).
   - В `PasswordConfig` настроен `BCryptPasswordEncoder`; при регистрации пользователей в `UserServiceImpl` пароль автоматически кодируется.
   - При старте приложения `AdminInitializer` создаёт администратора по умолчанию (`admin` / `admin123`), если он ещё не создан в БД.

2. **Пользователи, роли и аутентификация:**
   - Используется иерархия сущностей из ЛР № 1–3 (`User` → `AdminUser`, `SpecialistUser`, `CustomerUser`) и роли `UserRole` (`ADMIN`, `SPECIALIST`, `CUSTOMER`).
   - `TicketUserDetailsService` загружает пользователя из БД и преобразует `UserRole` в authority `ROLE_<ROLE>`, учитывая статус `UserStatus.ACTIVE / BLOCKED`.
   - Реализованы эндпоинты `POST /api/auth/register` (самостоятельная регистрация клиента) и `POST /api/auth/login` (получение JWT-токена).

3. **Разграничение методов REST API по ролям (`SecurityConfig` и `@PreAuthorize`):**
   - `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**` — публичный доступ.
   - `DELETE /api/tickets/**` и `DELETE /api/users/**` — **только `ADMIN`**.
   - `GET /api/users`, `POST /api/users/**`, `PUT /api/users/**` — **только `ADMIN`**.
   - `POST /api/tickets` и `PUT /api/tickets/{id}` — **`CUSTOMER`, `ADMIN`**.
   - `POST /api/tickets/{id}/assign/{specialistId}` и `POST /api/tickets/{id}/complete` — **`SPECIALIST`, `ADMIN`**.
   - `POST /api/tickets/{id}/cancel` — **`CUSTOMER`, `SPECIALIST`, `ADMIN`**.
   - `GET /api/tickets/**`, `GET /api/users/{id}`, `POST /api/tickets/{id}/messages` — все аутентифицированные пользователи (с дополнительной проверкой участников заявки в `TicketServiceImpl`).
   - Ошибки `401 Unauthorized` (`RestAuthenticationEntryPoint`) и `403 Forbidden` (`RestAccessDeniedHandler`) возвращаются в едином формате `ApiError`.

4. **Логирование в консоль и файл:**
   - Настроен `src/main/resources/logback-spring.xml` с одновременным выводом в консоль (`CONSOLE`) и в ротируемый файл `logs/ticket-service.log` (`RollingFileAppender`).
   - Добавлен `RequestLoggingFilter` для записи логов по всем HTTP-запросам к `/api/**`, а также логирование ключевых действий и ошибок через SLF4J (`@Slf4j`).

## 3. Структура пакетов

- `config` — `SecurityConfig`, `PasswordConfig`, `AdminInitializer`, `OpenApiConfig`
- `security` — `JwtService`, `JwtAuthenticationFilter`, `TicketUserDetailsService`, `RequestLoggingFilter`, `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`, `JsonErrorWriter`
- `controller` — `AuthController`, `UserController`, `TicketController`
- `service` — интерфейсы и реализации (`AuthService`/`AuthServiceImpl`, `UserService`/`UserServiceImpl`, `TicketService`/`TicketServiceImpl`, `SpecialistStatisticsScheduler`)
- `repository` — `UserRepository`, `TicketRepository`
- `model` — `User`, `AdminUser`, `SpecialistUser`, `CustomerUser`, `Ticket`, `Message`, `UserRole`, `UserStatus`, `TicketStatus`, `Department`, `Permission`, `ContactPreference`
- `dto/request` и `dto/response` — DTO запросов и ответов (включая `AuthResponse`, `ApiError`)
- `mapper` — `UserMapper`, `TicketMapper`
- `exception` — иерархия кастомных исключений и `GlobalExceptionHandler`
- `concurrent` — `TicketQueueProcessor` (многопоточная обработка очереди заявок)

## 4. Сборка и запуск тестов без БД

```powershell
.\mvnw.cmd clean verify
```
