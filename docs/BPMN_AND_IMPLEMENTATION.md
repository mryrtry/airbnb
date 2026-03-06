# BPMN и реализация: соответствие кода процессу

Документ позволяет проверить, что реализация в коде выполняет требования диаграммы `airbnb.bpmn`. Для каждого шага процесса указано: что должно происходить по BPMN, как это реализовано (контроллер, сервис, проверки доступа), какие уведомления отправляются и откуда клиент получает данные для следующего запроса. Формулировки привязаны к шагам BPMN без приукрашивания.

**Исходная диаграмма:** корень проекта, файл `airbnb.bpmn`.  
**Процесс:** «Заявки на штрафные санкции Airbnb (владелец → гость)».

---

## 1. Как пользоваться документом

- **Проверка требований:** найдите в разделе 3 шаг BPMN по названию или id — там указано, какой API/код его выполняет и что при этом происходит.
- **Понимание кода:** по цепочке «BPMN шаг → HTTP-метод → контроллер → сервис → доступ» можно пройти от сценария до конкретного класса и метода.
- **Валидация:** раздел 6 перечисляет элементы BPMN, которые не реализованы или реализованы частично; раздел 7 — сводная таблица соответствия.

Роли в процессе: **Гость** (подаёт заявку, заезжает/выезжает, отвечает на запрос компенсации), **Владелец** (публикует объявление, принимает решение по заявке, в окне резолюции запрашивает деньги или подаёт жалобу, эскалирует), **Система** (сервис-таски: регистрация, уведомления, открытие/закрытие окна). Проверки «может ли пользователь выполнить действие с этим ресурсом» выполняют **сервисы доступа** (`*AccessService`), не доменные сервисы.

---

## 2. Обзор процесса по BPMN

Три дорожки (lanes): **Система**, **Владелец**, **Гость**.

**Упрощённая последовательность:**

1. Владелец публикует объявление (Event_0xrs54c → Activity_1lmw3cm → уведомление владельцу).
2. Гость ищет жильё (Event_18m7ui0) → запрашивает список (Activity_0z6omph) → система отдаёт список (Activity_0txalis) → гость выбирает и подаёт заявку (Activity_1v216a5) → система регистрирует заявку (Activity_0jr0t65) и уведомляет владельца (Activity_0mbm8ir).
3. Владелец получает уведомление (Activity_06px8tj), принимает решение (Activity_1517h2c), шлюз «Одобрить?» (Gateway_1gugo4m): нет → уведомление об отказе гостю (Activity_15l0dit) → конец (Event_0m2x6w8); да → уведомление о возможности въезда (Activity_1n65xzp) → гость получает уведомление (Activity_1yn1qo3).
4. Гость въезжает (Activity_1dkj385) → система регистрирует въезд (Activity_1dhrvk2), уведомляет владельца (Activity_0tq7ms3). Гость выезжает (Activity_1lms11j) → система регистрирует выезд (Activity_1xotiem), уведомляет владельца (Activity_1wrvbq1) и **открывает окно разрешения споров** (Sys_OpenResolutionWindow).
5. Система уведомляет владельца об открытии окна (Activity_0llbpfa), владелец получает уведомление (Activity_0zsseu5) → шлюз «Действие владельца» (Gw_OwnerChoice): **мат. компенсация** (Flow_ChoiceToRequestMoney) или **жалоба** (Flow_ChoiceToReportIssue).
6. **Ветка «Мат. компенсация»:** Owner_RequestMoney → Sys_SendMoneyRequest → Sys_NotifyGuest → Guest_ReceiveRequest → Guest_RespondPayOrRefuse → Gw_GuestDecision. Оплатил → Sys_ReceivePayment → уведомление владельцу о зафиксированной оплате (Activity_03b40fh) → Event_0ka61nm (конфликт урегулирован). Отказал → Gw_GuestRefused (есть смысл продолжать?) → да: Owner_EscalateAfterRefusal → Sys_HandleEscalation → Gateway_0guf1pf (существенное происшествие?) → да: назначить обязательную выплату и уведомить гостя; нет: уведомить владельца об урегулировании → конец. Нет смысла продолжать → Event_0jpe54e (конфликт урегулирован).
7. **Ветка «Жалоба»:** Owner_AskAirbnbToIntervene → Sys_RecordIssue → уведомить владельца о жалобе (Activity_00i2mqr) → Event_1nihhyy (конфликт урегулирован).
8. **Таймер:** Event_027b8xy (не отвечает 14 дней) → Activity_1kv4q0h (закрыть окно) → Activity_0nrufzo (уведомить владельца о закрытии) → Event_0fqgy57 (конец).

Ниже каждый из этих блоков разобран с привязкой к коду.

---

## 3. Реализация по шагам BPMN

### 3.1 Публикация объявления (дорога Владелец/Система)

| BPMN | Элемент | Требование |
|------|--------|------------|
| Event_0xrs54c | Start | «Опубликовать объявление о сдаче жилья» |
| Activity_1lmw3cm | Service | «Опубликовать объявление о сдаче жилья» |
| Activity_1r7wjdn | Service | «Уведомить владельца» |
| Activity_0qi5hke | Receive | Владелец получает уведомление |
| Event_1lsw8is | End | — |

