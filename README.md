# UniSchool by Unicard

One app for everything school. Parents, learners, teachers, the principal, the school office
and the tuckshop each sign in once and see only what their verified role allows. Someone
with more than one role (for example a teacher who is also a parent) switches between roles
in the same account.

This repository holds the **backend REST API** (Java 21, Spring Boot 3.5), the **web app**
(`web/`) and the **shared TypeScript API package** (`packages/api/`). The design follows the
UniSchool prototype (`UniSchool by Unicard.html`).

## Tech stack

| Part | Choice |
|------|--------|
| Backend | Java 21, Spring Boot 3.5, Maven; Spring Web, Data JPA, Validation, Spring Security with JWT sign-in |
| API contract | OpenAPI via springdoc; generates the TypeScript client so the front ends always match the Java code |
| Database | PostgreSQL 16 in staging and production, H2 for local development, Flyway for every schema change |
| Web app | React, TypeScript, Vite, TanStack Query, React Router, Tailwind CSS |
| Mobile apps (Android, iOS) | React Native with Expo in TypeScript, Expo Router, TanStack Query; expo-camera (QR), react-native-nfc-manager (card taps), expo-notifications (push), expo-secure-store (tokens), expo-local-authentication (fingerprint/face) |
| Shared front-end package | One TypeScript package with the API client, data types and business rules, used by web and mobile |
| Testing | JUnit and Testcontainers (backend), Vitest and Playwright (web), Jest (mobile) |
| CI and releases | GitHub Actions for builds and tests, Expo EAS for app-store releases |
| Hosting | Docker containers on AWS Cape Town or Azure Johannesburg, managed PostgreSQL with automated backups |

## Run it

### Backend

**IntelliJ:** open the project, let Maven import dependencies, run `co.za.unicard.UniSchool`.

**Command line** (the Maven wrapper downloads Maven; you only need JDK 21):
```bash
./mvnw spring-boot:run   # starts on http://localhost:8081
./mvnw test              # runs the API and unit tests
```

Open `http/unischool.http` in IntelliJ to fire ready-made requests.
The API docs (Swagger UI) are at http://localhost:8081/swagger-ui.html (dev and staging only).
The H2 database console is at http://localhost:8081/h2-console (JDBC URL `jdbc:h2:file:./data/unischool;AUTO_SERVER=TRUE`, user `sa`, no password).
Dev data is saved in `data/unischool.mv.db` and survives restarts. To start again from the demo school, stop the app and delete the `data` folder.

### Web app

Needs Node.js 22 or newer. Start the backend first, then from the repository root:
```bash
npm install              # once; installs the web app and the shared API package
npm run dev              # http://localhost:5174 (Vite forwards /api to the backend on :8081)
```
Sign in with one of the demo accounts listed under **Demo accounts** below (local dev only).

```bash
npm test                         # unit tests (shared package and web app, Vitest)
npm run e2e --workspace web      # browser tests (Playwright); needs the backend running
npm run build                    # production build in web/dist
```

### On your phone

The web app works like an app on a phone: a bottom tab bar, everything else under **More**, and
it can be added to the home screen. With the backend running, `npm run dev` prints two addresses:
```
  ➜  Local:   http://localhost:5174/
  ➜  Network: http://192.168.1.102:5174/
```
Under them it prints a QR code of the Network address: point the phone's camera at it and tap the
link, or type the address in. The phone must be on the same Wi-Fi (allow Node.js through the Windows firewall if asked).
To put it on the home screen: Safari **Share → Add to Home Screen** on iPhone, Chrome **⋮ → Add to
Home screen** on Android. A full Android install (own window, app drawer) needs HTTPS, which you get
once it's deployed.

### Sharing it with someone outside your Wi-Fi (Tailscale)

