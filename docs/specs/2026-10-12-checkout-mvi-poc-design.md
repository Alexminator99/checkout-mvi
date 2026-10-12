# Checkout MVI POC — Design

**Date:** 2026-10-12
**Status:** Draft for review
**Baseline:** `4e6747c chore: import checkout MVI prototype`

## 1. Context and goal

This is a proof of concept for Betsson. Their teams already use plain MVI. The POC shows an MVI architecture that keeps scaling as a screen, the number of features and the team grow, while still reading as ordinary MVI.

The checkout screen is the vehicle. It is a good stress test because its sections depend on each other: changing the cart re-quotes shipping and re-checks the promo, picking an address re-validates it, and paying needs every section to be ready.

The prototype in the baseline commit already has the right MVI vocabulary and the right core idea: a pure reducer, effects as data, and an effect handler. This rebuild keeps that design and removes what made it hard to follow, fixes the bugs found in review, and makes it build, test and run.

## 2. Scope

**In scope**

- One checkout screen and one order confirmation screen.
- The checkout features of the prototype:
  - cart quantities, including removing a line
  - address selection and deliverability check
  - shipping quotes, with express options behind a feature flag and free standard shipping for members
  - promo codes
  - payment method
  - order summary
  - placing the order
  - offline banner
- A demo backend, in memory with simulated latency and scripted outcomes, so the app runs on an emulator with no server.
- Multi-module build with convention plugins.
- Unit tests on every layer.

**Out of scope**

- Real network and database (Retrofit, Room), authentication, analytics.
- Restoring state after process death. The state is rebuilt from the data streams; the promo input is lost.
- Any feature other than checkout and confirmation.
- Visual design beyond Material 3 defaults. iOS.

## 3. Success criteria

1. `./gradlew test assembleDebug` passes on a clean checkout of the repo.
2. On an emulator, the full flow works:
   - edit the cart
   - pick an address and see its deliverability
   - pick shipping
   - apply and remove promo codes, including each scripted error
   - pick a payment method
   - place the order and land on confirmation
   - a declined card shows a blocking dialog
   - airplane mode shows the offline banner and disables paying
3. Any user action can be followed in at most three files: the action type, its slice reducer, and (if it does I/O) the effect handler branch that calls one use case.
4. One top-level type per file. A sealed hierarchy lives in one file with its subtypes.
5. No hard-coded user-facing strings, no mocking libraries (fakes only).

## 4. Architecture

### 4.1 The MVI loop

```
 UI ── CheckoutIntent ──► CheckoutViewModel.onIntent ──► Store.dispatch(action)
                                                            │
                                     CheckoutReducer.reduce(state, action)
                                                            │
                                  Transition(state, effects, events)
                                   │              │               │
                          state committed   CheckoutEffectHandler  events ──► Channel ──► ObserveAsEvents
                                   │          (calls a use case)                          (navigation, snackbar)
                          CheckoutProjector         │
                                   │          result CheckoutAction ──► Store.dispatch
                          CheckoutUiState ──► UI
```

Vocabulary, unchanged from the prototype:

| Type | Meaning |
|---|---|
| `CheckoutState` | Single source of truth. Shaped like the domain: no strings, no formatting. |
| `CheckoutIntent` | What the UI may send. A subset of `CheckoutAction`. |
| `CheckoutAction` | Everything the reducer handles: intents plus effect results, grouped per section. |
| `CheckoutEffect` | A side-effect command, plain data, produced by a reducer and run by the effect handler. |
| `CheckoutEvent` | A one-off UI signal: navigation or a transient error. |
| `CheckoutUiState` | Render-ready state produced by the projector. |

Rules:

- **Reducers stay pure:** no I/O, coroutines, clock, randomness or Android types.
- **Effects are plain data, decided by the reducer:** they carry their own latest-wins policy (`KeyedEffect`).
- **The effect handler only translates:** effect → use case → action. It contains no decisions.
- **Late results are ignored:** results carry the id they were computed for (address id), and a result for something the user has moved away from is dropped by the reducer.

### 4.2 What changes compared with the prototype

These are the parts that made the prototype hard to follow, and how each is fixed:

1. **No generic interfaces bound in DI.** `StoreFactory` and all 17 `@Binds` for reducers, the effect handler and projectors are deleted.
   - The ViewModel injects the concrete `CheckoutReducer`, `CheckoutEffectHandler` and `CheckoutProjector`. These compose concrete, `internal`, `@Inject` slice reducers and section projectors.
   - This removes the "keep generics invariant for Dagger" workaround.
   - It was verified that Hilt's KSP processor and the generated factory accept an `@Inject internal constructor` taking `internal` collaborators.
   - Dependency inversion stays where it pays off: at the module boundary, where the domain owns the repository interfaces and the data module implements them.
2. **The ViewModel creates its store directly:** `Store(initialState, viewModelScope, reducer, effectHandler)`.
3. **`Projector` is no longer a framework interface.** Projectors are plain classes. `Reducer` and `EffectHandler` stay in `:core:mvi` because the generic `Store` consumes them.
4. **Events use `ObserveAsEvents`.** This lifecycle-aware composable collects only while the screen is `STARTED`, on `Dispatchers.Main.immediate` so no event is lost between lifecycle transitions, and with `rememberUpdatedState` so it always calls the latest callback. It replaces the hand-written collector. The screen's private `UiText.resolve` is removed in favour of `UiText.asString(context)` in `:core:ui`.
5. **`Started` is no longer an intent.** Only the ViewModel dispatches it, so the UI cannot restart the data subscription.
6. **One type per file,** in every module (see the layouts below).
7. **Business rules live in the domain, once.** Today "members ship standard for free" is written in both the totals use case and a projector.

### 4.3 Module graph

```
:app ──────────────► :feature:checkout:presentation ──► :feature:checkout:domain ──► :core:domain
  │                          │         │
  │                          │         └──► :core:mvi
  │                          └──► :core:ui ──► :core:domain
  └──► :feature:checkout:data ──► :feature:checkout:domain
```

- `:core:mvi`, `:core:domain` and `:feature:checkout:domain` are pure Kotlin (JVM) modules.
- `:feature:checkout:presentation` cannot see `:feature:checkout:data`. Only `:app` sees both, so only `:app` assembles the Hilt graph.
- New features follow the same three-module split (`domain`, `data`, `presentation`), which keeps build times and ownership per feature.

### 4.4 Build and toolchain

- **Convention plugins.** `build-logic/convention`, in the same style as the SIROC Android app. Plugin ids:
  - `shop.jvm.library`
  - `shop.android.library`
  - `shop.android.library.compose`
  - `shop.android.feature` (library + Compose + Hilt + the presentation dependencies)
  - `shop.android.application`
  - `shop.hilt`

  Each module's build file is then a few lines. The version catalog includes the plugin aliases, and typesafe project accessors are enabled.
- **`gradle.properties`** sets `android.useAndroidX=true` and a 4 GB heap. Its absence is why the prototype did not compile.
- **Toolchain target: current stable versions.**

  | Component | Version |
  |---|---|
  | Gradle | 9.6.0 (wrapper as committed) |
  | AGP | 9.4.1 |
  | Kotlin | 2.4.21 |
  | KSP | 2.3.12 |
  | Hilt / AndroidX Hilt | 2.60.1 / 1.4.0 |
  | Compose BOM | 2026.09.00 |
  | Navigation Compose | 2.10.2 |
  | Lifecycle | 2.11.0 |
  | Activity Compose | 1.13.0 |
  | kotlinx-serialization / kotlinx-coroutines | 1.11.0 / 1.11.0 |
  | Turbine | 1.2.1 |
  | compileSdk / targetSdk / minSdk | 36 / 36 / 26 |

  The current Hilt Gradle plugin requires AGP 9, so these move together.
- **Fallback, decided now:** if that toolchain is not green by the end of the first implementation task, the POC uses the set already verified on this machine (AGP 8.9.1, Kotlin 2.1.20, KSP 2.1.20-2.0.0, Hilt 2.56.1, Compose BOM 2025.04.01). The upgrade then becomes a follow-up.

## 5. Domain — `:core:domain` and `:feature:checkout:domain`

### 5.1 Core (`:core:domain`)