**Реализация.**

- Создание объявления: листинг создаётся в статусе `DRAFT`.  
  **Код:** `ListingController` → `POST /listings` (право `LISTING_CREATE`) → `ListingServiceImpl.create()` — `ListingStatus.DRAFT`, `ownerId` = текущий пользователь (`UserService.getAuthenticatedUser().getId()`).  
  Файл: `src/main/java/org/mryrt/airbnb/listing/service/ListingServiceImpl.java`.

- Публикация (перевод в «опубликовано»): обновление листинга с `status=PUBLISHED`.  
  **Код:** `PUT /listings/{id}` (право `LISTING_UPDATE` или `ALL_LISTING_UPDATE`) → `ListingServiceImpl.update()`. Перед изменением вызывается `ListingAccessService.requireCanModify(id)` — доступ только у владельца листинга или админа.  
  Файлы: `ListingController`, `ListingServiceImpl`, `ListingAccessServiceImpl` (папка `listing/access/`).

- Уведомление владельцу о публикации (Activity_1r7wjdn, Activity_0qi5hke): при смене статуса на PUBLISHED в `ListingServiceImpl.update()` вызывается `notificationService.notifyUser(listing.getOwnerId(), NotificationType.LISTING_PUBLISHED, Map.of("listingId", listing.getId()))`.

---

### 3.2 Поиск жилья и подача заявки (дорога Гость + Система)

| BPMN | Элемент | Требование |
|------|--------|------------|
| Event_18m7ui0 | Start | «Найти жилье» |
| Activity_0z6omph | User | «Запросить список доступного жилья» |
| Activity_0txalis | Service | «Отдать список доступного жилья» |
| Activity_0jlb1y7 | User | «Получить список доступного жилья» |
| Activity_1qvf11h | User | «Выбрать жилье» |
| Activity_1v216a5 | User | «Подать заявку» |
| Activity_0jr0t65 | Service | «Зарегистрировать заявку» |
| Activity_0mbm8ir | Service | «Уведомить владельца» |

**Реализация.**

- Запрос списка доступного жилья: в BPMN подразумевается только жильё, доступное для бронирования. В коде «доступное» = листинги со статусом `PUBLISHED`. Клиент сам передаёт фильтр `status=PUBLISHED`.  
  **Код:** `GET /listings?status=PUBLISHED` (и при необходимости `titleLike`, пагинация). Контроллер: `ListingController.getAll()`; сервис: `ListingServiceImpl.getAll()`; фильтр: `ListingFilter.status`, спецификации: `ListingSpecifications.withFilter()`. Отдельного эндпоинта «только доступное» нет — обязанность передать `status=PUBLISHED` лежит на клиенте.

- Подача заявки: создание бронирования от имени текущего пользователя (гость).  
  **Код:** `POST /bookings` с телом `{"listingId": <id>}`. Право: `BOOKING_CREATE`.  
  `BookingController.create()` → `BookingServiceImpl.create()`: проверяется существование листинга (`ListingService.getEntity(listingId)`), `guestId` берётся из `UserService.getAuthenticatedUser().getId()`, создаётся запись со статусом `APPLIED`, сохраняется в `BookingRepository`, публикуется событие `EntityEvent(CREATED, dto)`, вызывается `NotificationService.notifyUser(listing.getOwnerId(), BOOKING_APPLIED, Map.of("listingId", ..., "bookingId", ...))`.  
  Файлы: `booking/controller/BookingController.java`, `booking/service/BookingServiceImpl.java`.

- Уведомление владельца (Activity_0mbm8ir): выполняется тем же вызовом `notifyUser(..., BOOKING_APPLIED, ...)`. Текст письма формируется в `DefaultNotificationMessageProvider` по типу `BOOKING_APPLIED` и контексту; сохранение уведомления и опциональная отправка email — в `NotificationServiceImpl.notifyUser(userId, type, context)`.

Итог: шаги «запросить список», «отдать список», «подать заявку», «зарегистрировать заявку», «уведомить владельца» покрыты. Список «доступного» жилья обеспечивается передачей `status=PUBLISHED` в `GET /listings`.

---

### 3.3 Решение владельца по заявке (одобрить / отклонить)

| BPMN | Элемент | Требование |
|------|--------|------------|
| Activity_06px8tj | Receive | Владелец «Получить уведомление» |
| Activity_1517h2c | User | «Принять решение по заявке» |
| Gateway_1gugo4m | Gateway | «Одобрить?» |
| Flow_1ejtifz | — | нет → отказ |
| Activity_15l0dit | Service | «Уведомить пользователя об отказе» |
| Activity_0fgtvuy | Receive | Гость получает уведомление |
| Event_0m2x6w8 | End | «Конец» |
| Flow_13p9a50 | — | да → одобрение |
| Activity_1n65xzp | Service | «Уведомить пользователя о возможности въезда» |
| Activity_1yn1qo3 | Receive | Гость получает уведомление |

