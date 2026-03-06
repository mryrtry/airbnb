# Прохождение всех флоу через коллекцию API

Коллекция **`docs/airbnb-flows.postman_collection.json`** содержит запросы ко всем основным сценариям: гость, владелец, админ — регистрация, листинги, бронирования, окна разрешения споров, уведомления.

## Импорт коллекции

- **Postman:** File → Import → загрузите `airbnb-flows.postman_collection.json`. Либо перетащите файл в окно Postman.
- **Insomnia:** Import/Export → Import Data → Import from File → выберите JSON-файл коллекции (формат Postman v2.1 поддерживается).
- **Thunder Client (VS Code):** Import → Postman → выберите файл коллекции.

После импорта в коллекции появятся папки: Health, Auth, Listings, Bookings, Resolutions, Notifications, Users. Все эндпоинты API покрыты запросами коллекции (включая PUT /users/{id}, PUT /users/{id}/roles, DELETE /users/{id}).

## Переменные коллекции

| Переменная      | Описание |
|-----------------|----------|
| `baseUrl`       | Базовый URL API (по умолчанию `http://localhost:9000`). Для продакшена замените на ваш хост и порт (например `http://83.166.246.71:8126`). |
| `accessToken`   | JWT access-токен. Подставляется в заголовок `Authorization: Bearer {{accessToken}}`. Заполняется автоматически после запросов Login / Register (скрипты в разделе Tests). |
| `refreshToken`  | Refresh-токен для обновления пары токенов. Заполняется из ответов Login/Register. |
| `listingId`     | Id объявления. Заполняется после **Create listing** и используется в **Create booking** и **Get listing by id**. |
| `bookingId`     | Id бронирования. Заполняется после **Create booking** и используется в decide, check-in, check-out, resolutions. |
| `resolutionId`  | Id окна разрешения споров. Заполняется после **Open resolution for booking** (или берётся из **Get resolution by booking id**). |
| `notificationId`| Id уведомления. Заполняется вручную из ответа **Get all notifications** при необходимости (для Get/Mark read по одному). |
| `userId`        | Id пользователя. Для запросов Get/Update/Delete user задаётся вручную или из ответа **Get all users**. |

Важно: для сценариев с бронированием и резолюциями нужно выполнять запросы **по порядку**, чтобы `listingId` и `bookingId` подставились автоматически. Иначе вручную задайте их в переменных коллекции (например, после просмотра ответа списка листингов/бронирований).

## Порядок выполнения для полного прохождения флоу

Ниже — последовательность запросов, которая покрывает все роли и сценарии. Каждый шаг можно выполнять одним запросом из коллекции (при необходимости переключайте пользователя через Login и повторно запускайте запросы, чтобы обновить `accessToken`).

### 1. Проверка и подготовка

1. **Health check** — убедиться, что сервис доступен по `{{baseUrl}}`.

### 2. Владелец: регистрация и объявление

2. **Auth → Register (owner)** — регистрация владельца (`owner1`). Токен сохранится в переменные.
3. **Listings → Create listing** — создание объявления (DRAFT). В переменные попадёт `listingId`.
4. **Listings → Update listing (publish)** — публикация объявления (статус PUBLISHED).
5. При необходимости: **Listings → Get all listings** / **Get listing by id** — проверить объявление.

### 3. Гость: регистрация и бронирование

6. **Auth → Login (guest)** — если гость уже зарегистрирован; иначе сначала **Register (guest)**, затем при следующем проходе — Login (guest). После логина токен гостя записывается в `accessToken`.
7. **Bookings → Create booking** — создание заявки на бронирование (в теле подставляется `listingId`). В переменные попадёт `bookingId`.

### 4. Владелец: решение по заявке

8. **Auth → Login (owner)** — снова войти владельцем (токен владельца в переменных).
9. **Bookings → Get all bookings** — при необходимости посмотреть заявки.
10. **Bookings → Decide booking (approve)** — одобрить заявку (или **Decide booking (reject)** для отклонения).

