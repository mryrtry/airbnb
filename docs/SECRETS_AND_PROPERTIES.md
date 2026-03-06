# Секреты репозитория и настройка свойств приложения

## 1. Секреты GitHub (Actions)

В **Settings → Secrets and variables → Actions** репозитория должны быть заданы следующие секреты, чтобы CI/CD мог собрать и задеплоить приложение с корректным конфигом.

### Обязательные для сборки и деплоя

| Секрет | Описание |
|--------|----------|
| `DB_URL` | JDBC URL БД, например `jdbc:postgresql://host:5432/airbnb` |
| `DB_USERNAME` | Имя пользователя БД |
| `DB_PASSWORD` | Пароль БД |
| `JWT_SECRET` | Секрет для подписи JWT (достаточно длинная случайная строка) |
| `DEPLOY_KEY` | Приватный SSH-ключ для доступа к серверу деплоя (пользователь `root@83.166.246.71`) |

### Обязательные для подстановки в application-prod.properties (почта)

Workflow подставляет эти значения в шаблон. Если не задать их, в файле останутся пустые строки и приложение может не запуститься из‑за невалидной конфигурации почты.

| Секрет | Описание |
|--------|----------|
| `MAIL_FROM` | Адрес отправителя (например `noreply@yourdomain.com`) |
| `MAIL_HOST` | SMTP-хост (например `smtp.gmail.com`) |
| `MAIL_PORT` | Порт SMTP (например `587`) |
| `MAIL_USERNAME` | Логин SMTP |
| `MAIL_PASSWORD` | Пароль или пароль приложения SMTP |

Если в продакшене почта не используется: в `application-prod-template.properties` поставьте `airbnb.mail.enabled=false` и укажите для `MAIL_*` любые допустимые значения (чтобы подстановка не оставляла пустые поля), например тестовый хост и порт.

---

## 2. Свойства приложения, которые нужно задать

### 2.1. Общие (application.properties)

- `spring.application.name=airbnb` — уже задано.
- `spring.jpa.open-in-view=false` — уже задано.

### 2.2. Профиль prod (application-prod-template.properties)

Шаблон заполняется при сборке из секретов. Ниже перечислены все плейсхолдеры и соответствующие свойства.

| Свойство | Плейсхолдер | Откуда берётся |
|----------|-------------|----------------|
| `server.port` | `@SERVER_PORT@` | Переменная окружения workflow `SERVER_PORT` (8126) |
| `spring.datasource.url` | `@DB_URL@` | Секрет `DB_URL` |
| `spring.datasource.username` | `@DB_USERNAME@` | Секрет `DB_USERNAME` |
| `spring.datasource.password` | `@DB_PASSWORD@` | Секрет `DB_PASSWORD` |
| `airbnb.jwt.secret` | `@JWT_SECRET@` | Секрет `JWT_SECRET` |
| `airbnb.mail.from` | `@MAIL_FROM@` | Секрет `MAIL_FROM` |
| `spring.mail.host` | `@MAIL_HOST@` | Секрет `MAIL_HOST` |
| `spring.mail.port` | `@MAIL_PORT@` | Секрет `MAIL_PORT` |
| `spring.mail.username` | `@MAIL_USERNAME@` | Секрет `MAIL_USERNAME` |
| `spring.mail.password` | `@MAIL_PASSWORD@` | Секрет `MAIL_PASSWORD` |

Остальные свойства в шаблоне (JWT expiration, `spring.jpa.hibernate.ddl-auto=validate`, cron планировщика и т.д.) уже прописаны и при необходимости меняются правкой самого шаблона.

### 2.3. Профиль dev (application-dev.properties)

Используются для локального запуска: порт 9000, локальная БД, свой JWT secret, почта отключена. Менять по необходимости (URL БД, порт, включение почты для тестов).

---

## 3. Проверка перед деплоем

1. Все перечисленные секреты заданы в репозитории.
2. На сервере деплоя доступна PostgreSQL по `DB_URL` с учётом сети (доступ с runner’а не требуется; приложение подключается к БД с сервера).
3. Порт `SERVER_PORT` (8126) не занят на сервере и открыт при необходимости (файрвол/балансировщик).
4. Для работы почты в prod заданы все `MAIL_*` секреты и в шаблоне `airbnb.mail.enabled=true` (или отключите почту и подставьте заглушки, как указано выше).

После push в `master` workflow собирает JAR с профилем `prod`, подставляет секреты в `application-prod.properties`, загружает JAR на сервер и запускает его с `--spring.profiles.active=prod`.
