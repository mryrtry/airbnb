# Проверка по BPMN и сценарии прохождения флоу

Краткий документ: соответствие реализации диаграмме `airbnb.bpmn`, сценарии по ролям, защита эндпоинтов, расширяемость. Подробная привязка каждого шага BPMN к коду и валидация требований — в **`docs/BPMN_AND_IMPLEMENTATION.md`**.

## 0. Защита эндпоинтов (кратко)

- **Публичные (без JWT):** `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh`, `POST /auth/validate`, `GET /health` — настроено в `SecurityConfig` (`/auth/**`, `/health/**` → `permitAll()`).
- **Требуют аутентификации:** все остальные запросы (`anyRequest().authenticated()`).
- **Права по ресурсам:** на каждом контроллере стоит `@PreAuthorize` с нужными `hasAuthority(...)`. Проверка владения ресурсом (гость/владелец бронирования, владелец листинга, участник окна резолюции) вынесена в отдельный слой ответственности — **сервисы доступа** (`*AccessService` в каждом домене): `BookingAccessService`, `ListingAccessService`, `ResolutionAccessService`. Доменные сервисы только вызывают `requireCanRead`, `requireCanUpdate`, `requireIsOwner` и т.д.; вся логика «кто может что» сосредоточена в access-слое (SRP). Админ (`ALL_*`) обходится правилами внутри этих сервисов.

---

## 1. Соответствие BPMN и кода

### 1.1 Покрытые элементы процесса

| BPMN (название / id) | Реализация |
|----------------------|------------|
| **Опубликовать объявление** (Event_0xrs54c → Activity_1lmw3cm) | `ListingController.update(id, status=PUBLISHED)`, `ListingServiceImpl.update()`. Уведомление владельцу о публикации в BPMN есть, в коде не реализовано (самоуведомление не обязательно). |
| **Найти жилье → Список доступного жилья** (Event_18m7ui0 → Activity_0txalis) | `GET /listings?status=PUBLISHED` — клиент должен передать `status=PUBLISHED` для «доступного» жилья. |
| **Подать заявку / Зарегистрировать заявку** (Activity_1v216a5, Activity_0jr0t65) | `POST /bookings`, `BookingServiceImpl.create()` → уведомление владельцу `BOOKING_APPLIED`. |
| **Уведомить владельца** (Activity_0mbm8ir) | Реализовано через `NotificationService.notifyUser(ownerId, BOOKING_APPLIED, ...)`. |
| **Получить уведомление / Принять решение** (Activity_06px8tj, Activity_1517h2c), **Одобрить?** (Gateway_1gugo4m) | Владелец смотрит уведомления `GET /notifications`, решает через `PUT /bookings/{id}/decide` (APPROVED/REJECTED). |
| **Уведомить об отказе** (Activity_15l0dit) | `BookingServiceImpl.decide(REJECTED)` → `BOOKING_REJECTED` гостю. |
| **Уведомить о возможности въезда** (Activity_1n65xzp) | `BookingServiceImpl.decide(APPROVED)` → `BOOKING_APPROVED` гостю. |
| **Въехать / Зарегистрировать въезд / Уведомить владельца о въезде** (Activity_1dkj385, Activity_1dhrvk2, Activity_0tq7ms3) | `PUT /bookings/{id}/check-in` → статус CHECKED_IN, уведомление владельцу `BOOKING_CHECKED_IN`. |
| **Выехать / Зарегистрировать выезд** (Activity_1lms11j, Activity_1xotiem) | `PUT /bookings/{id}/check-out` → статус CHECKED_OUT, уведомление владельцу `BOOKING_CHECKED_OUT`. |
| **Открыть окно разрешения споров** (Sys_OpenResolutionWindow) | Автоматически при выезде: `BookingCheckoutListener` → `ResolutionService.openForBooking(bookingId)`. Опционально вручную: `POST /resolutions/open/{bookingId}`. |
| **Уведомить владельца об открытии окна** (Activity_0llbpfa) | `ResolutionServiceImpl.openForBooking()` → `RESOLUTION_WINDOW_OPENED`. |
| **Действие владельца** (Gw_OwnerChoice): мат. компенсация | `PUT /resolutions/{id}/request-money` → статус MONEY_REQUESTED, гостю `RESOLUTION_MONEY_REQUESTED`. |
| **Действие владельца**: жалоба | `PUT /resolutions/{id}/complaint` → статус COMPLAINT_RECORDED, владельцу `RESOLUTION_COMPLAINT_RECORDED`. |
| **Гость: оплатить** (Flow_GuestPaid) | `PUT /resolutions/{id}/respond-pay` → статус PAID, владельцу `RESOLUTION_PAYMENT_RECEIVED`. |
| **Гость: отказать** (Flow_GuestRefused) | `PUT /resolutions/{id}/respond-refuse` → статус REFUSED, владельцу `RESOLUTION_GUEST_REFUSED`. |
| **Попросить Airbnb вмешаться** (Owner_EscalateAfterRefusal) | `PUT /resolutions/{id}/escalate` (только из REFUSED) → статус ESCALATED, гостю `RESOLUTION_ESCALATED`. |
| **Зафиксировать жалобу / Уведомить владельца о жалобе** (Sys_RecordIssue, Activity_00i2mqr) | Реализовано как `recordComplaint` → `RESOLUTION_COMPLAINT_RECORDED`. |
| **Принять оплату / Уведомить владельца о зафиксированной оплате** (Sys_ReceivePayment, Activity_03b40fh) | `respondPay()` → `RESOLUTION_PAYMENT_RECEIVED`. |
| **Не отвечает 14 дней** (Event_027b8xy) → **Закрыть окно / Уведомить о закрытии** (Activity_1kv4q0h, Activity_0nrufzo) | `ResolutionScheduler` (cron) вызывает `ResolutionService.closeExpiredWindows()`; владельцу `RESOLUTION_WINDOW_CLOSED_AUTO`. |
| **Закрытие окна вручную** | `PUT /resolutions/{id}/close` (только ALL_RESOLUTION_UPDATE) → `RESOLUTION_WINDOW_CLOSED`. |

