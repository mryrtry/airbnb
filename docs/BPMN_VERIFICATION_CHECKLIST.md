# Критическая сверка BPMN: как попасть в каждый пункт, несоответствия

Документ для пошаговой проверки: каждый элемент диаграммы `airbnb.bpmn`, как в него попасть (условия/API), реализация в коде, замечания при расхождении.

---

## 1. Дорожка «Владелец»: публикация объявления

| BPMN id | Название | Как попасть | Реализация | Статус |
|--------|----------|-------------|------------|--------|
| Event_0xrs54c | Опубликовать объявление о сдаче жилья (Start) | Точка входа процесса | — | OK |
| Activity_1lmw3cm | Опубликовать объявление о сдаче жилья | Владелец создаёт листинг и переводит в PUBLISHED | `POST /listings` → `PUT /listings/{id}` с `{"status": "PUBLISHED"}` | OK |
| Activity_1r7wjdn | Уведомить владельца | После смены статуса на PUBLISHED | `ListingServiceImpl.update()` → `notifyUser(ownerId, LISTING_PUBLISHED, ...)` | OK |
| Activity_0qi5hke | Получить уведомление | Владелец смотрит уведомления | `GET /notifications` | OK |
| Event_1lsw8is | End | Конец подпроцесса публикации | — | OK |

---

## 2. Дорожка «Гость» + «Система»: поиск и заявка

| BPMN id | Название | Как попасть | Реализация | Статус |
|--------|----------|-------------|------------|--------|
| Event_18m7ui0 | Найти жилье (Start) | Точка входа | — | OK |
| Activity_0z6omph | Запросить список доступного жилья | Гость вызывает API списка | `GET /listings?status=PUBLISHED` (клиент обязан передать status=PUBLISHED) | OK |
| Activity_0txalis | Отдать список доступного жилья | Ответ на запрос списка | `ListingController.getAll()`, фильтр по status | OK |
| Activity_0jlb1y7 | Получить список доступного жилья | Клиент получает ответ | Ответ `GET /listings` | OK |
| Activity_1qvf11h | Выбрать жилье | Пользовательский выбор (вне API) | Нет отдельного шага; выбор = listingId в следующем запросе | OK |
| Activity_1v216a5 | Подать заявку | Гость отправляет заявку | `POST /bookings` с `{"listingId": <id>}` | OK |
| Activity_0jr0t65 | Зарегистрировать заявку | Система создаёт бронирование | `BookingServiceImpl.create()` | OK |
| Activity_0mbm8ir | Уведомить владельца | После регистрации заявки | `notifyUser(ownerId, BOOKING_APPLIED, ...)` в create() | OK |

---

## 3. Решение владельца по заявке

| BPMN id | Название | Как попасть | Реализация | Статус |
|--------|----------|-------------|------------|--------|
| Activity_06px8tj | Получить уведомление | После BOOKING_APPLIED владелец смотрит уведомления | `GET /notifications` | OK |
| Activity_1517h2c | Принять решение по заявке | Владелец решает одобрить/отклонить | `PUT /bookings/{id}/decide` с `{"status": "APPROVED"}` или `"REJECTED"` | OK |
| Gateway_1gugo4m | Одобрить? | Исход решения | Один вызов decide(); ветвление по значению status | OK |
| Flow_1ejtifz (нет) | → Уведомить об отказе | status=REJECTED | `notifyUser(guestId, BOOKING_REJECTED, ...)` в decide() | OK |
| Activity_15l0dit | Уведомить пользователя об отказе | — | В decide(REJECTED) | OK |
| Activity_0fgtvuy | Получить уведомление (гость) | Гость смотрит уведомления | `GET /notifications` | OK |
| Event_0m2x6w8 | Конец | После отказа процесс для этой заявки завершён | — | OK |
| Flow_13p9a50 (да) | → Уведомить о возможности въезда | status=APPROVED | `notifyUser(guestId, BOOKING_APPROVED, ...)` в decide() | OK |
| Activity_1n65xzp | Уведомить пользователя о возможности въезда | — | В decide(APPROVED) | OK |
| Activity_1yn1qo3 | Получить уведомление (гость) | Гость смотрит уведомления | `GET /notifications` | OK |

