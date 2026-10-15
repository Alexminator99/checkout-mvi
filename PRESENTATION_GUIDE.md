# Checkout MVI — presentation and rehearsal guide

Prepared 16 October 2026 for the presentation on 17 October 2026.

**The central message:** a complex screen can keep ordinary MVI while its decisions remain small, explicit, testable, and easy to follow.

This guide is designed to read once tonight, then use as speaker notes tomorrow. The recommended talk is 12–15 minutes, followed by questions. A five-minute version and a final checklist are included.

## 1. Your opening, in your own words

> A checkout is a useful stress test for presentation architecture. Changing a quantity affects shipping and discounts. Changing an address affects delivery approval and quotes. Payment needs all of those decisions to agree.
>
> This example keeps the familiar MVI loop, but separates each section's decisions into pure reducers. Side effects are explicit data, use cases own domain work, and projectors turn domain-shaped state into what the screen renders. The ViewModel mainly connects the loop to the screen lifecycle.
>
> I’ll show one interaction first, then follow it through the implementation.

Before demonstrating payment, establish the boundary:

> This is a runnable architecture proof of concept. It uses an in-memory backend with simulated latency. The card outcomes are scripted and no money moves. Connectivity is a real Android signal.

## 2. What you can confidently claim

- The Android app builds and runs locally with no server to provision.
- The feature has separate presentation, domain, and data modules.
- Reducers make state transitions without doing I/O.
- The effect handler calls use cases and converts their outcomes into actions.
- The same selector controls payment eligibility in the UI and the order reducer.
- Changing dependent inputs invalidates and recomputes shipping and promo state.
- Pending cart writes block payment; overlapping cart writes from the screen are rejected until the first settles.
- Payment preserves the visible order while deferring incoming checkout data. Connectivity can still update independently.
- Address edits invalidate delivery approval even when the address keeps its ID.
- Price-change recovery refreshes product prices, and a failed refresh can be retried.
- Replacing a keyed effect invalidates both its running work and results already queued by its older generation.
- The total and Pay action remain visible while scrolling. Disabled payment has an explanation.
- Reset demo restores the cart and selections for another rehearsal.
- The checked test suite contains 216 passing tests. Debug build and Android lint also pass, with no lint findings in that run.

Be precise about the last point: these are JVM unit/integration tests, plus separate manual emulator checks. They are not 216 device UI tests, and the count is not a coverage percentage.

## 3. The 12–15 minute agenda

| Time | Topic | What to show |
|---|---|---|
| 0:00–1:00 | Problem and scope | The ready checkout |
| 1:00–4:00 | A few dependent interactions | Promo, quantity, address validation |
| 4:00–7:00 | Follow one interaction | Intent → reducer → effect → action → projector |
| 7:00–9:00 | Module boundaries | The module graph and repository interfaces |
| 9:00–11:00 | Async correctness | Pending writes, payment snapshot, stale results |
| 11:00–13:00 | Testing and failure handling | A regression test and declined payment |
| 13:00–15:00 | Success, tradeoffs, questions | Visa confirmation and the limits of the POC |

If the room is interested in the demo, spend longer there and shorten the module tour. Avoid opening every file.

## 4. Prepare the environment before the meeting

Have an emulator or Android device ready, the app already installed, and the checkout already loaded. Build and test beforehand rather than making the audience wait for dependency downloads.

The project compiles to Java 17 bytecode. Use a JDK supported by the checked-in Gradle version; Android SDK platform 37 is needed. If the SDK is not discovered automatically, set `ANDROID_HOME` or create your own untracked `local.properties` with `sdk.dir` pointing to your SDK.

From the repository root:

```bash
./gradlew test assembleDebug lintDebug
./gradlew installDebug
```

The install command needs a running emulator or connected device. Launch the **Checkout** app from the launcher.

Keep these tabs open in your editor:

1. [CheckoutViewModel](feature/checkout/presentation/src/main/kotlin/com/example/feature/checkout/presentation/CheckoutViewModel.kt)
2. [PromoReducer](feature/checkout/presentation/src/main/kotlin/com/example/feature/checkout/presentation/reducer/PromoReducer.kt)
3. [CheckoutEffectHandler](feature/checkout/presentation/src/main/kotlin/com/example/feature/checkout/presentation/effect/CheckoutEffectHandler.kt)
4. [CheckoutReducer](feature/checkout/presentation/src/main/kotlin/com/example/feature/checkout/presentation/reducer/CheckoutReducer.kt)
5. [CheckoutConsistencyTest](feature/checkout/presentation/src/test/kotlin/com/example/feature/checkout/presentation/CheckoutConsistencyTest.kt)
6. [Store](core/mvi/src/main/kotlin/com/example/core/mvi/Store.kt)