### 1.2 Не покрытые или упрощённые элементы BPMN

| BPMN | Проблема |
|------|----------|
| **Существенное происшествие?** (Gateway_0guf1pf) | В BPMN после эскалации система ветвится: «Да» → назначить гостю обязательную выплату и уведомить; «Нет» → уведомить владельца об урегулировании. В коде ветвление не реализовано: после эскалации только статус ESCALATED и уведомление гостю. Решение «существенное ли происшествие» могло бы делать админ или отдельный сервис (будущее расширение). |
| **Назначить Гостю обязательную выплату** (Activity_0mozg5p), **Уведомить Гостя об обязательной выплате** (Activity_1t1yyy6) | Нет отдельного статуса «обязательная выплата» и нет принудительного сценария оплаты — только добровольный ответ гостя (pay/refuse). |
| **Уведомить владельца об урегулировании конфликта** (Activity_1w8qgok) при ответе «Нет» на существенное происшествие | Нет автоматического уведомления владельцу «конфликт урегулирован без взыскания» после эскалации. |
| **Есть смысл продолжать?** (Gw_GuestRefused) | В BPMN владелец решает: продолжать (эскалация) или нет (конец). В коде владелец явно вызывает эскалацию; «не продолжать» = просто не вызывать `escalate`. Явного «закрыть без эскалации» из состояния REFUSED нет (можно добавить закрытие окна или оставить как есть). |

Итог: основные сценарии (бронирование, решение, заезд/выезд, окно резолюции, запрос денег, оплата/отказ, эскалация, жалоба, авто-закрытие по таймеру) покрыты. Не покрыты: автоматическое ветвление после эскалации и сценарий «обязательная выплата».