**Реализация.**

- Получение уведомления владельцем: владелец вызывает `GET /notifications` (право `NOTIFICATION_READ` или `ALL_NOTIFICATION_READ`). В контроллере для пользователя без `ALL_NOTIFICATION_READ` в фильтр подставляется `userId` текущего пользователя — видны только свои уведомления.  
  Файл: `notification/controller/NotificationController.java` (метод `getAll`).

- Принятие решения: один вызов API с выбором «одобрить» или «отклонить».  
  **Код:** `PUT /bookings/{id}/decide` с телом `{"status": "APPROVED"}` или `{"status": "REJECTED"}`. Право: `BOOKING_DECIDE` или `ALL_BOOKING_UPDATE`.  
  `BookingController.decide()` → `BookingServiceImpl.decide()`: сначала `BookingAccessService.requireCanDecide(id)` — разрешено только владельцу листинга по этому бронированию или админу; затем загрузка бронирования, проверка статуса `APPLIED`, установка нового статуса, сохранение, публикация `EntityEvent(UPDATED, dto)`; затем `notifyUser(booking.getGuestId(), BOOKING_APPROVED или BOOKING_REJECTED, Map.of("bookingId", id))`.  
  Файлы: `booking/controller/BookingController.java`, `booking/service/BookingServiceImpl.java`, `booking/access/BookingAccessServiceImpl.java` (метод `requireCanDecide`).

- Шлюз «Одобрить?» в BPMN — это ветвление по решению пользователя; в коде оно выражается значением `request.getStatus()` (APPROVED/REJECTED) и соответствующей отправкой уведомления гостю (BOOKING_APPROVED или BOOKING_REJECTED). Отдельного шлюза в коде нет — только два варианта в одном методе.

Итог: «получить уведомление», «принять решение», «одобрить?», «уведомить об отказе»/«уведомить о возможности въезда» и получение уведомления гостем реализованы через `PUT /bookings/{id}/decide` и типы уведомлений BOOKING_REJECTED / BOOKING_APPROVED.

---

### 3.4 Въезд и выезд гостя

| BPMN | Элемент | Требование |
|------|--------|------------|
| Activity_1dkj385 | User | «Въехать» |
| Activity_1dhrvk2 | Service | «Зарегистрировать въезд» |
| Activity_0tq7ms3 | Service | «Уведомить владельца о въезде» |
| Activity_0rk5iq2 | Receive | Владелец получает уведомление |
| Activity_1lms11j | User | «Выехать» |
| Activity_1xotiem | Service | «Зарегистрировать выезд» (два исходящих потока) |
| Activity_1wrvbq1 | Service | «Уведомить владельца о выезде» |
| Activity_0gt6036 | Receive | Владелец получает уведомление |
| Flow_00tgyi1 | — | От выезда → Sys_OpenResolutionWindow |

**Реализация.**

- Регистрация въезда: один вызов API переводит бронирование в CHECKED_IN и уведомляет владельца.  
  **Код:** `PUT /bookings/{id}/check-in`. Право: `BOOKING_READ` или `ALL_BOOKING_UPDATE`.  
  `BookingController.recordCheckIn()` → `BookingServiceImpl.recordCheckIn()`: `BookingAccessService.requireCanCheckInCheckOut(id)` — доступ только у **гостя этого бронирования** или админа (ALL_BOOKING_UPDATE); владелец листинга без админских прав не может выполнять check-in; проверка статуса APPROVED; установка CHECKED_IN и `checkInAt`, сохранение; `notifyUser(listing.getOwnerId(), BOOKING_CHECKED_IN, Map.of("bookingId", id))`.  
  Файлы: `BookingController`, `BookingServiceImpl`, `booking/access/BookingAccessServiceImpl.requireCanCheckInCheckOut`.

- Регистрация выезда: перевод в CHECKED_OUT, уведомление владельца и **автоматическое открытие окна резолюции**.  
  **Код:** `PUT /bookings/{id}/check-out`. Право: те же, что для check-in.  
  `BookingServiceImpl.recordCheckOut()`: `requireCanCheckInCheckOut(id)` — только гость этого бронирования или админ; проверка статуса CHECKED_IN, установка CHECKED_OUT и `checkOutAt`, сохранение, `EntityEvent(UPDATED, dto)`, `notifyUser(..., BOOKING_CHECKED_OUT, ...)`, затем `eventPublisher.publishEvent(new BookingCheckedOutEvent(booking.getId()))`.  
  Событие обрабатывается в `BookingCheckoutListener` (`resolution/config/BookingCheckoutListener.java`): при получении `BookingCheckedOutEvent` вызывается `ResolutionService.openForBooking(event.getBookingId())`. Таким образом шаг BPMN «Зарегистрировать выезд» и переход Flow_00tgyi1 к Sys_OpenResolutionWindow реализованы: выезд и открытие окна выполняются в одной транзакции выезда плюс асинхронный слушатель.