[Tailscale](https://tailscale.com) gives this computer a private HTTPS address that only people
you invite can open, from anywhere. They always see your latest changes, as long as this computer,
the backend and `npm run dev` (fixed on port 5174) are running.

- One-time on this computer: install Tailscale, sign in, enable **HTTPS Certificates** at
  https://login.tailscale.com/admin/dns, then run `tailscale serve --bg 5174`. It survives restarts.
- To invite someone: https://login.tailscale.com/admin/machines → this computer → **⋯ → Share** →
  their email. They install the Tailscale app, sign in with that email and accept.
- They open `https://<computer>.<tailnet>.ts.net` (shown by `tailscale serve status`) and can add
  it to their home screen. Stop sharing with `tailscale serve --https=443 off`.

### Keeping the front ends in step with the Java code

The TypeScript types in `packages/api/src/schema.d.ts` are generated from the backend's OpenAPI spec.
After changing a controller or DTO, with the backend running:
```bash
npm run api:fetch        # saves packages/api/openapi.json from http://localhost:8081
npm run api:generate     # regenerates the types
```
Then `npm run build` shows every screen that no longer matches the API.

## Environments and database

Configuration is in YAML under `src/main/resources`. `application.yml` holds the shared settings,
and each profile file adds its own:

| Profile | Database | Schema | Demo data | Use |
|---------|----------|--------|-----------|-----|
| `dev` (default) | H2 file in `./data` | Flyway | Yes, once into an empty database | Your machine; kept between restarts |
| `staging` | PostgreSQL | Flyway migrations, Hibernate `validate` | No (opt in with `UNISCHOOL_SEED_DEMO=true` on a throwaway DB) | Testing with others |
| `prod` | PostgreSQL (managed, with backups) | Flyway migrations, Hibernate `validate` | Never | Live |

Choose a profile with `SPRING_PROFILES_ACTIVE=staging` (or `prod`). Staging and prod read their
secrets from environment variables, so no passwords live in the repo:

| Variable | Example |
|----------|---------|
| `DB_URL` | `jdbc:postgresql://db-host:5432/unischool?sslmode=require` |
| `DB_USERNAME`, `DB_PASSWORD` | from your cloud's secrets manager |
| `UNISCHOOL_JWT_SECRET` | 32+ random characters (required outside dev) |
| `UNISCHOOL_ADMIN_USERNAME`, `UNISCHOOL_ADMIN_PASSWORD`, `UNISCHOOL_ADMIN_NAME` | the first school office account (password 12+ characters) |
| `UNISCHOOL_SEED_DEMO` | `false` by default; `true` only on a throwaway staging database |

**Starting a real school (staging and prod).** The database starts empty; the demo school is
only for local testing. On the first start, `FirstAdminBootstrap` creates one school office account
from the `UNISCHOOL_ADMIN_*` variables (only while no office or principal account exists). From
then on everything is created by using the app:

1. The office signs in and sets the school up: enrols learners (**Learners**), sets next year's fees
   per grade (**Fees**), the uniform shop and sport kit, stationery lists and parent groups.
2. Staff, parents and learners ask for access in the app; the office approves them (and says
   which grade a teacher teaches). Learners named in a request who aren't enrolled yet are created
   on approval.
3. The tuckshop manages its menu (**Menu**). Learners sign up themselves, or a parent creates their sign-in.

`EmptySchoolJourneyTest` walks this whole journey on an empty database.

**Schema changes go through Flyway.** The tables are defined in
`src/main/resources/db/migration/V1__initial_schema.sql`. To change the schema, change the entity
*and* add a new file, `V2__what_changed.sql`. Never edit a migration that has already run on staging
or prod. On startup Flyway applies any new migrations, then Hibernate validates the entities
against the result and refuses to start if they don't match.

**Try PostgreSQL locally:** `docker compose up -d` starts PostgreSQL 16 (see `compose.yaml`), then
run the app with the `staging` profile and the variables listed in that file.

**Database tests:**
- `MigrationOnH2Test` builds the schema from the Flyway scripts on H2 (PostgreSQL mode) and runs
  the main flows. No Docker needed.
- `PostgresIntegrationTest` does the same on a real PostgreSQL 16 in Docker (Testcontainers), with
  Hibernate `validate`, exactly like staging and prod. It's skipped automatically when Docker isn't running.

**Wallet race protection:** accounts, learners, savings pots and pre-loved listings carry a
`version` column (optimistic locking). If two requests change the same wallet at the same moment,
the second gets `409 Conflict` ("Something changed at the same moment. Please try again.") instead
of silently overwriting the first.

## Demo accounts

Loaded from the prototype on startup (`unischool.seed-demo=true`). Password for all: **`unischool`**.

| Username   | Person            | Roles |
|------------|-------------------|-------|
| `naledi`   | Naledi Mokoena    | Parent of Lerato (Grade 4, Prep) and Thabo (Grade 8, College) |
| `naidoo`   | Priya Naidoo      | Teacher (Grade 4, Prep) **and** parent of Kayla (Grade 9, College) |
| `lerato`   | Lerato Mokoena    | Learner |
| `khumalo`  | Thandeka Khumalo  | Principal **and** school office |
| `vanwyk`   | Johan van Wyk     | School office (bursar) |
| `sithole`  | Grace Sithole     | Tuckshop |
| `dlamini`, `botha` | College teachers | Teacher |
| `sibongile`, `kabelo` | Waiting for school verification | Can't sign in until approved |

## Security

1. `POST /api/auth/login` with `{"username","password"}` returns a **JWT** (HS256, 8 hours) for the
   account's first verified role.
2. Send it on every request: `Authorization: Bearer <token>`.
3. `POST /api/auth/switch-role` with `{"role":"PARENT"}` returns a new token for another verified role.
4. Endpoints are protected with `@PreAuthorize("hasRole('...')")`, and services also check
   ownership (a parent can only act for their own children).

Sign-up (`POST /api/auth/register`) is open to every role, including learners, and creates an access
request. Parents describe their children and learners describe themselves (the student number is
optional). The school office approves or declines it, and the person can sign in only after approval.
On approval, learners already in UniSchool are linked and the others are added to the enrolment record.

Set `UNISCHOOL_JWT_SECRET` (32+ characters) in any real environment.

## What's implemented

| Area | Who | Highlights |
|------|-----|-----------|
| Wallets | Parent | Unicard wallet, top-up, move money to a child, combined activity |
| Card controls | Parent | Daily limit, freeze card, tuckshop mode (pre-orders only / approved items / free choice), blocked categories, approved items |
| Tuckshop | Parent, learner | Menu, plan the week (next 5 school days), change or cancel with automatic refund, repeat weekly (nightly job) |
| Till (POS) | Tuckshop | Search or scan card, charge with all parent rules enforced (declines charge nothing), hand over pre-orders in parts, today's summary |
| Shop | Parent | Catalogue tailored per child (sizes, sports, campus), supply list with "already have" items and 5% full-list discount, pre-loved marketplace (fit check, sold once), name labels, cart, checkout (wallet, savings pot, card, Pay in 4; home delivery R65 under R750), 5% give-back to the school |
| School payments | Parent | Trips/levies in instalments, event tickets with codes, donations |
| Fees | Parent | Next year's fee plan (annual -5%, termly, 10 or 12 months, financed) with instalment amounts |
| Savings | Parent | Back-to-school pot with projection to January, monthly debit, family share link (`/api/give/{token}`, no sign-in), weekly pocket money (daily job) |
| Learner app | Learner | Card QR token, wallet, savings goal (save R10), homework, school headlines |
| Notices | Parent, staff | Audience by grade or team, read receipts, acknowledgement per child, RSVP, linked payments |
| Homework | Teacher, parent, learner | Post homework, mark done, class completion stats |
| Approvals | Principal | Teachers' notices (and optionally homework) wait for approval; approve, edit and approve, or send back with a comment |
| Parent groups | Parent, teacher | Grade and team groups, quiet hours (21:00-06:00 delivered at 06:00), announce-only mode, mute, report |
| School office | School office | Overview, collections (create, remind, CSV export), access requests, staff list, group moderation, fee-plan uptake, supply lists per grade, uniform stock and restock, payouts |
| Principal | Principal | School at a glance, "needs your attention", tuckshop today, homework and notice reach |

## API overview

| Area | Endpoints |
|------|-----------|
| Auth | `POST /api/auth/login`, `POST /api/auth/register`, `GET /api/auth/me`, `POST /api/auth/switch-role` |
| Parent home | `GET /api/home` |
| Learners | `GET /api/learners`, `GET /api/learners/{id}`, `PATCH /api/learners/{id}/controls`, `GET /api/learners/{id}/activity` |
| Learner app | `GET /api/me/card`, `POST /api/me/goal/save`, `GET /api/me/headlines` |
| Wallet | `GET /api/wallet`, `POST /api/wallet/top-up`, `POST /api/wallet/move/{learnerId}`, `GET /api/wallet/activity` |
| Tuckshop | `GET /api/tuckshop/menu`, `GET /api/tuckshop/week`, `PUT /api/tuckshop/learners/{id}/orders`, `DELETE /api/tuckshop/learners/{id}/orders/{day}` |
| Till | `GET /api/pos/today`, `GET /api/pos/learners?q=`, `GET /api/pos/cards/{token}`, `POST /api/pos/charge`, `POST /api/pos/pre-orders/{id}/handout` |
| Shop | `GET /api/shop/catalog`, `GET /api/shop/supply-list`, `GET/POST /api/shop/cart`, `POST /api/shop/cart/supply-list`, `DELETE /api/shop/cart/{id}`, `POST /api/shop/checkout`, `GET /api/shop/orders`, `POST/DELETE /api/shop/preloved` |
| Payments | `GET /api/payments/collections`, `POST /api/payments/collections/{id}/pay`, `GET /api/payments/tickets`, `GET /api/fees`, `PUT /api/fees/{learnerId}/plan` |
| Savings | `GET /api/savings`, `POST /api/savings/add`, `PUT /api/savings/monthly`, `GET/POST /api/pocket-money`, `PATCH /api/pocket-money/{id}`, `GET/POST /api/give/{token}` |
| Notices | `GET /api/notices`, `POST /api/notices/{id}/read`, `POST /api/notices/read-all`, `POST /api/notices/{id}/acknowledge`, `POST /api/notices/{id}/rsvp` |
| Homework | `GET /api/homework`, `PUT /api/homework/{id}/done` |
| Groups | `GET /api/groups`, `GET/POST /api/groups/{id}/messages`, `POST /api/groups/messages/{id}/report`, `PUT /api/groups/{id}/mute` |
| Teacher | `GET /api/teacher/class`, `POST /api/teacher/class/{learnerId}/nudge`, `GET/POST /api/teacher/homework`, `PUT /api/teacher/homework/{id}` |
| Staff notices | `POST /api/staff/notices`, `GET /api/staff/notices/mine`, `GET /api/staff/notices`, `PUT /api/staff/notices/{id}` |
| Principal | `GET /api/principal/dashboard`, `GET /api/principal/approvals`, `POST .../approvals/{notices|homework}/{id}/approve`, `POST .../request-changes`, `PUT .../approvals/{notices|homework}/{id}`, `GET/PATCH /api/principal/settings` |
| School office | `/api/school/overview`, `/collections` (+ `/remind`, `/export`), `/access/requests` (+ `/approve`, `/decline`), `/access/staff`, `/groups` (+ `/mode`, `/reported`, `/messages/{id}/hide`), `/fees` (+ `/remind`), `/lists`, `/lists/{campus}/{grade}`, `/uniform`, `/uniform/stock/{id}/restock`, `/payouts` |

Errors come back as JSON problem details with a message written for the person using the app
(400 invalid input, 401 not signed in, 403 not allowed, 404 not found, 409 conflict, 422 rule broken,
for example "Your wallet has R120.00").

## Code layout

Layered (MVC) packages under `src/main/java/co/za/unicard`:

```
controller/  REST endpoints (/api/...)                 service/     business rules and transactions
repository/  Spring Data JPA repositories              model/       JPA entities and enums
dto/         request and response records              config/      security, OpenAPI and clock beans
security/    JWT issuing, current user, actor          exception/   error types and JSON error handler
util/        money maths, audience rules               seed/        demo data from the prototype
```

Front ends (npm workspaces from the repository root):

```
packages/api/     @unischool/api: OpenAPI spec, generated types, typed client, shared business rules
web/src/lib/      API client instance, sign-in state (token in sessionStorage)
web/src/components/  UI kit (ui.tsx), app shell with role switcher, shared notices and groups
web/src/roles/    one folder per role: parent, learner, teacher, principal, office, tuckshop
web/src/pages/    sign in, ask for access, the public family gift page (/give/<token>)
web/e2e/          Playwright browser tests
```

## Not built yet (stubs or next steps)

- **Real payments:** wallet top-ups, card and Pay in 4 checkouts, and family gifts are recorded directly.
  In production they should only be confirmed after the payment provider's callback.
- **Notifications:** reminders and nudges return who *would* be told. Push, SMS and email delivery isn't built.
- **File uploads:** notice and homework attachments store a file name only.
- **Backups and hosting:** choose a managed PostgreSQL in a South African region (e.g. AWS Cape Town,
  Azure or Google Cloud Johannesburg) with automated backups turned on.
- **Holidays:** the school calendar knows weekends, not public holidays or term dates.
- **Mobile apps:** the Expo app for parents and learners (sharing `packages/api`) isn't started yet.