Use a comfortable editor font size and close unrelated windows. Keep the guide available locally so the talk does not depend on a browser or network connection.

## 5. Live demo script: the reliable path

The initial demo cart contains two €12 mugs and one €20 T-shirt. The customer is a member. Standard and Pickup point shipping are free; Express costs €12.99. The initial total is **€44.00**.

### Step A — Establish the starting state

Scroll to the top and tap **Reset demo**. Wait for loading and validation to finish.

Show:

- Three items in the cart.
- Home selected and approved for delivery.
- Standard shipping selected.
- The persistent total and **Pay €44.00** action.

Say:

> The screen observes one checkout context. The reducer chooses valid defaults and asks for delivery validation and shipping quotes. The button becomes available when the shared payment preconditions are met.

### Step B — Apply a discount

Scroll until the promo field is visible. Enter `SAVE5`, close the keyboard if needed, and tap **Apply**. Wait for the result.

Expected: the discount is shown and the persistent action becomes **Pay €39.00**.

Say:

> Applying the promo first changes state to “redeeming” and describes an effect. The effect handler calls a use case. The answer returns as an action and the reducer decides the next state. The UI renders that decision.

### Step C — Change a quantity

Scroll to the cart and increase the mug quantity from two to three. Wait for shipping and promo revalidation to settle.

Expected: subtotal €56.00, discount €5.00, free Standard shipping, total **€51.00**.

Say:

> This is the interesting dependency: changing the cart invalidates data derived from it. Shipping quotes and the promo are checked against the new cart. Payment stays blocked while the cart write or dependent work is incomplete.

The local cart write is fast, so do not rely on visually catching its brief pending state. The delayed-write test demonstrates that case deterministically.

### Step D — Make delivery invalid

Choose **PO Box**.

Expected: a delivery warning, disabled Pay, and the persistent explanation **Choose an address we can deliver to.**

Switch back to **Home** and wait until validation and quotes are ready.

Say:

> An address ID alone is not delivery approval. Validation belongs to the selected address data. If that address is edited under the same ID, its old approval is invalidated too.

### Step E — Show a recoverable payment failure

Scroll to payment methods and choose **Mastercard •••• 0002**, then tap Pay.

Expected: a dialog explaining that the payment was declined and suggesting another card.

Dismiss the dialog. Select **Visa •••• 4242** and wait for the selection to update.

Say:

> Expected failures are typed values. A payment failure is durable screen state, so it stays visible until the user handles it. It is not a one-time snackbar that could disappear while the screen is stopped.

### Step F — Finish successfully

Tap Pay with Visa selected.

Expected: a loading state, followed by **Order placed** and an order number. Checkout is replaced in the navigation stack.

Say:

> During payment, new user edits are blocked and incoming checkout updates are deferred. The visible cart continues to represent the submitted order. Once the order succeeds, the completed checkout cannot submit again while navigation is being handled.

Do not promise that the order number will always be `ORD-1001`: the backend counter lives for the app process and earlier rehearsal orders can increment it.

Tap **Back to checkout** to start another checkout. Its checkout identifier is new. The demo intentionally retains cart contents so the flow can be repeated; Reset demo restores the initial quantities and clears the promo.

## 6. Optional demonstrations if time permits

### Minimum-spend discount

1. Reset demo.
2. Apply `BIG50` to the €44 cart: it is rejected because the minimum is €100.
3. Increase mug quantity to seven. Subtotal is €104.
4. Tap Apply again: a previously rejected promo is not automatically retried.
5. Expected total: **€54.00** with free Standard shipping.
6. Decrease mug quantity to six. Subtotal becomes €92; revalidation rejects the applied promo because the minimum is no longer met.

This is a useful demonstration of why discounts cannot simply remain attached to a mutable cart forever.

### Offline behavior

Enable airplane mode through Android Quick Settings and ensure Wi-Fi is actually off too. Some devices keep Wi-Fi enabled in airplane mode.

Expected: offline banner, disabled Pay, and an explanation. Restore connectivity and wait for Android to validate internet access before continuing.

The demo backend still answers promo and quote calls while offline because it runs in memory. Explain this upfront; connectivity only gates payment in this demo.

### Empty cart and reset

Decrement every line to zero. At quantity one, the decrement action becomes removal.

