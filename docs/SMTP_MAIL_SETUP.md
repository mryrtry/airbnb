# Настройка отправки почты (SMTP)

В приложении используется Spring Mail и сервис `SmtpEmailService` для отправки уведомлений пользователям. Письма отправляются только если включена отправка и у пользователя указан непустой email.

## 1. Включение/выключение отправки

- **`airbnb.mail.enabled`** — глобальный переключатель. При `false` письма не отправляются (логируется только отладочное сообщение).
- В dev-профиле по умолчанию: `airbnb.mail.enabled=false`.
- В prod-шаблоне: `airbnb.mail.enabled=true` (для продакшена с реальной почтой).

## 2. Свойства для SMTP

| Свойство | Описание |
|----------|----------|
| `airbnb.mail.from` | Адрес отправителя (например `noreply@yourdomain.com`) |
| `spring.mail.host` | Хост SMTP-сервера (например `smtp.gmail.com`, `smtp.yandex.ru`) |
| `spring.mail.port` | Порт (обычно 587 для TLS, 465 для SSL) |
| `spring.mail.username` | Логин для аутентификации на SMTP |
| `spring.mail.password` | Пароль или пароль приложения |

Дополнительно при необходимости (например для Gmail/Yandex):

- `spring.mail.properties.mail.smtp.auth=true`
- `spring.mail.properties.mail.smtp.starttls.enable=true`

## 3. Локальная разработка (application-dev.properties)

Почта по умолчанию отключена. Для теста без реальной отправки можно оставить:

```properties
airbnb.mail.enabled=false
spring.mail.host=localhost
spring.mail.port=3025
```

Для локальной проверки отправки поднимите тестовый SMTP (например MailHog на порту 1025/3025) и включите:

```properties
airbnb.mail.enabled=true
airbnb.mail.from=noreply@airbnb.local
spring.mail.host=localhost
spring.mail.port=3025
spring.mail.username=
spring.mail.password=
spring.mail.properties.mail.smtp.auth=false
spring.mail.properties.mail.smtp.starttls.enable=false
```

## 4. Продакшен (application-prod-template.properties)

В шаблоне используются плейсхолдеры, которые подставляются при сборке/деплое (см. `SECRETS_AND_PROPERTIES.md`):

- `@MAIL_FROM@`
- `@MAIL_HOST@`
- `@MAIL_PORT@`
- `@MAIL_USERNAME@`
- `@MAIL_PASSWORD@`

Укажите в секретах репозитория или в процессе деплоя реальные значения. Если в продакшене не нужна отправка писем, в шаблоне можно выставить `airbnb.mail.enabled=false` и задать для остальных плейсхолдеров любые допустимые значения (чтобы конфиг не падал при старте).

## 5. Где используется отправка

- Сервис: `org.mryrt.airbnb.notification.service.email.SmtpEmailService`
- Письма уходят только пользователям с непустым email и только при `airbnb.mail.enabled=true`. При ошибке отправки логируется предупреждение, приложение не падает.