Итог: «въехать», «зарегистрировать въезд», «уведомить владельца о въезде», «выехать», «зарегистрировать выезд», «уведомить владельца о выезде» и запуск открытия окна резолюции реализованы; владелец получает уведомления через общий механизм уведомлений (GET /notifications).

---

### 3.5 Открытие окна разрешения споров и уведомление владельца

| BPMN | Элемент | Требование |
|------|--------|------------|
| Sys_OpenResolutionWindow | Service | «Открыть окно разрешения споров для владельца» |
| Activity_0llbpfa | Service | «Уведомить Владельца об открытии окна» |
| Flow_11gn570 | — | К receive-задаче владельца |
| Activity_0zsseu5 | Receive | Владелец «Получить уведомление» |
| Flow_044fi06 | — | К шлюзу «Действие владельца» |
| Gw_OwnerChoice | Gateway | «Действие владельца» |

**Реализация.**

- Открытие окна: вызывается при выезде из `BookingCheckoutListener`, либо вручную.  
  **Код (автоматически):** после `recordCheckOut()` публикуется `BookingCheckedOutEvent`, в `BookingCheckoutListener.onBookingCheckedOut()` вызывается `resolutionService.openForBooking(event.getBookingId())`.  
  **Код (вручную):** `POST /resolutions/open/{bookingId}` (право `RESOLUTION_READ` или `ALL_RESOLUTION_UPDATE`).  
  `ResolutionServiceImpl.openForBooking(bookingId)`: загрузка бронирования, проверка статуса CHECKED_OUT; если окно для этого `bookingId` уже есть — возврат существующего; иначе создаётся `ResolutionWindow` (status OPEN, `openedAt` = now), сохранение, `notifyUser(listing.getOwnerId(), RESOLUTION_WINDOW_OPENED, Map.of("bookingId", bookingId, "resolutionId", window.getId()))`.  
  Файлы: `resolution/config/BookingCheckoutListener.java`, `resolution/service/ResolutionServiceImpl.java`, `resolution/controller/ResolutionController.java` (POST /open/{bookingId}).

- Уведомление владельца об открытии окна (Activity_0llbpfa) и получение уведомления владельцем (Activity_0zsseu5): выполняются тем же вызовом `notifyUser(..., RESOLUTION_WINDOW_OPENED, ...)`. В уведомлении сохраняются `relatedBookingId` и `relatedResolutionId` (`NotificationServiceImpl` извлекает их из context), так что владелец из ответа GET /notifications или из одного уведомления по id может получить `resolutionId` для следующих запросов (request-money, complaint).

- Шлюз «Действие владельца» (Gw_OwnerChoice) в BPMN имеет два исходящих потока: «Мат компенсация» и «Жалоба». В коде это не один шлюз, а два разных действия по одному и тому же окну резолюции: вызов `PUT /resolutions/{id}/request-money` или `PUT /resolutions/{id}/complaint`. Выбор делает клиент (владелец).

Итог: открытие окна (автоматически при выезде или вручную), уведомление владельца и переход к выбору действия реализованы. Данные для следующего шага: владелец получает `resolutionId` из уведомления (`relatedResolutionId`) или из `GET /resolutions` / `GET /resolutions/booking/{bookingId}`.

---

### 3.6 Ветка «Мат. компенсация»: запрос денег, уведомление гостя, ответ гостя

| BPMN | Элемент | Требование |
|------|--------|------------|
| Flow_ChoiceToRequestMoney | — | Выбор «Мат компенсация» |
| Owner_RequestMoney | User | «Запросить мат компенсацию» |
| Sys_SendMoneyRequest | Service | «Отправить запрос на оплату гостю» |
| Sys_NotifyGuest | Service | «Уведомить гостя о запросе» |
| Guest_ReceiveRequest | Receive | Гость «Получить запрос на оплату» |
| Guest_RespondPayOrRefuse | User | «Ответить: оплатить или отказать» |
| Gw_GuestDecision | Gateway | «Оплатил / Отказал?» |

**Реализация.**

- Запрос владельца и отправка запроса гостю (Owner_RequestMoney + Sys_SendMoneyRequest + Sys_NotifyGuest): один вызов API переводит окно в MONEY_REQUESTED и отправляет уведомление гостю.  
  **Код:** `PUT /resolutions/{id}/request-money` с телом `{"amountRequested": <число>}`. Право: `RESOLUTION_REQUEST_MONEY` или `ALL_RESOLUTION_UPDATE`.  
  `ResolutionController.requestMoney()` → `ResolutionServiceImpl.requestMoney()`: `ResolutionAccessService.requireIsOwner(id)` (только владелец листинга по этому бронированию или админ); загрузка окна, проверка статуса OPEN; установка MONEY_REQUESTED и `amountRequested`, сохранение; `notifyUser(b.getGuestId(), RESOLUTION_MONEY_REQUESTED, Map.of("bookingId", ..., "amount", ..., "resolutionId", window.getId()))`.  
  Файлы: `resolution/controller/ResolutionController.java`, `resolution/service/ResolutionServiceImpl.java`, `resolution/access/ResolutionAccessServiceImpl.requireIsOwner`.