---

## 2. Сценарии прохождения флоу по ролям

### 2.1 Роли и права (кратко)

- **Гость (ROLE_USER как гость)**  
  LISTING_READ, BOOKING_CREATE, BOOKING_READ, RESOLUTION_READ, RESOLUTION_RESPOND, NOTIFICATION_READ и др.

- **Владелец (ROLE_USER как владелец)**  
  LISTING_*, BOOKING_DECIDE, RESOLUTION_READ, RESOLUTION_REQUEST_MONEY, RESOLUTION_ESCALATE, RESOLUTION_COMPLAINT и др.

- **Админ (ROLE_ADMIN)**  
  ALL_* права: чтение/обновление любых сущностей, включая закрытие окна резолюции и просмотр чужих уведомлений.

Проверка «свой ресурс» (гость/владелец бронирования, владелец листинга, участник окна резолюции) выполняется в слое доступа (`BookingAccessService`, `ListingAccessService`, `ResolutionAccessService`): перед действием вызывается `requireCanRead`, `requireCanUpdate`, `requireIsOwner`, `requireIsGuest` и т.д. Списки (GET /bookings, GET /resolutions) для не-админа автоматически ограничены теми сущностями, где пользователь — участник.

---

### 2.2 Сценарий: Гость — от поиска до выезда и ответа по компенсации

1. **Поиск жилья**  
   `GET /listings?status=PUBLISHED` (и при необходимости `titleLike`, пагинация).

2. **Подача заявки**  
   `POST /bookings` с `{"listingId": <id>}`.  
   Ответ 201 + тело бронирования. Владелец получает уведомление `BOOKING_APPLIED`.

3. **Просмотр своих бронирований и уведомлений**  
   `GET /bookings` — возвращаются только бронирования, где текущий пользователь гость или владелец листинга (ограничение в `BookingAccessService.getScopeUserIdForList()`).  
   `GET /notifications` — свои уведомления, например BOOKING_APPROVED или BOOKING_REJECTED.

4. **После одобрения — заезд**  
   `PUT /bookings/{id}/check-in`.  
   Владелец получает `BOOKING_CHECKED_IN`.

5. **Выезд**  
   `PUT /bookings/{id}/check-out`.  
   Владелец получает `BOOKING_CHECKED_OUT`; система автоматически открывает окно резолюции для этого бронирования.

6. **Уведомление о запросе компенсации**  
   Владелец вызывает `PUT /resolutions/{id}/request-money`. Гость получает `RESOLUTION_MONEY_REQUESTED`.  
   Гость смотрит `GET /notifications`, видит запрос.

7. **Ответ гостя**  
   - Оплатить: `PUT /resolutions/{id}/respond-pay` → конфликт урегулирован, владельцу уходит `RESOLUTION_PAYMENT_RECEIVED`.  
   - Отказать: `PUT /resolutions/{id}/respond-refuse` → владельцу `RESOLUTION_GUEST_REFUSED`.

8. **Если гость отказал и владелец эскалировал**  
   Гость получает `RESOLUTION_ESCALATED`. Дальнейшее (обязательная выплата и т.д.) в текущей версии не автоматизировано.

Для гостя важно: в уведомлении приходят поля `relatedBookingId` и `relatedResolutionId` — по ним можно сразу вызвать `PUT /resolutions/{id}/respond-pay` или `respond-refuse`, не делая отдельный запрос `GET /resolutions/booking/{bookingId}`.

---

### 2.3 Сценарий: Владелец — от объявления до решения по заявке и резолюции

1. **Создание и публикация объявления**  
   `POST /listings` (создаётся в DRAFT).  
   `PUT /listings/{id}` с `{"status": "PUBLISHED"}` (и при необходимости title/description).

2. **Уведомление о новой заявке**  
   После `POST /bookings` гостем владелец видит уведомление в `GET /notifications` (тип BOOKING_APPLIED).