- `Money`: value object.
  - Arithmetic across currencies fails fast.
  - Amounts are normalised to the currency's scale on construction, so two equal amounts are equal regardless of how they were written (fixes the `BigDecimal` scale issue).
- `Outcome<out T, out E>`: the result type, generic over the error.
  - `Success(value)` / `Failure(error)`, with `map`, `fold`, `onSuccess`, `onFailure`.
  - It is generic because a sealed error hierarchy cannot span modules: each feature owns its errors.

### 5.2 Checkout errors

`CheckoutError` is one sealed hierarchy in checkout domain:

- Network, Unauthorized, NotFound, Unexpected
- Promo: Invalid, Expired, MinimumNotReached
- Payment: Declined, RequiresAction
- OutOfStock, PriceChanged

`typealias CheckoutOutcome<T> = Outcome<T, CheckoutError>`.

### 5.3 Models

One file each: `Cart`, `CartLine`, `Address`, `AddressValidation`, `ShippingOption`, `Promo`, `PaymentMethod`, `Customer`, `Order`, `OrderRequest`, `OrderTotals`, `CheckoutContext`, `FeatureFlag`.

`OrderRequest` gains `checkoutId`. That makes the idempotency key unique per checkout but stable across retries of the same request. It fixes the prototype bug where two identical orders placed on different days collided.

### 5.4 Business rules

Pure functions in a `rule` package, one file per rule. Reducers, selectors and projectors can call them without injection:

- **Member pricing:** `ShippingOption.priceFor(customer)`. Members get non-express shipping free. This is the single place the member rule lives.
- **Express flag:** `List<ShippingOption>.availableFor(isExpressEnabled)` hides express options when the flag is off.
- **Default shipping choice:** `List<ShippingOption>.preferredFor(currentId, customer)` keeps the current pick if it is still offered. Otherwise it picks the lowest price for this customer, and breaks ties by the fewest days. A member therefore gets free standard shipping rather than the slower free pickup point.
- **Quantity cap:** `MAX_QUANTITY_PER_LINE = 10`.

### 5.5 Repository interfaces

Owned by the domain, implemented by data, one file each:

- `CartRepository` (observe, set quantity, remove line)
- `AddressRepository` (observe, validate)
- `ShippingRepository` (quote)
- `PromoRepository` (redeem)
- `PaymentMethodRepository` (observe)
- `OrderRepository` (place)
- `CustomerRepository` (observe)
- `NetworkMonitor` (is online)
- `FeatureFlagRepository` (observe flag)

Streams backed by a remote source emit `CheckoutOutcome`, so a failed fetch is a value, not an exception that tears down collectors.

### 5.6 Use cases

One class per file, `@Inject` constructor, `operator fun invoke`:

| Use case | Responsibility |
|---|---|
| `ObserveCheckoutContextUseCase` | Combine cart, addresses, payment methods, customer, connectivity and the express flag into one `CheckoutContext` stream. |
| `UpdateCartQuantityUseCase` | Clamp to the per-line maximum; a quantity of 0 removes the line. |
| `ValidateAddressUseCase` | Ask whether an address is deliverable. |
| `GetShippingQuotesUseCase` | Quote shipping for an address and cart. |
| `RedeemPromoUseCase` | Normalise the code (trim, upper-case) and redeem it against the cart. |
| `PlaceOrderUseCase` | Place an order request. |
| `CalculateOrderTotalsUseCase` | Pure. Subtotal, shipping (via `priceFor`), discount capped at the subtotal, total. |

## 6. Data — `:feature:checkout:data`

### 6.1 Remote

- **Interface:** `CheckoutRemoteDataSource` works with DTOs (one per file).
- **Demo implementation:** `DemoCheckoutRemoteDataSource` keeps an in-memory backend with about 600 ms of latency per call. It signals failures the way an HTTP client would: `CheckoutApiException(code)` for HTTP-style errors and `IOException` for connectivity.
- **Demo data:**

  | Area | Data |
  |---|---|
  | Addresses | Home (default) and Office are deliverable; "PO Box 42" is rejected as a PO box; "Island cabin" is outside the service area. |
  | Quotes | Standard €4.99 (3 days), Pickup point €2.99 (4 days), Express €12.99 (1 day). |
  | Promo codes | `SAVE5` gives €5 off; `BIG50` gives €50 off with a €100 minimum (rejected below it); `EXPIRED` is expired; anything else is invalid. |
  | Payment methods | Visa •••• 4242 succeeds; Mastercard •••• 0002 is declined; Amex •••• 3155 requires bank confirmation. |
  | Customer | A member, so free standard shipping is visible. |
  | Express flag | On. |

  The demo backend does not empty the cart after an order, so the flow can be repeated.

