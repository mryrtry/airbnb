# Прохождение всех флоу через коллекцию API

Коллекция **`docs/airbnb-flows.postman_collection.json`** содержит запросы ко всем основным сценариям: гость, владелец, админ — регистрация, листинги, бронирования, окна разрешения споров, уведомления.

## Импорт коллекции

- **Postman:** File → Import → загрузите `airbnb-flows.postman_collection.json`. Либо перетащите файл в окно Postman.
- **Insomnia:** Import/Export → Import Data → Import from File → выберите JSON-файл коллекции (формат Postman v2.1 поддерживается).
- **Thunder Client (VS Code):** Import → Postman → выберите файл коллекции.

После импорта в коллекции появятся папки: Health, Auth, Listings, Bookings, Resolutions, Notifications, Users.

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
12. **Bookings → Check-in** — фиксация въезда.
13. **Bookings → Check-out** — фиксация выезда. После выезда по этому бронированию может автоматически открыться окно разрешения споров (зависит от конфигурации приложения).

### 6. Окно разрешения споров (resolution)

14. **Auth → Login (owner)** (или оставить гостя, если открытие разрешено и для гостя).
15. **Resolutions → Open resolution for booking** — открыть окно по `bookingId` (если ещё не открыто автоматически). В переменные попадёт `resolutionId`. Либо **Get resolution by booking id** и вручную задать `resolutionId`.
16. **Resolutions → Request money** — владелец запрашивает компенсацию (в теле указать `amountRequested`).
17. **Auth → Login (guest)**.
18. **Resolutions → Respond pay** или **Respond refuse** — гость соглашается оплатить или отказывается.
19. При отказе: **Auth → Login (owner)** → **Resolutions → Escalate** — эскалация.
20. Альтернатива запросу денег: **Resolutions → Complaint** — владелец подаёт жалобу (без суммы).

### 7. Админ: закрытие окна разрешения

21. **Auth → Login (admin)** — вход под `admin` / `password` (пользователь создаётся при старте приложения).
22. **Resolutions → Close resolution (admin)** — закрытие окна разрешения.

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
33. **Users → Get user by id (admin)** — пользователь по id (в URL замените `1` на нужный id).

## Краткая схема по ролям

- **Гость:** Register/Login → Create booking → Check-in → Check-out → (Resolutions: respond-pay / respond-refuse) → Notifications.
- **Владелец:** Register/Login → Create listing → Update listing (publish) → Decide booking → (Resolutions: open, request-money, escalate, complaint).
- **Админ:** Login (admin) → Resolutions: close; Users: get all, get by id.

Запуск всей коллекции подряд (Run collection) возможен только с учётом порядка и переключения пользователей: после каждого Login подставляется новый `accessToken`, поэтому следующие запросы выполняются от имени последнего залогиненного пользователя. Для полного прохождения всех флоу удобнее выполнять запросы по описанному выше порядку вручную, переключая логин при смене роли.
