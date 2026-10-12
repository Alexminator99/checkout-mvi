# Checkout MVI

A proof of concept of an MVI architecture that keeps scaling. A checkout screen has eight sections that depend on each other: the cart, the address, shipping, the promo code, payment, the summary, placing the order and connectivity. Here each section's logic lives in small, pure, tested pieces, and the ViewModel stays the same size however much the screen grows.

The design and its reasoning are written up in the [design spec](docs/specs/2026-10-12-checkout-mvi-poc-design.md).

## Run it

You need JDK 17 or newer and the Android SDK with platform 37.

```bash
./gradlew test
```

```bash
./gradlew installDebug
```

`installDebug` needs a running emulator or a connected device.

The app talks to an in-memory demo backend with about 600 ms of latency, so it needs no server. Things to try:

| Try | What happens |
|---|---|
| Promo `SAVE5` | €5 off |
| Promo `BIG50` | €50 off orders of €100 or more. Below that it is refused; raise the mug quantity to qualify. |
| Promo `EXPIRED`, or any other code | Expired / not valid |
| Address "PO Box" or "Island cabin" | Cannot be delivered to, and Pay stays disabled |
| Card Mastercard •••• 0002 | Declined: a dialog explains it |
| Card Amex •••• 3155 | The bank asks for confirmation |
| Airplane mode | Offline banner, and Pay is disabled until the connection is back. The in-memory backend keeps answering while offline, so promo, address and shipping quotes still work; only paying is blocked |
| Remove every item | Empty cart, no shipping options, Pay disabled. The in-memory cart refills only when the app process restarts |

The demo customer is a member, so every shipping option except Express is free (Standard and Pickup point).

## How the MVI loop works

```
 UI ── CheckoutIntent ──► CheckoutViewModel.onIntent ──► Store.dispatch(action)
                                                            │
                                     CheckoutReducer.reduce(state, action)
                                                            │
                                  Transition(state, effects, events)
                                   │              │               │
                          state committed   CheckoutEffectHandler  events ──► ObserveAsEvents
                                   │          (one use case)                (navigation, snackbar)
                          CheckoutProjector         │
                                   │          result CheckoutAction ──► Store.dispatch
                          CheckoutUiState ──► UI
```

| Piece | Role |
|---|---|
| `CheckoutState` | The single source of truth, shaped like the domain |
| `CheckoutIntent` | What the UI may send; a subset of `CheckoutAction` |
| `CheckoutAction` | Everything the reducer handles: intents and effect results, grouped per section |
| `CheckoutReducer` | Routes each action group to one pure slice reducer. While an order is being placed it ignores every intent, so new sections are frozen during payment automatically |
| `CheckoutEffect` | A side effect as plain data; keyed effects are latest-wins |
| `CheckoutEffectHandler` | Runs an effect through one use case and reports the result as an action |
| `CheckoutEvent` | A one-off signal (navigation, transient error), collected with `ObserveAsEvents` |
| `CheckoutProjector` | Turns state into render-ready `CheckoutUiState`, one section projector per section |
| `Store` (`:core:mvi`) | The generic loop every screen reuses: ordered reducing, effects, events |

### Following one tap: "Apply" on a promo code

1. `PromoAction.ApplyClicked` (`contract/PromoAction.kt` in `:feature:checkout:presentation`) reaches `PromoReducer`.
2. `PromoReducer` marks the promo as being checked and asks for `CheckoutEffect.RedeemPromo`.
3. `CheckoutEffectHandler` calls `RedeemPromoUseCase` and reports `PromoAction.Redeemed` or `PromoAction.Rejected`.
4. `PromoReducer` applies the answer, and `PromoSectionProjector` shows it.

### Adding a section

1. Add a sealed `XAction`; the actions the UI sends also implement `CheckoutIntent`. The compiler then refuses to build until `CheckoutReducer` routes it.
2. Add `XReducer` and route it: one line.
3. If the section needs I/O, add a `CheckoutEffect` and its branch in `CheckoutEffectHandler`, calling one use case.
4. Add `XSectionProjector`, its UI model and its composable.

## Modules

```
:app ──────────────► :feature:checkout:presentation ──► :feature:checkout:domain ──► :core:domain
  │                          │         │
  │                          │         └──► :core:mvi
  │                          └──► :core:ui ──► :core:domain
  ├──► :core:ui
  └──► :feature:checkout:data ──► :feature:checkout:domain
```

- `:core:mvi`, `:core:domain` and every `domain` module are pure Kotlin.
- Presentation never depends on data. Only `:app` sees both, so only `:app` assembles the Hilt graph. `:app` also depends on `:core:ui` directly, for `ShopTheme`.
- A new feature gets the same `domain` / `data` / `presentation` split.
- `build-logic` holds the convention plugins (`shop.jvm.library`, `shop.android.library`, `shop.android.feature`, …), so each module's build file is a few lines.

## Tests

`./gradlew test` runs 202 tests, all with hand-written fakes:

- **Reducers and projectors** are plain functions, so their tests need no coroutines and no Android.
- **Use cases** are tested over fakes of the repository interfaces, which live in the domain module's `testFixtures`.
- **Repositories** are tested over the demo backend, covering every scripted error code.
- **`CheckoutViewModelTest`** runs the whole loop: the real store, reducers, effect handler, use cases and projector over fakes.
