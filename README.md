# Ticket Service — лабораторная работа № 2

REST-приложение для учёта пользовательских заявок. Клиенты создают заявки, специалисты принимают их в работу, ведут переписку и завершают обработку. Данные сохраняются в PostgreSQL, доступ к ним реализован через Spring Data JPA.

## 1. Технологический стек

- Java 17
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA / Hibernate
- PostgreSQL 17
- Jakarta Validation
- Lombok
- JUnit 5, Mockito, Spring Boot Test, MockMvc
- Maven Wrapper
- Docker Compose

## 2. Архитектура

```text
Controller → Service (interface + implementation) → Repository → PostgreSQL
                 ↓
             Model / DTO / Mapper
```

- `controller` — REST-контроллеры пользователей и заявок.
- `service` — интерфейсы и бизнес-логика.
- `repository` — Spring Data JPA-репозитории и запросы JPQL.
- `model` — JPA-сущности и перечисления предметной области.
- `dto/request` — входные модели запросов и Bean Validation.
- `dto/response` — модели ответов API; хэш пароля в ответы не включается.
- `mapper` — преобразование DTO и сущностей.
- `exception` — исключения предметной области и единый обработчик HTTP-ошибок.
- `concurrent` — многопоточный обработчик очереди из лабораторной работы № 1.

Пользователи представлены базовой сущностью `User` и типами `AdminUser`, `CustomerUser`, `SpecialistUser`. Для хранения используется `InheritanceType.SINGLE_TABLE` с дискриминатором `user_type`.

## 3. База данных PostgreSQL

SQL-схема находится в `database/schema.sql`:

| Таблица | Назначение |
|---|---|
| `users` | Клиенты, специалисты и администраторы |
| `admin_permissions` | Разрешения администраторов |
| `admin_specialists` | ID специалистов, закреплённых за администраторами |
| `tickets` | Заявки, статусы, связи и версия сущности |
| `messages` | Сообщения внутри заявок |

В `tickets` присутствует поле `version`, соответствующее `@Version` в сущности `Ticket`.

Hibernate настроен на `spring.jpa.hibernate.ddl-auto=validate`: приложение проверяет соответствие уже созданной схемы JPA-модели, но не создаёт и не изменяет таблицы автоматически.

### Вариант A. Docker Compose

1. При необходимости скопируйте `.env.example` в `.env`.
2. Запустите сервисы: `docker compose up -d`.
3. PostgreSQL будет доступен на `localhost:5432`, pgAdmin — на `http://localhost:5050`.
4. В pgAdmin войдите с учётными данными из `.env` (по умолчанию `admin@example.com` / `admin`). Добавьте сервер: Host `postgres`, Port `5432`, Maintenance database `ticket_service`, Username/Password — значения `POSTGRES_USER` / `POSTGRES_PASSWORD`.

При первой инициализации пустого Docker volume файл `database/schema.sql` выполняется автоматически. Если volume уже создан, изменение SQL-файла не приводит к повторному выполнению: примените обновлённую схему вручную через Query Tool pgAdmin. Команда `docker compose down -v` удаляет тома и все данные в них.

### Вариант B. Установленный PostgreSQL и pgAdmin

1. Создайте базу данных `ticket_service`.
2. Откройте Query Tool этой базы в pgAdmin.
3. Выполните содержимое `database/schema.sql`.
4. Укажите параметры подключения через переменные окружения.

### Переменные окружения

| Переменная | Значение по умолчанию | Назначение |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/ticket_service` | JDBC URL приложения |
| `DB_USERNAME` | `postgres` | Пользователь PostgreSQL |
| `DB_PASSWORD` | `postgres` | Пароль PostgreSQL |
| `POSTGRES_DB` | `ticket_service` | Имя БД контейнера |
| `POSTGRES_USER` | `postgres` | Пользователь контейнера PostgreSQL |
| `POSTGRES_PASSWORD` | `postgres` | Пароль контейнера PostgreSQL |
| `PGADMIN_DEFAULT_EMAIL` | `admin@example.com` | Логин pgAdmin |
| `PGADMIN_DEFAULT_PASSWORD` | `admin` | Пароль pgAdmin |
| `SERVER_PORT` | `8080` | Порт REST-приложения |
| `STATISTICS_TIME_ZONE` | `UTC` | Часовой пояс ежедневного сброса статистики |

Демонстрационные пароли предназначены только для локальной учебной среды. Не публикуйте `.env` и не используйте эти значения в открытой или рабочей среде.

## 4. Запуск приложения

Сначала запустите PostgreSQL и примените SQL-схему.

Windows PowerShell:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/ticket_service"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="postgres"
.\mvnw.cmd spring-boot:run
```

Linux / macOS:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/ticket_service
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
./mvnw spring-boot:run
```

REST API по умолчанию доступен по адресу `http://localhost:8080`.

`TicketQueueProcessor` сохранён как отдельный компонент лабораторной работы № 1 и не запускается автоматически. Внутри процессора общий набор резервируемых ID предотвращает одновременную попытку нескольких рабочих потоков забрать одну и ту же первую заявку. Окончательная защита от повторного назначения обеспечивается транзакционной блокировкой строки заявки.

## 5. REST API

Все запросы и ответы используют JSON.

### 5.1. Пользователи — `/api/users`

