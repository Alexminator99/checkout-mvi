package com.example.domain.error

/**
 * Errors the business understands. The data layer maps exceptions (HTTP codes,
 * IOException, SQLite errors) into these, so nothing above the data layer ever
 * sees a Throwable, and screen state stays comparable and serializable.
 */
sealed interface DomainError {
    data object Network : DomainError
    data object Unauthorized : DomainError
    data object NotFound : DomainError

    sealed interface Promo : DomainError {
        data object Invalid : Promo
        data object Expired : Promo
        data object MinimumNotReached : Promo
    }

    sealed interface Payment : DomainError {
        data object Declined : Payment
        data object RequiresAction : Payment
    }

    data object OutOfStock : DomainError
    data object PriceChanged : DomainError
    data class Unexpected(val diagnostic: String?) : DomainError
}

/** Result type used across use cases. Use cases never throw for expected failures. */
sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: DomainError) : Outcome<Nothing>
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value))
    is Outcome.Failure -> this
}

inline fun <T, R> Outcome<T>.fold(onSuccess: (T) -> R, onFailure: (DomainError) -> R): R = when (this) {
    is Outcome.Success -> onSuccess(value)
    is Outcome.Failure -> onFailure(error)
}