- Получение запроса гостем (Guest_ReceiveRequest): гость вызывает `GET /notifications`; в уведомлении типа RESOLUTION_MONEY_REQUESTED приходят `relatedBookingId` и `relatedResolutionId`, по которым можно сразу вызвать respond-pay или respond-refuse без дополнительного запроса по bookingId.

- Ответ гостя (Guest_RespondPayOrRefuse) и шлюз «Оплатил / Отказал?» (Gw_GuestDecision): два отдельных эндпоинта.  
  **Оплатил:** `PUT /resolutions/{id}/respond-pay`. Право: `RESOLUTION_RESPOND` или `ALL_RESOLUTION_UPDATE`.  
  `ResolutionServiceImpl.respondPay()`: `ResolutionAccessService.requireIsGuest(id)` (только гость этого бронирования или админ); статус окна должен быть MONEY_REQUESTED; переход в PAID, `closedAt` = now, сохранение; `notifyUser(listing.getOwnerId(), RESOLUTION_PAYMENT_RECEIVED, Map.of("bookingId", ..., "resolutionId", ...))`.  
  **Отказал:** `PUT /resolutions/{id}/respond-refuse`. Те же права и проверка гостя; переход в REFUSED, сохранение; `notifyUser(listing.getOwnerId(), RESOLUTION_GUEST_REFUSED, ...)`.  
  Файлы: те же контроллер и сервис; доступ: `ResolutionAccessServiceImpl.requireIsGuest`.

Итог: запрос мат. компенсации, отправка запроса гостю, уведомление гостя, получение запроса гостем и ответ «оплатить/отказать» реализованы. Идентификатор окна для ответа гость берёт из уведомления (`relatedResolutionId`) или из `GET /resolutions/booking/{bookingId}`.

---

### 3.7 Ветка «Оплатил»: приём оплаты и уведомление владельца

| BPMN | Элемент | Требование |
|------|--------|------------|
| Flow_GuestPaid | — | «Оплатил» |
| Sys_ReceivePayment | Service | «Принять оплату от гостя» |
| Activity_03b40fh | Service | «Уведомить владельца о зафиксированной оплате» |
| Activity_0ynw3fz | Receive | Владелец получает уведомление |
| Event_0ka61nm | End | «Конфликт урегулирован» |

**Реализация.**

В BPMN «принять оплату» и «уведомить владельца о зафиксированной оплате» — отдельные задачи. В коде реальных платёжных операций нет: вызов `respond-pay` считается согласием гостя на оплату и сразу переводит окно в PAID и отправляет владельцу уведомление RESOLUTION_PAYMENT_RECEIVED. То есть Sys_ReceivePayment и Activity_03b40fh совмещены в одном действии `ResolutionServiceImpl.respondPay()`. Владелец видит уведомление через GET /notifications. Конфликт считается урегулированным (Event_0ka61nm) с точки зрения статуса окна (PAID) и уведомления; отдельного «завершения процесса» в коде нет.

---

### 3.8 Ветка «Отказал»: эскалация и шлюз «Существенное происшествие?»

| BPMN | Элемент | Требование |
|------|--------|------------|
| Flow_GuestRefused | — | «Отказал» |
| Gw_GuestRefused | Gateway | «Есть смысл продолжать?» |
| Flow_1lkc774 | — | Нет → Event_0jpe54e (конфликт урегулирован) |
| Flow_RefusedToOwnerEscalate | — | Да → Owner_EscalateAfterRefusal |
| Owner_EscalateAfterRefusal | User | «Попросить Airbnb вмешаться» |
| Sys_HandleEscalation | Service | «Обработать запрос вмешательства» |
| Gateway_0guf1pf | Gateway | «Существенное происшествие?» |
| Flow_1a6th2p | — | Да → Activity_0mozg5p (назначить обязательную выплату) |
| Flow_1olgfbz | — | Нет → Activity_1w8qgok (уведомить владельца об урегулировании) |

**Реализация.**

- «Есть смысл продолжать?» (Gw_GuestRefused): в BPMN это решение владельца. В коде владелец явно вызывает эскалацию; если не вызывает — окно остаётся в REFUSED, отдельного «закрыть без эскалации» или явного перехода к Event_0jpe54e нет (можно добавить закрытие окна или оставить как есть).

- Эскалация (Owner_EscalateAfterRefusal → Sys_HandleEscalation):  
  **Код:** `PUT /resolutions/{id}/escalate`. Право: `RESOLUTION_ESCALATE` или `ALL_RESOLUTION_UPDATE`.  
  `ResolutionServiceImpl.escalate()`: `requireIsOwner(id)`; статус окна должен быть REFUSED; переход в ESCALATED, сохранение; `notifyUser(b.getGuestId(), RESOLUTION_ESCALATED, Map.of("bookingId", ..., "resolutionId", ...))`.  
  Файл: `ResolutionServiceImpl.escalate()`.