Expected: empty-cart message, no payable shipping selection, and disabled Pay. Scroll to the top and tap Reset demo to recover without restarting the process.

### Additional scripted outcomes

| Input | Expected result |
|---|---|
| `EXPIRED` | Expired-promo message |
| Any unknown promo | Invalid-promo message |
| Island cabin address | Outside-delivery-area warning |
| Amex •••• 3155 | A message saying bank confirmation is needed |

The Amex scenario only shows that outcome. It does not implement a bank-authentication or challenge flow. Product-price conflict recovery is covered by tests; the default demo cards do not script a price conflict.

## 7. Explain the MVI loop clearly

```text
User interaction
    ↓
CheckoutIntent
    ↓
CheckoutViewModel → Store.dispatch
    ↓
CheckoutReducer → relevant slice reducer
    ↓
Transition(state, effects, events)
    ├─ state committed → CheckoutProjector → CheckoutUiState → Compose
    ├─ effect → CheckoutEffectHandler → use case → result action → Store
    └─ event → lifecycle-aware collector → navigation or transient message
```

Use these definitions consistently:

| Term | Meaning in this project |
|---|---|
| State | What the checkout currently knows |
| Intent | An action the UI is allowed to send |
| Action | An intent or an effect result that the reducer handles |
| Reducer | A pure decision about the next state and requested work |
| Effect | A description of work to perform outside the reducer |
| Effect handler | The bridge that executes a use case and reports an action |
| Event | A one-time UI signal, such as navigation |
| Projector | Mapping state to render-ready UI values |

A useful sentence:

> State describes what is true now; an effect describes work to do; an event describes something the UI should handle once.

Do not call every coroutine operation an event. Effects and UI events have separate roles here.

## 8. Follow Apply through the code

Start with [PromoAction](feature/checkout/presentation/src/main/kotlin/com/example/feature/checkout/presentation/contract/PromoAction.kt).

1. The UI sends `InputChanged` and then `ApplyClicked`.
2. [PromoReducer](feature/checkout/presentation/src/main/kotlin/com/example/feature/checkout/presentation/reducer/PromoReducer.kt) checks that the cart and input are valid, changes the promo status to `Redeeming`, and requests `RedeemPromo`.
3. [CheckoutEffectHandler](feature/checkout/presentation/src/main/kotlin/com/example/feature/checkout/presentation/effect/CheckoutEffectHandler.kt) calls [RedeemPromoUseCase](feature/checkout/domain/src/main/kotlin/com/example/feature/checkout/domain/usecase/RedeemPromoUseCase.kt).
4. The use case normalizes the code and asks the domain-owned repository interface to redeem it.
5. The data implementation translates DTOs and backend errors into domain values.
6. The handler emits `Redeemed` or `Rejected`.
7. The reducer commits the result; [PromoSectionProjector](feature/checkout/presentation/src/main/kotlin/com/example/feature/checkout/presentation/projector/PromoSectionProjector.kt) produces display state.

Point out that the ViewModel does not grow a promo-specific method to coordinate this flow. It still accepts an intent and dispatches it to the store.

## 9. Why the modules matter

```text
app ──► checkout:presentation ──► checkout:domain ──► core:domain
 │                ├──► core:mvi
 │                └──► core:ui ──► core:domain
 ├──► core:ui
 └──► checkout:data ──► checkout:domain
```

- **Presentation:** contracts, reducers, effects, projectors, composables, navigation.
- **Domain:** checkout models, repository interfaces, use cases, pricing and selection rules.
- **Data:** demo backend, local cart, platform connectivity, mapping, repository implementations.
- **App:** composition root that brings data and presentation together.
- **Core MVI:** generic state loop, independent of Android.
- **Build logic:** convention plugins so modules share build configuration.

Say:

> Dependency inversion is most valuable at the boundary. The domain owns the repository contract, and the data module implements it. Inside presentation, concrete small collaborators keep the wiring easy to follow.

The diagram shows compile dependencies. Hilt generates the application graph from those modules; do not imply that every implementation binding is physically declared in the app module.

## 10. Async correctness: the strongest technical section

### Pending writes

A quantity intent records `isUpdatingCart` before starting its effect. The use case returns the cart after the write, and `CartAction.Updated` acknowledges completion. The reducer also reconciles dependent state from that cart. Failed writes release the pending guard and emit a transient error.

Screen-originated cart writes are serialized by rejecting further quantity edits while one is pending. This is a simple deliberate policy for the POC. A production product could instead queue changes or coalesce a user's target quantity, but would need to define that policy explicitly.