---

## 4. Въезд и выезд гостя

| BPMN id | Название | Как попасть | Реализация | Статус |
|--------|----------|-------------|------------|--------|
| Activity_1dkj385 | Въехать | Гость после одобрения фиксирует въезд | `PUT /bookings/{id}/check-in` | OK |
| Activity_1dhrvk2 | Зарегистрировать въезд | Система переводит в CHECKED_IN | `BookingServiceImpl.recordCheckIn()` | OK |
| Activity_0tq7ms3 | Уведомить владельца о въезде | После check-in | `notifyUser(ownerId, BOOKING_CHECKED_IN, ...)` | OK |
| Activity_0rk5iq2 | Получить уведомление (владелец) | Владелец смотрит уведомления | `GET /notifications` | OK |
| Activity_1lms11j | Выехать | Гость фиксирует выезд | `PUT /bookings/{id}/check-out` | OK |
| Activity_1xotiem | Зарегистрировать выезд | Система переводит в CHECKED_OUT | `BookingServiceImpl.recordCheckOut()` | OK |
| Activity_1wrvbq1 | Уведомить владельца о выезде | После check-out | `notifyUser(ownerId, BOOKING_CHECKED_OUT, ...)` | OK |
| Activity_0gt6036 | Получить уведомление (владелец) | Владелец смотрит уведомления | `GET /notifications` | OK |
| Flow_00tgyi1 | → Открыть окно резолюции | Сразу после выезда | `BookingCheckoutListener` → `openForBooking(bookingId)` | OK |

**Замечание:** В BPMN задачи «Въехать» и «Выехать» в дорожке Гость — инициирует гость. В коде для check-in/check-out используется `requireCanCheckInCheckOut`: выполнять могут только **гость этого бронирования** или админ (ALL_BOOKING_UPDATE). Владелец листинга без админских прав не может выполнять check-in/check-out — соответствие BPMN.

---

## 5. Открытие окна разрешения споров и выбор владельца

| BPMN id | Название | Как попасть | Реализация | Статус |
|--------|----------|-------------|------------|--------|
| Sys_OpenResolutionWindow | Открыть окно разрешения споров для владельца | После recordCheckOut (или вручную) | `ResolutionServiceImpl.openForBooking()` (из listener или `POST /resolutions/open/{bookingId}`) | OK |
| Activity_0llbpfa | Уведомить Владельца об открытии окна | Сразу после открытия окна | `notifyUser(ownerId, RESOLUTION_WINDOW_OPENED, ...)` в openForBooking() | OK |
| Activity_0zsseu5 | Получить уведомление (владелец) | Владелец смотрит уведомления | `GET /notifications` | OK |
| Gw_OwnerChoice | Действие владельца | Владелец выбирает: компенсация или жалоба | Два разных API: request-money или complaint | OK |

---

## 6. Ветка «Мат. компенсация»

| BPMN id | Название | Как попасть | Реализация | Статус |
|--------|----------|-------------|------------|--------|
| Flow_ChoiceToRequestMoney | Мат компенсация | Выбор владельца | Владелец вызывает request-money | OK |
| Owner_RequestMoney | Запросить мат компенсацию | Из Gw_OwnerChoice | `PUT /resolutions/{id}/request-money` с `{"amountRequested": ...}` | OK |
| Sys_SendMoneyRequest | Отправить запрос на оплату гостю | После request-money | В requestMoney() устанавливается MONEY_REQUESTED и отправляется уведомление | OK |
| Sys_NotifyGuest | Уведомить гостя о запросе | — | `notifyUser(guestId, RESOLUTION_MONEY_REQUESTED, ...)` | OK |
| Guest_ReceiveRequest | Получить запрос на оплату | Гость смотрит уведомления | `GET /notifications`; в теле есть relatedResolutionId | OK |
| Guest_RespondPayOrRefuse | Ответить: оплатить или отказать | Гость отвечает | `PUT /resolutions/{id}/respond-pay` или `respond-refuse` | OK |
| Gw_GuestDecision | Оплатил / Отказал? | Исход ответа гостя | Два разных эндпоинта | OK |

