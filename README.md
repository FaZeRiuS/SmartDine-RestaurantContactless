# SmartDine — Contactless Service for Restaurants

**SmartDine** is a modern platform for restaurant service automation, allowing guests to browse the menu, place orders, and make payments independently using their smartphones via QR codes.

The project is developed as a thesis, combining a powerful Spring Boot backend and an interactive frontend with PWA support.

---

## Key Features

- **QR Integration**: Each table has a unique QR code for automatic location detection of the order.
- **Interactive Menu**: Browse dishes with categories, photos, and detailed descriptions.
- **Smart Recommendations**: Cross-sell algorithms ("frequently bought together") and personalized recommendations based on user history.
- **Order System**: Basket management, quantity selection, and instant order placement.
- **Loyalty & Reviews**: Accumulate bonus points, pay with bonus points, leave tips, and write reviews for dishes and service.
- **Online Payments**: Integration with **LiqPay** for secure payments (Card, Apple Pay, Google Pay).
- **Real-time Updates**: Order status updates via **SSE** (Server-Sent Events, `EventSource`).
- **Admin Panel**: Manage menus, categories, tables, and view dashboard analytics.
- **Staff Interface**: A dedicated interface for waiters and kitchen staff to process orders.
- **PWA & Web-Push**: Install the app on a smartphone and receive push notifications when orders are ready or when called.

---

## Technology Stack

- **Backend**: Java 25, Spring Boot 3.4.
- **Security**: Keycloak (OAuth2, OpenID Connect) for customer and staff authorization.
- **Database**: PostgreSQL (Production) / H2 (Testing), Flyway for DB migrations.
- **Frontend**: HTML5, CSS3 (Modern Vanilla), JavaScript (ES6+), Thymeleaf, HTMX for dynamic UI.
- **Real-time**: Spring MVC `SseEmitter` + browser `EventSource` (SSE).
- **Media**: Thumbnailator and WebP-imageio for optimizing food images.
- **Testing**: Playwright, HtmlUnit, JUnit 5, Mockito.
- **DevOps**: Docker, Docker Compose, Nginx.
- **Payments**: LiqPay API.

---

## Code Structure (Packages)

- **Layers**: `controller` (web), `service/<domain>` + `service/<domain>/impl`, `repository`, `model`, `mapper`, `config`, `exception`, `advice`.
- **Services by Domain** (like `service/order`): `order` (orders, tips, reviews, loyalty on orders), `cart`, `menu` (menu, dishes, ratings), `payment` (LiqPay, checkout, callback), `loyalty`, `dashboard`, `recommendation`, `push`, `sse`, `image` (including image migration), `qr`.
- **Controllers**: subpackages `controller.admin`, `controller.staff`, `controller.htmx` for grouping by role and HTMX fragments.
- **DTO**: subpackages `dto.order`, `dto.cart`, `dto.menu`, `dto.loyalty`, `dto.payment`, `dto.push` plus the existing `dto.dashboard`.
- **Other**: `web.error` (REST errors), `bootstrap` (data seeder, `dev` profile), `i18n` (notification texts), `security` (user identification in request), `util` (utility classes).

---

## Setup and Running

### Requirements
- Docker & Docker Compose
- JDK 25
- Maven 3.9+

### Local Run (Development)

1. Clone the repository.
2. Create a `.env` file based on `.env.example`.
3. Start the required services (Database, Keycloak):
   ```bash
   docker-compose up -d postgres keycloak
   ```
4. Run the application:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

### Running with Docker (Full Stack)
```bash
docker-compose up --build
```

---

## LiqPay Integration

To make payments work in Sandbox mode:
1. Run **ngrok** to get a public URL: `ngrok http 8081`.
2. Update the `APP_PUBLIC_BASE_URL` variable in your `.env` file with the ngrok URL.
3. Specify your `LIQPAY_PUBLIC_KEY` and `LIQPAY_PRIVATE_KEY` in the `.env` file.
4. Webhooks will be received at `/api/payment/callback`.

---

## Security & Deployment (Production)

### Checklist:
1. **Secrets**: Use environment variables only (`SPRING_DATASOURCE_PASSWORD`, `KEYCLOAK_CLIENT_SECRET`, etc.).
2. **CORS**: Specify allowed domains in `app.cors.allowed-origins` (variable `APP_ALLOWED_ORIGINS`) instead of `*` if the front or API is hosted on a different origin.
3. **HTTPS**: Use a reverse proxy (Nginx/Traefik) for SSL termination.
4. **Profiles**: Use `application.properties` with secure defaults for production.

### Nginx (host-based) + Docker Compose (prod)

In production, Nginx runs **on the host** (outside Docker) and proxies requests to containers that are published only on `127.0.0.1`:
- `app` → `127.0.0.1:8081`
- `keycloak` → `127.0.0.1:8080` (with `KC_HTTP_RELATIVE_PATH=/auth`)

An example configuration is located in `deploy/nginx/smart-dine.conf.example`.

In brief:
1. Copy the example and replace `<DOMAIN>`:
   ```bash
   sudo cp deploy/nginx/smart-dine.conf.example /etc/nginx/sites-available/smart-dine.conf
   sudo sed -i 's/<DOMAIN>/your-domain.example/g' /etc/nginx/sites-available/smart-dine.conf
   sudo ln -sf /etc/nginx/sites-available/smart-dine.conf /etc/nginx/sites-enabled/smart-dine.conf
   ```
2. Spin up the services:
   ```bash
   docker compose -f docker-compose.prod.yml up -d --build
   docker compose -f docker-compose.keycloak.yml up -d
   ```
3. Set up TLS (e.g., Certbot) and reload Nginx:
   ```bash
   sudo nginx -t && sudo systemctl reload nginx
   ```

---

## License

This project is created as part of an academic thesis. All rights reserved by the author.