### Payment consistency

The visible checkout state is retained during submission. A newer context is saved as `deferredContext`, while online status can still change. A failed payment reconciles that latest deferred context; success navigates away and discards it.

This is not a server-side price lock. Production payment still needs an authoritative cart/quote revision and durable attempt identity.

### Address edits

The context reducer compares selected address contents, not only its ID. An edit invalidates delivery approval and requests fresh validation and quotes. The same update does not need duplicate shipping requests when both cart and address change.

### Stale results

The store owns effect generations. Replacing a keyed effect increments its generation. Result actions are queued with the originating key and generation, and the loop rejects an outdated result before reducing it.

Cancellation avoids unnecessary work. The generation check protects state even if the older effect had already queued a result before it was cancelled.

### Price conflicts

A `PriceChanged` order outcome starts a product-price refresh. Payment and edits stay blocked while refresh runs. Once the cart has authoritative demo prices, ordinary context reconciliation recomputes dependent shipping and promo state. If the refresh fails, the dialog offers Retry; it does not pretend that prices were successfully updated.

### Connectivity

The Android adapter observes network-capability changes and requires validated internet access. Being attached to Wi-Fi alone is insufficient. This is a useful local signal, not a guarantee that a particular payment endpoint will respond; request errors still need handling.

## 11. How to present the tests

Open [CheckoutConsistencyTest](feature/checkout/presentation/src/test/kotlin/com/example/feature/checkout/presentation/CheckoutConsistencyTest.kt) and show **a delayed cart write cannot be overtaken by payment**.

Explain the sequence:

1. Start the real store, reducers, handler, use cases and projector over fakes.
2. Hold the cart write behind a deterministic gate.
3. Dispatch a quantity change.
4. Check that Pay is disabled.
5. Dispatch Pay anyway and verify that no order request is sent.
6. Release the gate and verify that the new quantity becomes payable after dependent work settles.

Then show the [StoreTest](core/mvi/src/test/kotlin/com/example/core/mvi/StoreTest.kt) that queues a replacement request before the previous result. The test verifies that only the newer generation updates state.

Say:

> The important tests assert observable behavior under controlled ordering. They do not depend on sleeping for a network request and hoping to hit a race.

The suite also covers money precision, pricing rules, repository mapping, scripted errors, reducers, projectors and ViewModel flows. Hand-written fakes are shared through the domain test-fixture module.

## 12. Likely questions and concise answers

### Why use reducers instead of placing this in the ViewModel?

A ViewModel remains a good lifecycle owner. The benefit here is separating decisions from execution: reducers can be tested with plain values, and individual section decisions remain discoverable as the screen grows.

### Is this a replacement for MVI?

No. The state/action/reducer/effect vocabulary is ordinary MVI. This example concentrates on how to organize a complex feature without putting all its coordination in one class.

### Does a small ViewModel mean complexity disappeared?

No. Coordination remains in explicit places, especially the context reducer and cross-section helpers. The goal is to make dependencies visible and testable, not to claim that checkout is inherently simple.

### Why a separate UI state?

Domain-shaped state supports decisions. UI models provide formatted amounts, localized text references, selection state and enabled controls. Projectors bridge those needs without putting formatting in business rules.

### Why not make every collaborator an interface?

Repository interfaces support module boundaries and replaceable external behavior. Concrete internal reducers and projectors are already small and pure; an interface for every one would add wiring without the same benefit here.

### Why are some use cases only one repository call?

They give the handler a consistent feature-level operation and a place for future domain policy. That has a cost in files and indirection. It is a choice to apply proportionally, rather than a claim that every function needs a use-case class.

### How would a new section be added?

Add its actions, route them in the top-level reducer, add its state/reducer, then its projector and composable. Add effects and use cases only if it performs I/O. Sealed action routing provides a compile-time reminder. If the section affects shipping or pricing, add those coordination rules and tests explicitly too.

### Does cancelling an effect undo a backend write?

No. Cancellation and stale-result rejection protect local orchestration; they cannot roll back an accepted external transaction. Production mutations need an explicit server contract and reconciliation policy.

### Can this process real payments?

No. The backend is a simulation. Real payments need secure backend integration, durable order/attempt state, authentication handling, authoritative revisions, and recovery after interruption. The demo's idempotency key is not a complete payment protocol.

### What survives process death?

