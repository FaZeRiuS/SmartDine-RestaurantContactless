# SmartDine — Контакт-фрі сервіс для ресторанів

**SmartDine** — це сучасна платформа для автоматизації обслуговування в ресторанах, що дозволяє гостям самостійно переглядати меню, оформлювати замовлення та здійснювати оплату через смартфон за допомогою QR-кодів.

Проєкт розроблений як дипломна робота, що поєднує в собі потужний бекенд на Spring Boot та інтерактивний фронтенд з підтримкою PWA.

---

## Основні можливості

- **QR-інтеграція**: Кожен столик має унікальний QR-код для автоматичного визначення місця замовлення.
- **Інтерактивне меню**: Перегляд страв з категоріями, фотографіями та детальним описом.
- **Система замовлень**: Керування кошиком, вибір кількості та миттєве оформлення замовлення.
- **Онлайн-оплата**: Інтеграція з **LiqPay** для безпечних платежів (карта, Apple Pay, Google Pay).
- **Реальний час**: Оновлення статусу замовлення через **SSE** (Server-Sent Events, `EventSource`).
- **Адмін-панель**: Керування меню, категоріями, столиками та перегляд аналітики на дашборді.
- **Staff Interface**: Спеціальний інтерфейс для офіціантів та кухні для обробки замовлень.
- **PWA (Progressive Web App)**: Додаток можна встановити на головний екран смартфона.
- **Push-сповіщення**: Отримання повідомлень про готовність замовлення або нові виклики офіціанта.

---

## Технологічний стек

- **Backend**: Java 24, Spring Boot 3.4.4.
- **Security**: Keycloak (OAuth2, OpenID Connect) для авторизації клієнтів та персоналу.
- **Database**: PostgreSQL (Production) / H2 (Testing), Flyway для міграцій БД.
- **Frontend**: HTML5, CSS3 (Modern Vanilla), JavaScript (ES6+), Thymeleaf, HTMX для динамічного UI.
- **Real-time**: Spring MVC `SseEmitter` + browser `EventSource` (SSE).
- **Media**: Thumbnailator та WebP-imageio для оптимізації фото страв.
- **Testing**: Playwright, HtmlUnit, JUnit 5, Mockito.
- **DevOps**: Docker, Docker Compose, Nginx.
- **Payments**: LiqPay SDK.

---

## Структура коду (пакети)

- **Шари**: `controller` (веб), `service/<домен>` + `service/<домен>/impl`, `repository`, `model`, `mapper`, `config`, `exception`, `advice`.
- **Сервіси за доменами** (як `service/order`): `order` (замовлення, чайові, відгуки, лояльність по замовленню), `cart`, `menu` (меню, страви, рейтинги), `payment` (LiqPay, checkout, callback), `loyalty`, `dashboard`, `recommendation`, `push`, `sse`, `image` (у т.ч. міграція зображень), `qr`.
- **Контролери**: підпакети `controller.admin`, `controller.staff`, `controller.htmx` для групування за роллю та HTMX-фрагментами.
- **DTO**: підпакети `dto.order`, `dto.cart`, `dto.menu`, `dto.loyalty`, `dto.payment`, `dto.push` плюс існуючий `dto.dashboard`.
- **Інше**: `web.error` (REST-помилки), `bootstrap` (сідер даних, профіль `dev`), `i18n` (тексти сповіщень), `security` (ідентифікація користувача в запиті), `util` (допоміжні класи).

---

## Налаштування та запуск

### Вимоги
- Docker & Docker Compose
- JDK 24
- Maven 3.9+

### Локальний запуск (Development)

1. Клонуйте репозиторій.
2. Створіть файл `.env` на основі `.env.example`.
3. Запустіть необхідні сервіси (Database, Keycloak):
   ```bash
   docker-compose up -d postgres keycloak
   ```
4. Запустіть додаток:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

### Запуск через Docker (Full Stack)
```bash
docker-compose up --build
```

---

## Інтеграція з LiqPay

Для роботи платежів у режимі Sandbox:
1. Запустіть **ngrok** для отримання публічного URL: `ngrok http 8081`.
2. Оновіть змінну `APP_PUBLIC_BASE_URL` у файлі `.env` значенням від ngrok.
3. Вкажіть ваші ключі `LIQPAY_PUBLIC_KEY` та `LIQPAY_PRIVATE_KEY` у файлі `.env`.
4. Вебхуки будуть надходити на `/api/payment/callback`.

---

## Безпека та Розгортання (Production)

### Checklist:
1. **Секрети**: Тільки через змінні оточення (`SPRING_DATASOURCE_PASSWORD`, `KEYCLOAK_CLIENT_SECRET` тощо).
2. **CORS**: Вкажіть дозволені домени в `app.cors.allowed-origins` (змінна `APP_ALLOWED_ORIGINS`) замість `*`, якщо фронт або API з іншого origin.
3. **HTTPS**: Використовуйте зворотний проксі (Nginx/Traefik) для термінації SSL.
4. **Профілі**: Використовуйте `application.properties` з надійними дефолтами для продакшну.

### Nginx (host-based) + Docker Compose (prod)

У продакшні Nginx працює **на хості** (поза Docker) і проксіює запити у контейнери, які опубліковані тільки на `127.0.0.1`:
- `app` → `127.0.0.1:8081`
- `keycloak` → `127.0.0.1:8080` (з `KC_HTTP_RELATIVE_PATH=/auth`)

Приклад конфігурації лежить у `deploy/nginx/smart-dine.conf.example`.

Коротко:
1. Скопіюйте приклад та замініть `<DOMAIN>`:
   ```bash
   sudo cp deploy/nginx/smart-dine.conf.example /etc/nginx/sites-available/smart-dine.conf
   sudo sed -i 's/<DOMAIN>/your-domain.example/g' /etc/nginx/sites-available/smart-dine.conf
   sudo ln -sf /etc/nginx/sites-available/smart-dine.conf /etc/nginx/sites-enabled/smart-dine.conf
   ```
2. Підніміть сервіси:
   ```bash
   docker compose -f docker-compose.prod.yml up -d --build
   docker compose -f docker-compose.keycloak.yml up -d
   ```
3. Налаштуйте TLS (наприклад, Certbot) і перезавантажте Nginx:
   ```bash
   sudo nginx -t && sudo systemctl reload nginx
   ```

---

## Ліцензія

Цей проєкт створений у рамках навчальної дипломної роботи. Усі права належать автору.