### 5. Гость: въезд и выезд

11. **Auth → Login (guest)**.
12. **Bookings → Check-in** — фиксация въезда. Выполнить может только **гость этого бронирования** или админ (ALL_BOOKING_UPDATE); владелец не может.
13. **Bookings → Check-out** — фиксация выезда (также только гость или админ). После выезда по этому бронированию может автоматически открыться окно разрешения споров (зависит от конфигурации приложения).

### 6. Окно разрешения споров (resolution)

14. **Auth → Login (owner)** (или оставить гостя, если открытие разрешено и для гостя).
15. **Resolutions → Open resolution for booking** — открыть окно по `bookingId` (если ещё не открыто автоматически). В переменные попадёт `resolutionId`. Либо **Get resolution by booking id** и вручную задать `resolutionId`.
16. **Resolutions → Request money** — владелец запрашивает компенсацию (в теле указать `amountRequested`).
17. **Auth → Login (guest)**.
18. **Resolutions → Respond pay** или **Respond refuse** — гость соглашается оплатить или отказывается.
19. При отказе: **Auth → Login (owner)** — затем либо **Resolutions → Escalate** (эскалация), либо **Resolutions → Close resolution (admin or owner from REFUSED)** (закрыть без эскалации; владельцу уйдёт RESOLUTION_WINDOW_CLOSED_BY_OWNER).
20. После эскалации: **Auth → Login (admin)** → **Resolutions → Resolve escalation (admin)** с телом `{"substantialIncident": true}` (назначить обязательную выплату гостю) или `{"substantialIncident": false}` (урегулировать без взыскания, уведомить владельца).
21. Если назначена обязательная выплата: **Auth → Login (guest)** → **Resolutions → Respond pay** — гость оплачивает (допускается из статуса MANDATORY_PAYMENT).
22. Альтернатива запросу денег: **Resolutions → Complaint** — владелец подаёт жалобу (без суммы).

### 7. Админ: закрытие окна разрешения

23. **Auth → Login (admin)** — вход под `admin` / `password` (пользователь создаётся при старте приложения).
24. **Resolutions → Close resolution (admin or owner from REFUSED)** — закрытие окна (админ может закрыть любое; владелец — только из REFUSED).

### 8. Уведомления

В любой момент после входа:

23. **Notifications → Get all notifications** — список уведомлений (при необходимости взять `id` и подставить в `notificationId`).
24. **Notifications → Get unread count** — счётчик непрочитанных.
25. **Notifications → Mark notification read** — отметить одно прочитанным (нужен `notificationId`).
26. **Notifications → Mark all notifications read** — отметить все прочитанными.

### 9. Пользователи и токены

27. **Users → Get me** — текущий пользователь по токену.
28. **Users → Get permissions** — права текущего пользователя.
29. **Auth → Refresh token** — обновить access-токен по refresh.
30. **Auth → Validate token** — проверить токен.
31. **Auth → Logout** — выход (инвалидация токена).

При необходимости (под админом):

32. **Users → Get all users (admin)** — список пользователей.
33. **Users → Get user by id (admin)** — пользователь по id (подставьте `{{userId}}` или id вручную).
34. **Users → Update user (admin)** — обновление профиля (username, email, password). PUT `/users/{{userId}}`.
35. **Users → Update user roles (admin)** — обновление ролей. PUT `/users/{{userId}}/roles`, тело: `{"roles": ["ROLE_USER", "ROLE_ADMIN"]}`.
36. **Users → Delete user (admin)** — удаление пользователя. DELETE `/users/{{userId}}`.

## Краткая схема по ролям

- **Гость:** Register/Login → Create booking → Check-in → Check-out (только гость этого бронирования или админ) → (Resolutions: respond-pay / respond-refuse; при обязательной выплате — respond-pay из MANDATORY_PAYMENT) → Notifications.
- **Владелец:** Register/Login → Create listing → Update listing (publish) → Decide booking → (Resolutions: open, request-money, escalate или close из REFUSED, complaint).
- **Админ:** Login (admin) → Resolutions: close, resolve-escalation (существенное происшествие да/нет); Users: get all, get by id.