3. **Решение по заявке**  
   `PUT /bookings/{id}/decide` с телом `{"status": "APPROVED"}` или `{"status": "REJECTED"}`.  
   Гость получает BOOKING_APPROVED или BOOKING_REJECTED.  
   Доступ: `BookingAccessService.requireCanDecide(id)` — только владелец листинга по этому бронированию или админ.

4. **Уведомления о заезде/выезде**  
   После check-in/check-out владелец получает BOOKING_CHECKED_IN и BOOKING_CHECKED_OUT.

5. **Открытие окна резолюции**  
   Происходит автоматически при выезде. Владелец получает `RESOLUTION_WINDOW_OPENED`.  
   Список окон: `GET /resolutions` — возвращаются только окна по бронированиям, где текущий пользователь гость или владелец листинга (`ResolutionAccessService.getScopeBookingIdsForList()`). В уведомлениях приходят `relatedBookingId` и `relatedResolutionId` для перехода к действиям.

6. **Действия в окне**  
   - Запрос компенсации: `PUT /resolutions/{id}/request-money` с `{"amountRequested": ...}`.  
   - Жалоба без запроса денег: `PUT /resolutions/{id}/complaint` с `{"description": "..."}`.  
   После отказа гостя владелец может вызвать `PUT /resolutions/{id}/escalate`.

7. **Уведомления по резолюции**  
   Владелец получает: RESOLUTION_PAYMENT_RECEIVED, RESOLUTION_GUEST_REFUSED, RESOLUTION_COMPLAINT_RECORDED, RESOLUTION_WINDOW_CLOSED_AUTO (если 14 дней без действий), RESOLUTION_WINDOW_CLOSED (при ручном закрытии админом).

---

### 2.4 Сценарий: Админ

- **Просмотр всех сущностей**  
  `GET /listings`, `GET /bookings`, `GET /resolutions`, `GET /notifications` (в т.ч. чужие при ALL_NOTIFICATION_READ).

- **Помощь по бронированию**  
  `PUT /bookings/{id}/check-in`, `PUT /bookings/{id}/check-out` (ALL_BOOKING_UPDATE).

- **Закрытие окна резолюции**  
  `PUT /resolutions/{id}/close` (ALL_RESOLUTION_UPDATE) → владельцу уходит `RESOLUTION_WINDOW_CLOSED`.

- **Просмотр чужих уведомлений**  
  `GET /notifications` с учётом фильтра; при ALL_NOTIFICATION_READ доступны уведомления других пользователей (контроллер подставляет userId только для обычного пользователя).

Автоматическое «существенное происшествие» и назначение обязательной выплаты админ в текущей реализации не делает через отдельный API — можно расширить (например, отдельный endpoint «назначить обязательную выплату» после эскалации).

---

## 3. Расширяемость

### 3.1 Новый тип уведомления

- Добавить значение в `NotificationType`.
- Добавить константы заголовка/тела в `NotificationMessageTemplates` и ветку в `DefaultNotificationMessageProvider.getContent(...)` (или новый провайдер, если замените дефолтный).
- Вызывающие сервисы вызывают только `notificationService.notifyUser(userId, type, context)` — новых зависимостей не нужно.  
Расширяемость: хорошая; единственное место с перечислением типов — провайдер сообщений.

### 3.2 Новый доменный пакет (например, payments)

- Новый пакет не должен зависеть от notification на уровне сущностей; только от интерфейса `NotificationService`.
- Вызовы: `NotificationService.notifyUser(..., type, context)`. Новые типы — через расширение enum и провайдера (см. выше).  
Добавление нового пакета сервисов не ломает существующие модули при соблюдении зависимостей только на интерфейсы и общие константы/события.

### 3.3 Новый флоу в BPMN (новый процесс)