### 6.2 Local and platform

- **Cart:** `CartDataSource` interface with `InMemoryCartDataSource`, a `StateFlow` seeded with two demo lines (a mug ×2, a T-shirt ×1). This is the seam where Room would go.
- **Connectivity:** `ConnectivityNetworkMonitor` stays a real Android adapter, so airplane mode drives the offline banner.

### 6.3 Repositories and error mapping

- Repository implementations are `internal`, one per file, and bound to the domain interfaces with `@Binds`.
- `safeCall` is the single place that maps exceptions to `CheckoutError`:
  - It rethrows `CancellationException`.
  - `IOException` becomes `Network`.
  - Each endpoint maps its own error codes (for example, 410 on redeem means `Promo.Expired`), with a common default.
- **Idempotency:** `OrderRepositoryImpl` derives the key from the whole `OrderRequest`, which now includes `checkoutId`.

### 6.4 DI

Hilt modules, one per file:

- `RepositoryModule` (binds)
- `DataSourceModule` (demo data sources, IO dispatcher, store currency)

## 7. Presentation — `:feature:checkout:presentation`

### 7.1 Package layout

```
CheckoutViewModel.kt
contract/   CheckoutState, AddressSlice, ShippingSlice, PromoSlice, PromoStatus, Submission, Loadable,
            CheckoutAction, CheckoutIntent, LifecycleAction, ContextAction, CartAction, AddressAction,
            ShippingAction, PromoAction, PaymentAction, OrderAction, CheckoutEffect, CheckoutEvent,
            CheckoutSelectors (extension functions)
reducer/    CheckoutReducer (routes only), one reducer per slice (Lifecycle, Context, Cart, Address,
            Shipping, Promo, Payment, Order), CheckoutTransition (type aliases), cross-slice steps one
            per file: SelectAddress, RequestQuotes, RevalidatePromo, ReconcileShippingSelection
effect/     CheckoutEffectHandler
projector/  CheckoutProjector, ReadyCheckout, one projector per section (Cart, Address, Shipping, Promo,
            Payment, Summary, PlaceOrderButton), CheckoutErrorText, AddressReasonText
ui/         CheckoutScreen.kt (CheckoutRoute + CheckoutScreen), component/ (one composable per section),
            model/ (one UI model per file), confirmation/OrderConfirmationScreen.kt
navigation/ CheckoutDestination, OrderConfirmationDestination, CheckoutNavigation (NavGraphBuilder extensions)
```

### 7.2 State

- The state is the prototype's `CheckoutState` plus `checkoutId`, which the ViewModel generates once with `UUID.randomUUID()`. Reducers never create ids.
- `orderRequestOrNull()` stays the single definition of "ready to pay". The order reducer uses it as its guard, and the place-order button projector uses it for the enabled state.

### 7.3 Reducers

- `CheckoutReducer` routes with an exhaustive `when` over the sealed action groups. Adding a section is a compile error until it is routed.
- Slice reducers and cross-slice steps keep the prototype's behaviour (already covered by its reducer tests), with these changes:
  - decrementing a line at quantity 1 removes it
  - the shipping selection uses the domain rules from §5.4

### 7.4 Effect handler

- One `when` branch per effect; each branch calls one use case and maps the outcome to a result action.
- Keyed effects:

  | Effect | Key |
  |---|---|
  | `ObserveContext` | context |
  | `ValidateAddress` | address-validation |
  | `LoadShippingQuotes` | shipping-quotes |
  | `RedeemPromo` | promo |

### 7.5 Events

