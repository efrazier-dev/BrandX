# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project summary

BrandX is a server-rendered CRUD app (product/order/customer management) built with
Spring Boot 4.1 / Java 21, Thymeleaf templates, and Spring Data JPA. Three entities —
Product, Customer, Order (with OrderItem lines) — each get a controller, service,
repository, and set of list/form/view templates following an identical pattern. There is
no REST API, no JS framework, and no build step beyond Maven; pages are classic
GET-render / POST-redirect flows with a small amount of vanilla JS for dynamic order
line rows (`static/js/order-lines.js`).

**Authentication is form login against a local `app_users` table, with two roles.**
Every screen requires a signed-in user; `ADMIN` may change anything, `STAFF` may read
everything and create or edit orders. This is a proof-of-concept front door, chosen
because it is the cheapest one that exercises the role model — the intended path is
OIDC/SSO later, which replaces `formLogin()` and leaves the roles, the `@PreAuthorize`
annotations and the `sec:authorize` guards in place. See `config/SecurityConfig.java`.

Accounts exist only so role checks have something to resolve against: there is no
controller, service or screen for managing them, and the only accounts anywhere come
from `DevUserSeeder` on the dev profile. **Provisioning a first account outside dev is
unsolved** — flag it if a change starts to depend on real user management.

## Commands

JDK 21 and Maven wrapper are required; the JDK path is not recorded in the repo (only
the release is pinned in `pom.xml`), so `JAVA_HOME` must be set per shell.

```powershell
# Run locally (dev profile: in-memory H2, seeded demo data, template hot-reload)
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\mvnw.cmd -B spring-boot:run
```

Serves on http://localhost:8080. Records created while testing vanish on restart (H2 is
in-memory).

Every screen needs a sign-in. The dev profile seeds two throwaway accounts:
`admin`/`admin` (full access) and `staff`/`staff` (read everything, plus create and edit
orders). Use `staff` when checking that a change is correctly hidden *and* refused —
hiding a control and refusing the POST behind it are separate mechanisms.

```powershell
.\mvnw.cmd test                              # full test suite
.\mvnw.cmd -Dtest=BrandXApplicationTests test # single test class
.\mvnw.cmd -Ppostgres spring-boot:run         # run against Postgres instead of H2
.\mvnw.cmd -Pmysql spring-boot:run            # run against MySQL instead of H2
```

Database selection is two independent axes: the `-Ppostgres`/`-Pmysql` Maven profile
picks the JDBC driver at build time; `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` env vars pick
the connection at runtime. No Java code or mapping annotation names a specific database
— the Hibernate dialect and JDBC driver class are inferred from the connection, never
hardcoded, so as not to break portability.

No linter or static-analysis config exists in this repo, by choice — don't add one
unasked, and don't act as one in review.

## Architecture

**Layering.** Each of the three entities (Product, Customer, Order) is implemented as a
vertical slice: `web` controller → `service` (transactional boundary) → `repository`
(Spring Data JPA) → `domain` entity → Thymeleaf templates for list/form/view. Product and
Customer are structurally identical slices; Order is the more complex one, because it
owns a child collection (OrderItem lines) and references the other two entities.

**Entity binding vs. form-DTO binding.** Product and Customer controllers bind form
submissions directly onto the JPA entity. Order does not — its screen submits ids and a
variable number of line rows that don't map onto the entity graph, and line prices must
be decided server-side rather than trusted from the request, so it binds onto a separate
form object instead and the service translates that into entity state. When adding a
screen with similar shape (ids + child rows + server-decided values), follow the Order
pattern rather than the Product/Customer one.

**Read/write path shaped by `open-in-view: false`.** The Hibernate session closes before
Thymeleaf renders, so the service layer is responsible for fetching everything a template
needs up front (via `@EntityGraph` or `join fetch`) — lazy access during rendering fails
loudly rather than silently issuing extra queries. This pushes fetch strategy and
aggregation (e.g. order totals) into the repository/service layer instead of the
template or the entity.

**Order-specific domain rules:** line prices are snapshotted at write time rather than
re-read from the current product price, so editing a product later doesn't rewrite order
history; order numbers are a separate human-friendly identifier from the primary key,
generated with a retry-on-collision scheme backed by a DB unique constraint.

**Error handling is intentionally split two ways.** Exceptions that represent "this page
doesn't exist" flow through a global handler into a dedicated error view. Exceptions that
represent a business-rule refusal (duplicate value, entity still in use) are instead
caught locally in the controller and surfaced as a flash message or a field-level
validation error — because those belong back on the screen the user was already on, not
on a separate error page. An authorization refusal is a third case with its own view
(`access-denied.html`), kept distinct from not-found so a 403 never implies the record
is missing.

**Authorization is enforced at the service layer, not the controller or the template.**
`@PreAuthorize` sits on the service methods because that is the transactional boundary —
a second controller or a future REST endpoint reaching the same method is checked too.
The `sec:authorize` attributes in the templates only hide controls; they are
presentation, and a review should treat a guard that exists *only* in a template as
missing. Note the consequence for testing: MockMvc records the forward to
`/access-denied` without executing it, so a status assertion alone can pass while the
real container returns 405 — `AuthController.accessDenied` is deliberately mapped
without an HTTP method restriction because the forward preserves the original POST.

**Users are loaded with their roles eagerly, for the same reason as everything else
above.** Authentication runs in a servlet filter, outside any transaction, and the
`UserDetails` it produces is held in the session — so `AppUserRepository` attaches an
`@EntityGraph` to the login lookup and `AppUserDetailsService` converts roles to
authorities before returning. Anything lazy there fails at the first authorization check,
not at login.

**Shared UI conventions:** list-screen sort/search state round-trips through the URL
rather than the session; templates share a common layout fragment (nav, alerts,
pagination, footer, and a hand-drawn SVG icon set) rather than each page duplicating
chrome; and dev-only demo data is seeded through the repositories at startup rather than
a SQL script, to stay database-agnostic.

## Working with review tooling in this repo

`/review` (or the `senior-reviewer` agent directly) runs a security-first,
then-performance, then-correctness review scoped to either `diff` (changed lines) or
`audit` (diff plus the controllers/services/repositories/entities/templates it touches).
It is read-only — it never edits — and requires an explicit scope; ask the user for
`diff` vs `audit` before invoking it if they haven't said. See
`.claude/agents/senior-reviewer.md` for the full checklist it applies (it encodes most of
the architectural constraints above, e.g. the open-in-view fetch rules and the entity
vs. form-DTO binding pattern).