---

## 7. Ветка «Оплатил» (добровольная)

| BPMN id | Название | Как попасть | Реализация | Статус |
|--------|----------|-------------|------------|--------|
| Flow_GuestPaid | Оплатил | Гость выбрал оплатить | `PUT /resolutions/{id}/respond-pay` | OK |
| Sys_ReceivePayment | Принять оплату от гостя | — | В respondPay() окно переводится в PAID, closedAt | OK |
| Activity_03b40fh | Уведомить владельца о зафиксированной оплате | — | `notifyUser(ownerId, RESOLUTION_PAYMENT_RECEIVED, ...)` | OK |
| Activity_0ynw3fz | Получить уведомление (владелец) | Владелец смотрит уведомления | `GET /notifications` | OK |
| Event_0ka61nm | Конфликт урегулирован | Конец ветки | Статус PAID, окно закрыто | OK |

---

## 8. Ветка «Отказал»: эскалация и «Есть смысл продолжать?»

| BPMN id | Название | Как попасть | Реализация | Статус |
|--------|----------|-------------|------------|--------|
| Flow_GuestRefused | Отказал | Гость выбрал отказать | `PUT /resolutions/{id}/respond-refuse` | OK |
| Gw_GuestRefused | Есть смысл продолжать? | Решение владельца после отказа | Владелец вызывает escalate или close | OK |
| Flow_1lkc774 (Нет) | → Конфликт урегулирован (Event_0jpe54e) | Владелец решил не продолжать | `PUT /resolutions/{id}/close` из статуса REFUSED (владелец); уведомление RESOLUTION_WINDOW_CLOSED_BY_OWNER | OK |
| Flow_RefusedToOwnerEscalate (Да) | → Попросить Airbnb вмешаться | Владелец решил эскалировать | `PUT /resolutions/{id}/escalate` | OK |
| Owner_EscalateAfterRefusal | Попросить Airbnb вмешаться | — | escalate() | OK |
| Sys_HandleEscalation | Обработать запрос вмешательства | После эскалации | В коде: переход в ESCALATED + уведомление гостю; «обработка» = вызов админом resolve-escalation | OK |
| Gateway_0guf1pf | Существенное происшествие? | После Sys_HandleEscalation (в BPMN — система; в коде — решение админа) | `PUT /resolutions/{id}/resolve-escalation` с `{"substantialIncident": true/false}` | OK |

---

## 9. Ветка «Существенное происшествие?» → Да (обязательная выплата)

| BPMN id | Название | Как попасть | Реализация | Статус |
|--------|----------|-------------|------------|--------|
| Flow_1a6th2p (Да) | → Назначить обязательную выплату | substantialIncident: true | resolveEscalation(id, {substantialIncident: true}) | OK |
| Activity_0mozg5p | Назначить Гостю обязательную выплату | — | Статус MANDATORY_PAYMENT в resolveEscalation() | OK |
| Activity_1t1yyy6 | Уведомить Гостя об обязательной выплате | — | `notifyUser(guestId, RESOLUTION_MANDATORY_PAYMENT_ASSIGNED, ...)` | OK |
| Activity_0qbvh10 | Получить запрос на оплату (гость) | Гость смотрит уведомления | `GET /notifications` | OK |
| Activity_18h6wxc | Оплатить | Гость выполняет обязательную выплату | `PUT /resolutions/{id}/respond-pay` (допускается из MANDATORY_PAYMENT) | OK |
| Activity_0ax6kpu | Зарегистрировать выплату | — | В respondPay() при MANDATORY_PAYMENT | OK |
| Activity_049zrpt | Уведомить владельца о выплате пользователе | — | `notifyUser(ownerId, RESOLUTION_PAYMENT_RECEIVED, ...)` | OK |
| Activity_1l1dnrs | Получить уведомление (владелец) | Владелец смотрит уведомления | `GET /notifications` | OK |
| Event_0xhmdy1 | Конфликт урегулирован | Конец ветки | Статус PAID | OK |

---

## 10. Ветка «Существенное происшествие?» → Нет