- Шлюз «Существенное происшествие?» (Gateway_0guf1pf): админ вызывает `PUT /resolutions/{id}/resolve-escalation` с телом `{"substantialIncident": true/false}`. Право: только `ALL_RESOLUTION_UPDATE`.  
  **Код:** `ResolutionServiceImpl.resolveEscalation()`: `requireCanResolveEscalation(id)`; статус окна должен быть ESCALATED. При `substantialIncident: true` — переход в MANDATORY_PAYMENT, сохранение; `notifyUser(guestId, RESOLUTION_MANDATORY_PAYMENT_ASSIGNED, Map.of("bookingId", ..., "amount", window.getAmountRequested(), "resolutionId", ...))`. При `false` — переход в CLOSED, `closedAt`, сохранение; `notifyUser(ownerId, RESOLUTION_RESOLVED_WITHOUT_PAYMENT, ...)`.

- Назначить гостю обязательную выплату (Activity_0mozg5p), уведомить гостя об обязательной выплате (Activity_1t1yyy6): реализованы в `resolveEscalation(..., substantialIncident: true)` — статус MANDATORY_PAYMENT, уведомление гостю. Гость может оплатить через тот же `PUT /resolutions/{id}/respond-pay` (допускаются статусы MONEY_REQUESTED и MANDATORY_PAYMENT).

- Уведомить владельца об урегулировании конфликта (Activity_1w8qgok): реализовано в `resolveEscalation(..., substantialIncident: false)` — уведомление `RESOLUTION_RESOLVED_WITHOUT_PAYMENT`.

- «Есть смысл продолжать?» (Gw_GuestRefused): владелец может закрыть окно без эскалации — `PUT /resolutions/{id}/close` из статуса REFUSED (в `requireCanClose` разрешён владелец при REFUSED); владельцу уходит `RESOLUTION_WINDOW_CLOSED_BY_OWNER`.

Итог: эскалация, ветвление по существенному происшествию, обязательная выплата, уведомление владельцу об урегулировании без взыскания и закрытие владельцем без эскалации реализованы.

---

### 3.9 Ветка «Жалоба»: подать жалобу на гостя

| BPMN | Элемент | Требование |
|------|--------|------------|
| Flow_ChoiceToReportIssue | — | «Жалоба» |
| Owner_AskAirbnbToIntervene | User | «Подать жалобу на Гостя» |
| Sys_RecordIssue | Service | «Зафиксировать жалобу» |
| Activity_00i2mqr | Service | «Уведомить владельца о зафиксированной жалобе» |
| Activity_0dhvmlc | Receive | Владелец получает уведомление |
| Event_1nihhyy | End | «Конфликт урегулирован» |

**Реализация.**

Один вызов API фиксирует жалобу и уведомляет владельца (в BPMN владелец же и подаёт жалобу, так что уведомление — подтверждение фиксации).  
**Код:** `PUT /resolutions/{id}/complaint` с телом `{"description": "..."}`. Право: `RESOLUTION_COMPLAINT` или `ALL_RESOLUTION_UPDATE`.  
`ResolutionServiceImpl.recordComplaint()`: `requireIsOwner(id)`; статус окна OPEN; установка COMPLAINT_RECORDED, `complaintDescription`, `closedAt`, сохранение; `notifyUser(listing.getOwnerId(), RESOLUTION_COMPLAINT_RECORDED, ...)`.  
Файлы: `ResolutionController`, `ResolutionServiceImpl.recordComplaint()`.  
Итог: подача жалобы, фиксация и уведомление владельца реализованы.

---

### 3.10 Таймер «Не отвечает 14 дней» и закрытие окна

| BPMN | Элемент | Требование |
|------|--------|------------|
| Event_027b8xy | Timer | «Не отвечает 14 дней» |
| Activity_1kv4q0h | Service | «Закрыть окно разрешения споров» |
| Activity_0nrufzo | Service | «Уведомить владельца о закрытие окна» |
| Activity_1o84a61 | Receive | Владелец получает уведомление |
| Event_0fqgy57 | End | «Конец» |

**Реализация.**

- Таймер в BPMN — промежуточное событие по времени (14 дней). В коде таймер реализован задачей по расписанию: раз в час (по умолчанию) вызывается метод, который закрывает просроченные окна.  
  **Код:** `ResolutionScheduler.closeExpiredResolutionWindows()` вызывает `ResolutionService.closeExpiredWindows()`.  
  В `ResolutionServiceImpl.closeExpiredWindows()`: закрываются только окна в статусе OPEN и `openedAt` старше 14 дней (владелец не запросил компенсацию и не подал жалобу — «решил не открывать» окно). Окна в MONEY_REQUESTED, REFUSED и др. по таймеру не закрываются. Для каждого просроченного OPEN — CLOSED, `closedAt`, `notifyUser(..., RESOLUTION_WINDOW_CLOSED_AUTO, ...)`.  
  Файлы: `resolution/config/ResolutionScheduler.java`, `resolution/constants/ResolutionConstants.java`, `resolution/service/ResolutionServiceImpl.closeExpiredWindows()`, `ResolutionRepository.findByStatusAndOpenedAtBefore`.

