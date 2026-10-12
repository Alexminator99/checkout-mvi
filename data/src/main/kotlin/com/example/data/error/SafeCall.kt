package com.example.data.error

import com.example.domain.error.DomainError
import com.example.domain.error.Outcome
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

/**
 * The single place where exceptions become [DomainError]s.
 *
 * @param mapHttp endpoint-specific translation of HTTP failures (e.g. 404 on
 *                /promos/redeem means "invalid promo", not "not found").
 *                Falls back to [defaultHttpError].
 */
internal suspend inline fun <T> safeCall(
    noinline mapHttp: (HttpException) -> DomainError? = { null },
    crossinline block: suspend () -> T,
): Outcome<T> = try {
    Outcome.Success(block())
} catch (e: CancellationException) {
    throw e                                  // never swallow cancellation
} catch (e: HttpException) {
    Outcome.Failure(mapHttp(e) ?: defaultHttpError(e))
} catch (e: IOException) {
    Outcome.Failure(DomainError.Network)
}

internal fun defaultHttpError(e: HttpException): DomainError = when (e.code()) {
    401, 403 -> DomainError.Unauthorized
    404 -> DomainError.NotFound
    409 -> DomainError.PriceChanged
    else -> DomainError.Unexpected("HTTP ${e.code()}")
}