| Метод | Endpoint | Назначение |
|---|---|---|
| `POST` | `/api/users` | Создание пользователя |
| `GET` | `/api/users` | Получение списка и фильтрация |
| `GET` | `/api/users/{id}` | Получение пользователя по ID |
| `PUT` | `/api/users/{id}` | Частичное обновление переданных полей |
| `DELETE` | `/api/users/{id}` | Удаление пользователя при отсутствии связей |

Фильтры: `role`, `status`, `username`, `email`.

Пример: `GET /api/users?role=SPECIALIST&status=ACTIVE&username=pet`.

`PUT /api/users/{id}` принимает `UpdateUserRequest`: обновляются только переданные поля. Роль и `passHash` через этот endpoint не изменяются. Для клиента телефон должен соответствовать формату `+7XXXXXXXXXX`; пустая строка не считается корректным значением.

### 5.2. Заявки — `/api/tickets`

| Метод | Endpoint | Назначение |
|---|---|---|
| `POST` | `/api/tickets` | Создание заявки |
| `GET` | `/api/tickets` | Получение списка и фильтрация |
| `GET` | `/api/tickets/{id}` | Получение заявки по ID |
| `PUT` | `/api/tickets/{id}` | Изменение заголовка и описания в статусе `PENDING` |
| `DELETE` | `/api/tickets/{id}` | Удаление заявки, кроме завершённой |
| `POST` | `/api/tickets/{id}/assign/{specialistId}` | Назначение специалиста |
| `POST` | `/api/tickets/{id}/complete` | Завершение заявки и сохранение оценки |
| `POST` | `/api/tickets/{id}/cancel` | Отмена заявки |
| `POST` | `/api/tickets/{id}/messages` | Добавление сообщения |

Фильтры: `status`, `customerId`, `specialistId`, `customerEmail`.

Примеры:

```http
GET /api/tickets?status=PENDING&customerId=1
GET /api/tickets?customerEmail=ivan@example.com
```

Создание заявки:

```json
{
  "title": "Не работает принтер",
  "description": "При печати появляется ошибка",
  "customerId": 1
}
```

Завершение заявки:

```json
{
  "rating": 4.5
}
```

Добавление сообщения:

```json
{
  "senderId": 1,
  "text": "Добавляю уточнение по проблеме"
}
```

### 5.3. Правила бизнес-логики

- Создавать заявки могут только существующие активные клиенты.
- Назначать заявку можно только существующему активному и доступному специалисту; заявка должна иметь статус `PENDING`.
- Сообщения могут отправлять клиент, которому принадлежит заявка, назначенный специалист или активный администратор с разрешением `MANAGE_TICKETS`.
- Неактивный пользователь не может отправлять сообщения.
- Завершить можно только заявку в статусе `IN_PROGRESS`; оценка должна быть в диапазоне от `0.0` до `5.0`.
- Изменять можно только заявку в статусе `PENDING`.
- Завершённую заявку удалить нельзя: она участвует в расчёте количества завершённых заявок и средней оценки специалиста. Запрет предотвращает расхождение агрегированной статистики.
- Перед удалением пользователя проверяются связи с заявками, сообщениями и списками ответственных специалистов администраторов.
- Суточный счётчик завершённых заявок специалистов сбрасывается планировщиком ежедневно в `00:00` в часовом поясе `STATISTICS_TIME_ZONE` (по умолчанию UTC).

### 5.4. HTTP-статусы ошибок

| HTTP | Ситуация |
|---|---|
| `400 Bad Request` | Ошибка валидации или некорректный аргумент |
| `404 Not Found` | Пользователь или заявка не найдены |
| `409 Conflict` | Нарушение бизнес-правила, конфликт или ограничение целостности |

`GlobalExceptionHandler` формирует единый JSON-ответ с временной меткой, кодом, названием ошибки и сообщением.

## 6. Транзакции и конкурентный доступ

- Изменения заявок выполняются в транзакциях сервисного слоя.
- `PESSIMISTIC_WRITE` используется при чтении заявки/пользователя перед критическим изменением.
- `@Version` в сущности `Ticket` обеспечивает оптимистический контроль версии.
- `TicketRepository.findByIdForUpdate` дополнительно загружает связи `customer`, `specialist`, `messages`, `messages.sender`, которые использует `TicketMapper`. Это важно при отключённом Open Session In View (`spring.jpa.open-in-view=false`).
- При удалении заявки блокируется строка клиента перед изменением счётчика созданных заявок.

## 7. Тестирование

Запуск полного Maven lifecycle с компиляцией, тестами и проверками:

Windows PowerShell:

```powershell
.\mvnw.cmd clean verify
```

Linux / macOS:

```bash
./mvnw clean verify
```

В проекте подготовлено 35 тестовых методов: unit-тесты сервисов с Mockito, HTTP-тесты REST-контроллеров с MockMvc, тесты обработчика исключений и загрузки Spring-контекста. Тестовый контекст подменяет репозитории Mockito и отключает автоконфигурацию DataSource/JPA. Тесты не подключаются к PostgreSQL и не изменяют содержимое базы. Проверка соответствия реальной SQL-схемы JPA-модели выполняется отдельно при запуске приложения на подготовленной БД.

## 8. CI

- `.gitlab-ci.yml` — GitLab Pipeline с выполнением `mvn verify`.
- `.github/workflows/ci.yml` — GitHub Actions с выполнением `./mvnw verify`.