- Ручное закрытие окна:  
  **Код:** `PUT /resolutions/{id}/close`. Право: `RESOLUTION_READ` или `ALL_RESOLUTION_UPDATE` (доступ владельца нужен для «закрыть без эскалации» из REFUSED).  
  `ResolutionServiceImpl.close()`: `requireCanClose(id)` разрешает админа (ALL_RESOLUTION_UPDATE) или владельца листинга при статусе REFUSED. Установка CLOSED и `closedAt`; при закрытии владельцем из REFUSED — `notifyUser(..., RESOLUTION_WINDOW_CLOSED_BY_OWNER, ...)`, иначе — `RESOLUTION_WINDOW_CLOSED`.

Итог: автоматическое закрытие по таймеру только для OPEN (владелец не задействовал окно); ручное закрытие — админом всегда, владельцем из REFUSED (без эскалации).

---

## 4. Защита доступа

Проверки выполняются в два уровня.

1. **Контроллер:** `@PreAuthorize("hasAuthority('...')")` — проверяется наличие у пользователя права (например, BOOKING_READ, RESOLUTION_REQUEST_MONEY). Без нужного права запрос не дойдёт до сервиса.

2. **Сервис доступа (resource-level):** доменный сервис перед действием вызывает соответствующий *AccessService. Тот проверяет, что текущий пользователь — участник ресурса (гость бронирования, владелец листинга) или админ (ALL_*), и при несоответствии бросает `ServiceException(CANNOT_ACCESS_SOURCE, ...)`.

Соответствие по доменам:

- **Бронирования:** `BookingAccessService` — `requireCanRead(bookingId)`, `requireCanUpdate(bookingId)`, `requireCanCheckInCheckOut(bookingId)`, `requireCanDecide(bookingId)`, `getScopeUserIdForList()`. Check-in и check-out разрешены только гостю этого бронирования или админу (ALL_BOOKING_UPDATE); владелец листинга не может их выполнять. Список бронирований (GET /bookings) для не-админа ограничивается теми, где пользователь гость или владелец листинга (`BookingSpecifications.scopeForUser`, `findWithFilter(..., scopeUserId)`).  
  Файлы: `booking/access/BookingAccessService.java`, `BookingAccessServiceImpl.java`, `BookingSpecifications.java`, `BookingRepository.findWithFilter(filter, pageable, scopeUserId)`.

- **Листинги:** `ListingAccessService.requireCanModify(listingId)` — только владелец листинга или админ (ALL_LISTING_UPDATE / ALL_LISTING_DELETE). Вызывается из `ListingServiceImpl.update()` и `delete()`.

- **Резолюции:** `ResolutionAccessService` — `requireCanRead(resolutionId)`, `requireIsOwner(resolutionId)`, `requireIsGuest(resolutionId)`, `requireCanClose(resolutionId)`, `getScopeBookingIdsForList()`. Список резолюций (GET /resolutions) для не-админа ограничивается окнами по бронированиям, где пользователь гость или владелец листинга (`ResolutionSpecifications.withFilter(..., scopeBookingIds)`, список bookingId из `BookingRepository.findBookingIdsByGuestOrOwner(userId)`).  
  Файлы: `resolution/access/ResolutionAccessService.java`, `ResolutionAccessServiceImpl.java`, `ResolutionSpecifications`, `ResolutionRepository.findWithFilter(..., scopeBookingIds)`, `BookingRepository.findBookingIdsByGuestOrOwner`.

- **Уведомления:** список и операции (GET, mark read) привязаны к текущему пользователю; при отсутствии ALL_NOTIFICATION_READ в фильтр подставляется его userId; доступ к чужому уведомлению по id возвращает 403.  
  Файл: `notification/controller/NotificationController.java`.

Публичные эндпоинты (без проверки JWT): `/auth/**`, `/health/**` — заданы в `auth/config/SecurityConfig.java` в `authorizeHttpRequests`.

---

## 5. Данные для перехода между шагами

Чтобы клиент мог выполнить следующий шаг процесса, ему нужны идентификаторы сущностей. Откуда они берутся:

- **listingId для подачи заявки:** из ответа `GET /listings` (в элементах списка есть `id`).
- **bookingId:** после `POST /bookings` возвращается созданное бронирование с `id`; также приходит в уведомлениях владельцу и гостю (в контексте уведомлений и в полях `relatedBookingId` у сущности Notification).
- **resolutionId:** после открытия окна его возвращает `POST /resolutions/open/{bookingId}` или `GET /resolutions/booking/{bookingId}`. В уведомлениях, связанных с резолюцией, в сущности Notification сохраняются `relatedBookingId` и `relatedResolutionId` (заполняются в `NotificationServiceImpl.notifyUser(..., context)` из context при наличии ключей `bookingId` и `resolutionId`). Клиент может брать `resolutionId` из ответа GET /notifications или GET /notifications/{id} и сразу вызывать, например, `PUT /resolutions/{id}/respond-pay` без дополнительного запроса по bookingId.

