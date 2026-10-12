package com.example.feature.checkout.data.error

import com.example.core.domain.Outcome
import com.example.feature.checkout.data.remote.CheckoutApiException
import com.example.feature.checkout.domain.error.CheckoutError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class SafeCallTest {

    @Test
    fun `a successful call is a success`() = runTest {
        assertEquals(Outcome.Success(42), safeCall { 42 })
    }

    @Test
    fun `an IO failure is a network error`() = runTest {
        assertEquals(Outcome.Failure(CheckoutError.Network), safeCall<Int> { throw IOException("offline") })
    }

    @Test
    fun `status codes without an endpoint meaning map to common errors`() = runTest {
        assertEquals(Outcome.Failure(CheckoutError.Unauthorized), safeCall<Int> { throw CheckoutApiException(401) })
        assertEquals(Outcome.Failure(CheckoutError.Unauthorized), safeCall<Int> { throw CheckoutApiException(403) })
        assertEquals(Outcome.Failure(CheckoutError.NotFound), safeCall<Int> { throw CheckoutApiException(404) })
        assertEquals(Outcome.Failure(CheckoutError.PriceChanged), safeCall<Int> { throw CheckoutApiException(409) })
        assertEquals(Outcome.Failure(CheckoutError.Unexpected("HTTP 500")), safeCall<Int> { throw CheckoutApiException(500) })
    }

    @Test
    fun `an endpoint meaning wins over the common one`() = runTest {
        val outcome = safeCall<Int>(mapApiError = { CheckoutError.Promo.Expired }) { throw CheckoutApiException(410) }

        assertEquals(Outcome.Failure(CheckoutError.Promo.Expired), outcome)
    }

    @Test
    fun `an unexpected exception is a failure that names its type and never its message`() = runTest {
        val outcome = safeCall<Int> { throw IllegalArgumentException("bad") }

        assertEquals(Outcome.Failure(CheckoutError.Unexpected("IllegalArgumentException")), outcome)
    }

    @Test
    fun `cancellation is never swallowed`() {
        assertThrows(CancellationException::class.java) {
            runBlocking { safeCall<Int> { throw CancellationException("stopped") } }
        }
    }
}