- `CheckoutEvent.NavigateToConfirmation(orderId)` and `CheckoutEvent.ShowError(error: CheckoutError)`.
- The store delivers them through an unlimited `Channel`, so emitting an event never blocks the reducer loop and order is kept.
- `CheckoutRoute` collects them with `ObserveAsEvents` and maps errors to text with `toUiText().asString(context)`.
- Errors that must stay visible are state, not events:
  - promo rejection is shown inline
  - a payment failure is a blocking dialog

### 7.6 Projectors

- Pure classes: same state in, same UI state out.
- `CheckoutProjector` decides between Loading, Error and Content and composes the section projectors.
- Formatting goes through the `MoneyFormatter` interface. `UiText` carries resource ids so projectors need no `Context`.

### 7.7 UI

- `CheckoutRoute` (stateful: `hiltViewModel()`, `collectAsStateWithLifecycle()`, `ObserveAsEvents`, snackbar).
- `CheckoutScreen` is stateless and previewable (`state`, `onIntent`). Each section is its own composable file with a preview.
- All copy and content descriptions come from string resources. That includes the quantity buttons, the card mask and the address line format, which are hard-coded in the prototype.

### 7.8 Navigation

- Navigation Compose with type-safe `@Serializable` destinations.
- The feature exposes `NavGraphBuilder.checkoutScreen(onOrderPlaced)` and `NavGraphBuilder.orderConfirmationScreen(onBackToCheckout)`.
- Placing an order navigates to confirmation and pops checkout. "Back to checkout" opens a fresh checkout, which gets a new `checkoutId`.

## 8. App — `:app`

- `ShopApp` (`@HiltAndroidApp`).
- `MainActivity` (`@AndroidEntryPoint`, edge-to-edge, `ShopTheme`).
- `ShopNavHost`, `AndroidManifest.xml`, app strings.

It is the only module that depends on both presentation and data.

## 9. Testing

Fakes only. JUnit 4, `kotlinx-coroutines-test` and Turbine.

| Module | What is tested |
|---|---|
| `:core:mvi` | `Store`: actions reduced in order, keyed effects latest-wins, results dispatched back, events delivered once. The store runs on a scope driven by the test scheduler, never `backgroundScope` with `advanceUntilIdle()`. That combination is why the prototype's 7 store tests failed with the real library. |
| `:core:domain` | `Money` arithmetic, scale normalisation and currency mismatch. |
| `:feature:checkout:domain` | One test class per use case and per rule file. |
| `:feature:checkout:data` | Repositories over a fake remote data source: DTO mapping, error-code mapping, idempotency key stability. |
| `:feature:checkout:presentation` | One test class per slice reducer (pure, no coroutines), one per section projector, the effect handler with fake repositories, and `CheckoutViewModel` end to end with `Dispatchers.setMain` and Turbine. The end-to-end flows: start → ready, quantity change → re-quote, apply promo, place order → navigation event, declined card → blocking dialog. |

- Fakes for the domain's repository interfaces live in the domain module's `testFixtures`, one fake per file.
- Presentation and data tests consume them with `testImplementation(testFixtures(projects.feature.checkout.domain))`.

## 10. Review findings and where they are resolved

| Finding | Resolution |
|---|---|
| 7 of 31 tests fail with the real `kotlinx-coroutines-test` | §9 store test setup |
| Android modules don't compile (`android.useAndroidX`, heap) | §4.4 `gradle.properties` |
| `:app` has no manifest or Activity, and the Hilt graph is incomplete | §8, §6.4 |
| Retrofit DTOs have no serialization setup | Retrofit removed; demo data source (§6.1) |
| Idempotency key collides for identical orders | §5.3, §6.3 |
| Cart line cannot be removed | §7.3 |
| Member shipping rule duplicated | §5.4 |
| Hard-coded UI strings | §7.7 |
| `Money` equality depends on scale | §5.1 |
| Event collection can drop events or call a stale callback | §7.5 `ObserveAsEvents` |
| DI-bound generics, `StoreFactory`, 17 `@Binds` | §4.2 |

## 11. Follow-ups (not part of this POC)

- Replace the demo data source with a Retrofit-backed one and the in-memory cart with Room. Both are single bindings in `DataSourceModule`.
- Restore promo input and selections after process death with `SavedStateHandle`.
- Split `CheckoutEffectHandler` per section, the same way reducers are split, once the number of effects grows.