Модель уведомления: `notification/model/Notification.java` (поля `relatedBookingId`, `relatedResolutionId`); DTO: `notification/dto/NotificationDto.java`. Контекст с `resolutionId` передаётся из `ResolutionServiceImpl` во всех вызовах `notifyUser` по резолюциям.

---

## 6. Покрытие BPMN

Все элементы процесса из диаграммы `airbnb.bpmn` реализованы: уведомление владельцу о публикации листинга, таймер 14 дней только для статуса OPEN (владелец не задействовал окно), закрытие владельцем без эскалации из REFUSED, шлюз «Существенное происшествие?» (resolve-escalation), обязательная выплата (MANDATORY_PAYMENT), уведомление владельцу об урегулировании без взыскания. Упрощений по сравнению с BPMN нет.

---

## 7. Сводная таблица соответствия BPMN и кода

| Шаг / сценарий BPMN | Реализация в коде | Эндпоинт / компонент |
|---------------------|-------------------|----------------------|
| Опубликовать объявление | Создание листинга (DRAFT) + обновление status=PUBLISHED | POST /listings, PUT /listings/{id} |
| Уведомить владельца о публикации | notifyUser(LISTING_PUBLISHED) при переходе в PUBLISHED | ListingServiceImpl.update() |
| Список доступного жилья | GET /listings с filter status=PUBLISHED (передаёт клиент) | GET /listings |
| Подать заявку, зарегистрировать, уведомить владельца | create() + notifyUser(BOOKING_APPLIED) | POST /bookings |
| Получить уведомление / принять решение | GET /notifications, затем decide() | GET /notifications, PUT /bookings/{id}/decide |
| Одобрить? нет → уведомить об отказе | decide(REJECTED) → BOOKING_REJECTED | PUT /bookings/{id}/decide |
| Одобрить? да → уведомить о возможности въезда | decide(APPROVED) → BOOKING_APPROVED | PUT /bookings/{id}/decide |
| Въехать, зарегистрировать въезд, уведомить владельца | recordCheckIn() + BOOKING_CHECKED_IN | PUT /bookings/{id}/check-in |
| Выехать, зарегистрировать выезд | recordCheckOut() + BOOKING_CHECKED_OUT | PUT /bookings/{id}/check-out |
| Открыть окно резолюции | BookingCheckoutListener → openForBooking(); или POST /resolutions/open/{bookingId} | Событие + ResolutionServiceImpl.openForBooking |
| Уведомить владельца об открытии окна | notifyUser(RESOLUTION_WINDOW_OPENED) | В openForBooking() |
| Действие владельца: мат. компенсация | requestMoney() → MONEY_REQUESTED, notifyUser(RESOLUTION_MONEY_REQUESTED) | PUT /resolutions/{id}/request-money |
| Действие владельца: жалоба | recordComplaint() → COMPLAINT_RECORDED, notifyUser(RESOLUTION_COMPLAINT_RECORDED) | PUT /resolutions/{id}/complaint |
| Гость: оплатить | respondPay() → PAID, notifyUser(RESOLUTION_PAYMENT_RECEIVED) | PUT /resolutions/{id}/respond-pay |
| Гость: отказать | respondRefuse() → REFUSED, notifyUser(RESOLUTION_GUEST_REFUSED) | PUT /resolutions/{id}/respond-refuse |
| Попросить Airbnb вмешаться | escalate() → ESCALATED, notifyUser(RESOLUTION_ESCALATED) | PUT /resolutions/{id}/escalate |
| Существенное происшествие? | resolveEscalation(substantialIncident) | PUT /resolutions/{id}/resolve-escalation (ALL_RESOLUTION_UPDATE) |
| Обязательная выплата, уведомить гостя | resolveEscalation(true) → MANDATORY_PAYMENT, RESOLUTION_MANDATORY_PAYMENT_ASSIGNED | ResolutionServiceImpl.resolveEscalation() |
| Уведомить владельца об урегулировании (нет) | resolveEscalation(false) → CLOSED, RESOLUTION_RESOLVED_WITHOUT_PAYMENT | ResolutionServiceImpl.resolveEscalation() |
| Закрыть без эскалации (Gw_GuestRefused «Нет») | close() владельцем из REFUSED → RESOLUTION_WINDOW_CLOSED_BY_OWNER | PUT /resolutions/{id}/close (владелец при REFUSED) |
| Не отвечает 14 дней → закрыть окно → уведомить владельца | closeExpiredWindows() только для OPEN (openedAt старше 14 дней) — владелец не задействовал окно → RESOLUTION_WINDOW_CLOSED_AUTO | ResolutionScheduler, ResolutionServiceImpl.closeExpiredWindows |
| Закрыть окно вручную | close() → RESOLUTION_WINDOW_CLOSED или RESOLUTION_WINDOW_CLOSED_BY_OWNER (владелец из REFUSED) | PUT /resolutions/{id}/close |

Прочитав этот документ и проследив по разделам 3–5 и таблице 7, можно проверить соответствие кода требованиям BPMN и при необходимости дополнить реализацию по пунктам из раздела 6.