This POC deliberately does not persist checkout state. Promo input, selections and the generated checkout ID can be lost. Production work would define restoration through persistent repositories and selected saved UI state, with durable payment-attempt recovery.

### Is the app fully production-ready?

No. It is a tested architecture demonstration. Real networking/database integration, payment authentication, process recovery, broader UI automation, accessibility validation, and performance profiling remain separate work.

### Why expose Reset demo?

It makes rehearsal repeatable and recovers from an empty cart without restarting the process. `restoreDemoCart` is intentionally a demo capability; a production checkout contract would replace or remove it.

## 13. Tradeoffs worth volunteering

- More small files improve ownership and focused testing, but require a good walkthrough and clear naming.
- A combined checkout context makes dependencies explicit, but a larger product may need more granular loading and refresh behavior.
- The generic store is small and understandable, but its channels are unbounded. High-volume producers would need an explicit pressure policy.
- The effect handler is currently one class. It can be split when its size and ownership justify that, while preserving the same effect/result contract.
- Production cart persistence and remote pricing require a concurrency/version contract. Replacing an adapter is not the same as completing all production behavior.
- Money is normalized to currency precision and rejects currency mismatches. This does not replace a backend's tax, rounding, discount allocation, or settlement policy.

## 14. If you only have five minutes

1. **30 seconds:** state the problem and the in-memory-backend boundary.
2. **90 seconds:** apply SAVE5, change quantity, and show address validation blocking Pay.
3. **90 seconds:** follow Apply through the reducer and effect handler; show the small ViewModel.
4. **45 seconds:** show the delayed-write regression test.
5. **45 seconds:** complete Visa payment and state the production boundaries.

Skip optional promo variants and the full module-by-module tour.

## 15. If the demo goes wrong

| Situation | Recovery |
|---|---|
| Cart or promo state is confusing | Scroll to the top and tap Reset demo |
| Pay is disabled | Read the explanation; wait for validation or restore internet access |
| Promo Apply is disabled | Check that the cart is non-empty and the field contains a code |
| A promo is already applied | Remove it before entering another code |
| BIG50 remains rejected after raising quantity | Tap Apply again |
| Bank-confirmation message appears | Dismiss it and choose Visa; the demo does not perform authentication |
| Network recovery is slow | Wait for Android validation; use screenshots and explain the tested flow |
| App was closed or restarted | Reopen it and use Reset demo; the backend and cart are in memory |
| Build is slow | Use the APK installed before the meeting; do not troubleshoot Gradle live |

Keep [the checkout screenshot](docs/images/checkout.png) available as a fallback. Screenshots illustrate observed UI; the tests provide separate logic evidence.

## 16. Your closing

> The value is that a complicated screen still has a predictable loop. Decisions are pure, side effects are explicit, and dependencies between sections have a visible home. The ViewModel stays focused on lifecycle, while tests can exercise ordering and failure cases without a device.
>
> This is a working proof of concept with deliberate boundaries. It gives us an understandable structure to extend, plus evidence for the behaviors we have implemented.

## 17. The final two-minute checklist

- [ ] App installed, emulator responsive, and internet validated.
- [ ] Reset demo; confirm the starting total is €44.00.
- [ ] Rehearse SAVE5 → €39.00 and mug quantity three → €51.00.
- [ ] Rehearse PO Box → Home, then Mastercard decline → Visa success.
- [ ] Keep the six source tabs and this guide open.
- [ ] Be ready to explain intents, effects, and events with distinct meanings.
- [ ] Say “216 passing tests” only for the checked version; never imply a coverage percentage.
- [ ] Say “simulated payment,” and be clear about process-death and production boundaries.
- [ ] Slow down after each interaction so the audience can see the transition.

## Reference links

- [Project README](README.md)
- [Original design spec](docs/specs/2026-10-12-checkout-mvi-poc-design.md)
- [Shared payment guard](feature/checkout/presentation/src/main/kotlin/com/example/feature/checkout/presentation/contract/CheckoutSelectors.kt)
- [Context coordination](feature/checkout/presentation/src/main/kotlin/com/example/feature/checkout/presentation/reducer/ContextReducer.kt)
- [Order transitions](feature/checkout/presentation/src/main/kotlin/com/example/feature/checkout/presentation/reducer/OrderReducer.kt)
- [Money value object](core/domain/src/main/kotlin/com/example/core/domain/Money.kt)
- [Android Scaffold documentation](https://developer.android.com/develop/ui/compose/components/scaffold)
- [Android network-state documentation](https://developer.android.com/develop/connectivity/network-ops/reading-network-state)