- **События:** при необходимости — новый тип события (аналог `BookingCheckedOutEvent`) и `@EventListener` в соответствующем пакете.
- **Права:** новые значения в `Permission` и привязка к ролям в `Role`.
- **Уведомления:** новый тип в `NotificationType`, шаблоны и ветка в провайдере.
- **Состояния/агрегаты:** новые сущности и сервисы в своём пакете, без дублирования логики из booking/resolution.  
Архитектура допускает добавление нового флоу без изменения старых доменов, если контракты (события, API) стабильны.

### 3.4 Новые шаблоны сообщений

- Все тексты вынесены в `NotificationMessageTemplates`; контент собирается в `NotificationMessageProvider`. Добавление нового типа уведомления = новая константа + одна ветка в провайдере. Замена провайдера (например, для i18n) возможна через реализацию интерфейса без правок доменной логики.

---

## 4. Замечания и рекомендации

1. **Проверка «свой ресурс»**  
   Реализована в слое доступа: `BookingAccessService` (requireCanRead, requireCanUpdate, requireCanDecide, getScopeUserIdForList), `ListingAccessService` (requireCanModify), `ResolutionAccessService` (requireCanRead, requireIsOwner, requireIsGuest, requireCanClose, getScopeBookingIdsForList). Доменные сервисы вызывают эти методы до выполнения действия; списки GET /bookings и GET /resolutions для не-админа ограничены участником (гость или владелец листинга).

2. **Список «доступного жилья»**  
   BPMN подразумевает «список доступного жилья». В API фильтр по `status=PUBLISHED` есть; клиент передаёт `status=PUBLISHED` при поиске. Опционально: отдельный endpoint «доступные для бронирования» с фиксированным фильтром PUBLISHED.

3. **Ветвление после эскалации (BPMN)**  
   Шлюз «Существенное происшествие?» и сценарии обязательной выплаты / уведомления владельцу об урегулировании не реализованы. Расширение: роли «поддержка» и API для решения «да/нет» и назначения обязательной выплаты.

4. **Таймер 14 дней**  
   Реализован в `ResolutionScheduler` и `ResolutionConstants.WINDOW_DAYS`; закрываются только окна в статусе OPEN. Окна в MONEY_REQUESTED или REFUSED по таймеру не закрываются — при необходимости уточнить в BPMN и доработать.

5. **«Есть смысл продолжать?» (Gw_GuestRefused)**  
   В коде владелец либо вызывает эскалацию (`PUT /resolutions/{id}/escalate`), либо нет. Явного действия «закрыть без эскалации» из состояния REFUSED нет — при желании можно добавить закрытие окна или оставить как есть.

---

## 5. Краткая сводка

- **Покрытие BPMN:** основные шаги процесса (листинг, заявка, решение, заезд, выезд, окно резолюции, запрос денег, оплата/отказ, эскалация, жалоба, авто-закрытие по 14 дням, ручное закрытие) реализованы. Не реализованы: автоматическое ветвление «существенное происшествие» и сценарий обязательной выплаты (см. раздел 1.2).
- **Доступ к ресурсам:** проверки «гость/владелец этого бронирования/листинга/окна» выполняются в *AccessService; списки бронирований и резолюций для не-админа ограничены участником.
- **Сценарии по ролям:** документированы пошагово для гостя, владельца и админа с указанием эндпоинтов и ожидаемых уведомлений.
- **Данные для следующего шага:** в уведомлениях сохраняются `relatedBookingId` и `relatedResolutionId` — клиент может брать `resolutionId` из уведомления для вызова respond-pay/respond-refuse без дополнительного запроса.
- **Расширяемость:** добавление новых типов уведомлений, новых пакетов сервисов, новых флоу и шаблонов возможно без нарушения архитектуры при соблюдении границ модулей и интерфейсов.
- **Подробная валидация:** пошаговая привязка BPMN к коду и перечень непокрытых элементов — в `docs/BPMN_AND_IMPLEMENTATION.md`.
