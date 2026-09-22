---
name: scaffold-entity
description: Use when adding a new CRUD entity/feature to BrandX (a new domain object with its own Controller, Service, Repository, and list/form/view Thymeleaf templates). Generates the new stack by cloning the codebase's vetted reference patterns instead of whichever existing entity gets copy-pasted next, so the new code doesn't reintroduce bugs the CODE-REVIEW-LOOK-AND-FEEL.md review already found duplicated across Product/Customer/Order.
---

# Scaffolding a new entity

BrandX has three parallel CRUD stacks (`Product`, `Customer`, `Order`), each with a
Controller, Service, Repository, and `list.html` / `form.html` / `view.html`. The
2026-09-18 review (`CODE-REVIEW-LOOK-AND-FEEL.md`) found the *same* bug independently
duplicated across these stacks because new code kept copying the nearest existing
example rather than the correct one. This skill is the fix: it names, for each part of
the stack, which existing file is the reference to copy and which is the cautionary
example to avoid.

## Service layer — `create()` must never trust a submitted id

**Copy:** `OrderService.create`/`applyForm` (`src/main/java/com/brandx/service/OrderService.java`),
or `ProductService.create`/`update` and `CustomerService.create`/`update` — every
`create()`/`update()` in this codebase builds/mutates a **managed** entity and copies
only whitelisted fields onto it, never saving the request-bound instance directly.

**Why this matters:** the create forms round-trip a hidden `id` field, and
`SimpleJpaRepository.save` treats a non-null id as a merge — saving a request-bound
entity directly in `create()` would let a POST to the create endpoint overwrite an
arbitrary existing row. This was the 2026-09-18 review's top finding (since fixed in
`ProductService`/`CustomerService`); do not reintroduce it by having a new entity's
`create()` save the bound instance directly.

Rule for the new `<Entity>Service.create(...)`:
- If the entity has non-trivial relationships or a variable-length collection (like
  `Order`'s line items), bind a form DTO under `web/form/` with **no `id` field at
  all** (mirror `OrderForm`/`OrderItemForm`), and build a fresh `new <Entity>()` inside
  `create()`.
- If the entity is a flat record (like `Product`/`Customer`), still don't save the
  request-bound instance directly — either bind the same DTO pattern, or add
  `@InitBinder` with `binder.setDisallowedFields("id")` on the controller and never
  round-trip a hidden `id` input on the create form specifically (the edit form can
  keep it, since `update(id, ...)` takes its id from the path, not the body).

## Repository — fetch what the templates read

**Copy:** `OrderRepository.findDetailById` (`@EntityGraph`/fetch-join covering
`customer` and `items.product`) and `OrderService.totalsFor` (one aggregate query per
page, not one per row).

`spring.jpa.open-in-view` is `false` project-wide, so any association the new
`view.html`/`list.html` touches must already be fetched by the query the controller
called — an unfetched lazy access throws `LazyInitializationException` at render time
instead of silently issuing an extra query. Check every field the new templates read
against the query that loaded the entity before wiring them up.

## Templates — icon toggle and shared fragments

**Copy the icon-badge convention:** add `icon-<entity>` and `badge-<entity>` fragments
to `fragments/layout.html` alongside the existing ones, and reference them with
`th:replace` from the new templates — don't inline raw `<svg><path>` markup per row
(the review's performance finding: duplicated inline SVG paths bloat every row by
~1.3KB).

**Watch for the double-icon bug**, already fixed in `products/form.html`,
`customers/form.html`, and `orders/form.html` (see each template's submit button, where
the `icon-plus`/`icon-check` pair is wrapped in `<span th:if>`/`<span th:unless>`) but
easy to reintroduce in a new template. Thymeleaf evaluates `th:replace` *before*
`th:if`/`th:unless` (inclusion is precedence order 1, conditionals are order 3), so
putting the condition directly on the same element as `th:replace` gets discarded and
**both** icons render. Always put the condition on a wrapping element instead, matching
the existing three templates:

```html
<span th:if="${entity.id} == null"><th:block th:replace="~{fragments/layout :: icon-plus}"></th:block></span>
<span th:unless="${entity.id} == null"><th:block th:replace="~{fragments/layout :: icon-check}"></th:block></span>
```

**Delete forms:** keep them as real `<form method="post">` POSTs (never a GET link),
matching `products/list.html`. `onsubmit="return confirm(...)"` is a UX courtesy only —
it is not a security control. CSRF protection is on (Spring Security), and the token
arrives automatically **because the form uses `th:action`**. A plain `action="/..."`
gets no token and the POST is rejected with 403, so always write `th:action`. Never add
a one-off token mechanism for the new entity, and never `csrf().disable()`.

**Authorization on the new stack.** Two separate things, and the scaffold needs both:

1. `@PreAuthorize` on the new **service**'s mutating methods — this is the enforcement.
   Match the existing split: `hasRole('ADMIN')` for create/update/delete, unless the new
   entity is order-like day-to-day work, in which case `hasAnyRole('ADMIN', 'STAFF')`
   for create/update with delete still ADMIN-only. Reads stay unannotated; the filter
   chain already requires a signed-in user.
2. `sec:authorize` on the new templates' New/Edit/Delete controls — this only hides
   them. Add `xmlns:sec="http://www.thymeleaf.org/extras/spring-security"` to the
   `<html>` tag of any template that uses it. In a list table, guard the whole
   `<td class="actions">` and add an empty `<td class="actions" sec:authorize="!hasRole('ADMIN')">`
   beside it so the row keeps the header's cell count.

A new stack with only the template guards looks correct in a browser and is wide open to
curl. Verify with the seeded `staff` account, not just `admin`.

## After scaffolding

Run the `review` command (`/review`) against the new files before considering the
feature done — pass `diff` scope so it checks the new stack against exactly these
patterns.