Запуск всей коллекции подряд (Run collection) возможен только с учётом порядка и переключения пользователей: после каждого Login подставляется новый `accessToken`, поэтому следующие запросы выполняются от имени последнего залогиненного пользователя. Для полного прохождения всех флоу удобнее выполнять запросы по описанному выше порядку вручную, переключая логин при смене роли.

## Справочник эндпоинтов (для сверки с кодом и коллекцией)

| Метод | Путь | Права / примечание |
|-------|------|---------------------|
| GET | /health | Публичный |
| POST | /auth/register | Публичный |
| POST | /auth/login | Публичный |
| POST | /auth/refresh | Публичный |
| POST | /auth/validate | Публичный |
| POST | /auth/logout | isAuthenticated() |
| GET | /users | ALL_USER_READ |
| GET | /users/me | isAuthenticated() |
| GET | /users/permissions | isAuthenticated() |
| GET | /users/{id} | ALL_USER_READ |
| GET | /users/username/{username} | ALL_USER_READ |
| PUT | /users/{id} | ALL_USER_UPDATE |
| PUT | /users/{id}/roles | ALL_USER_UPDATE |
| DELETE | /users/{id} | ALL_USER_DELETE |
| POST | /listings | LISTING_CREATE |
| GET | /listings | LISTING_READ или ALL_LISTING_READ |
| GET | /listings/{id} | LISTING_READ или ALL_LISTING_READ |
| PUT | /listings/{id} | LISTING_UPDATE или ALL_LISTING_UPDATE |
| DELETE | /listings/{id} | LISTING_DELETE или ALL_LISTING_DELETE |
| POST | /bookings | BOOKING_CREATE |
| GET | /bookings | BOOKING_READ или ALL_BOOKING_READ |
| GET | /bookings/{id} | BOOKING_READ или ALL_BOOKING_READ |
| PUT | /bookings/{id}/decide | BOOKING_DECIDE или ALL_BOOKING_UPDATE |
| PUT | /bookings/{id}/check-in | BOOKING_READ или ALL_BOOKING_UPDATE; доступ: только гость этого бронирования или админ |
| PUT | /bookings/{id}/check-out | то же |
| POST | /resolutions/open/{bookingId} | RESOLUTION_READ или ALL_RESOLUTION_UPDATE |
| GET | /resolutions | RESOLUTION_READ или ALL_RESOLUTION_READ |
| GET | /resolutions/{id} | RESOLUTION_READ или ALL_RESOLUTION_READ |
| GET | /resolutions/booking/{bookingId} | RESOLUTION_READ или ALL_RESOLUTION_READ |
| PUT | /resolutions/{id}/request-money | RESOLUTION_REQUEST_MONEY или ALL_RESOLUTION_UPDATE |
| PUT | /resolutions/{id}/respond-pay | RESOLUTION_RESPOND или ALL_RESOLUTION_UPDATE |
| PUT | /resolutions/{id}/respond-refuse | RESOLUTION_RESPOND или ALL_RESOLUTION_UPDATE |
| PUT | /resolutions/{id}/escalate | RESOLUTION_ESCALATE или ALL_RESOLUTION_UPDATE |
| PUT | /resolutions/{id}/complaint | RESOLUTION_COMPLAINT или ALL_RESOLUTION_UPDATE |
| PUT | /resolutions/{id}/close | RESOLUTION_READ или ALL_RESOLUTION_UPDATE |
| PUT | /resolutions/{id}/resolve-escalation | ALL_RESOLUTION_UPDATE; тело: substantialIncident (true/false) |
| GET | /notifications | NOTIFICATION_READ или ALL_NOTIFICATION_READ |
| GET | /notifications/unread-count | то же |
| GET | /notifications/{id} | то же |
| PATCH | /notifications/{id}/read | то же |
| POST | /notifications/mark-all-read | то же |