| BPMN id | Название | Как попасть | Реализация | Статус |
|--------|----------|-------------|------------|--------|
| Flow_1olgfbz (Нет) | → Уведомить владельца об урегулировании | substantialIncident: false | resolveEscalation(id, {substantialIncident: false}) | OK |
| Activity_1w8qgok | Уведомить владельца об урегулировании конфликта | — | `notifyUser(ownerId, RESOLUTION_RESOLVED_WITHOUT_PAYMENT, ...)` | OK |
| Activity_01lvfbz | Получить уведомление (владелец) | Владелец смотрит уведомления | `GET /notifications` | OK |
| Event_1u5h19h | Конфликт урегулирован | Конец ветки | Окно закрыто (CLOSED) | OK |

---

## 11. Ветка «Жалоба»

| BPMN id | Название | Как попасть | Реализация | Статус |
|--------|----------|-------------|------------|--------|
| Flow_ChoiceToReportIssue | Жалоба | Выбор владельца из Gw_OwnerChoice | Владелец вызывает complaint | OK |
| Owner_AskAirbnbToIntervene | Подать жалобу на Гостя | — | `PUT /resolutions/{id}/complaint` с `{"description": "..."}` | OK |
| Sys_RecordIssue | Зафиксировать жалобу | — | recordComplaint() → COMPLAINT_RECORDED, closedAt | OK |
| Activity_00i2mqr | Уведомить владельца о зафиксированной жалобе | — | `notifyUser(ownerId, RESOLUTION_COMPLAINT_RECORDED, ...)` | OK |
| Activity_0dhvmlc | Получить уведомление (владелец) | Владелец смотрит уведомления | `GET /notifications` | OK |
| Event_1nihhyy | Конфликт урегулирован | Конец ветки | Статус COMPLAINT_RECORDED, окно закрыто | OK |

---

## 12. Таймер «Не отвечает 14 дней»

| BPMN id | Название | Как попасть | Реализация | Статус |
|--------|----------|-------------|------------|--------|
| Event_027b8xy | Не отвечает 14 дней | Промежуточное событие по времени после открытия окна | В BPMN таймер срабатывает, если «не отвечают» 14 дней. В коде трактуется как: владелец не задействовал окно (не запросил деньги, не подал жалобу) | OK |
| Activity_1kv4q0h | Закрыть окно разрешения споров | По срабатыванию таймера | `closeExpiredWindows()` закрывает только окна в статусе **OPEN** и openedAt старше 14 дней | OK |
| Activity_0nrufzo | Уведомить владельца о закрытие окна | — | `notifyUser(ownerId, RESOLUTION_WINDOW_CLOSED_AUTO, ...)` | OK |
| Activity_1o84a61 | Получить уведомление (владелец) | Владелец смотрит уведомления | `GET /notifications` | OK |
| Event_0fqgy57 | Конец | — | Окно в статусе CLOSED | OK |

**Важно:** Таймер в коде применяется только к окнам в статусе OPEN (владелец «не открыл» окно — не сделал ни запрос денег, ни жалобу). Окна в MONEY_REQUESTED или REFUSED по истечении 14 дней не закрываются автоматически — по требованию заказчика.

---

## 13. Сводка несоответствий и замечаний

| Тип | Описание |
|-----|----------|
| Соответствие | Check-in и check-out в коде разрешены только гостю этого бронирования или админу (`requireCanCheckInCheckOut`). Владелец листинга не может их выполнять — соответствие BPMN (инициирует гость). |
| Роль «система» vs админ | Шлюз «Существенное происшествие?» в BPMN — системное решение; в коде решение принимает админ через `resolve-escalation`. Это осознанная реализация (нужен ответственный субъект). |
| Таймер | В BPMN не задано явно, от какого момента считать 14 дней и по каким статусам закрывать. В коде: только OPEN, по `openedAt` — «владелец не задействовал окно». Соответствует уточнённому требованию. |

**Итог:** Все элементы BPMN имеют реализацию и достижимы. Явных несоответствий нет. Check-in/check-out выполняют только гость бронирования или админ (соответствие BPMN). Решение «Существенное происшествие?» принимает админ через resolve-escalation.
