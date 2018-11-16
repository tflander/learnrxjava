package learnrxjava.todd


import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import rx.Observable
import rx.schedulers.Schedulers
import java.util.*
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.function.Function

class FluentGenericAuthTests {

    @Test
    fun `gets token from right server and doesn't wait for slow server`() {

        val startTime = Date().time
        val theCorrectServerIsUpAndHaveSlowServer = listOf<AuthRequest>(
                AuthRequest("Slow and Wrong Server A", 6000, false),
                AuthRequest("Medium Fast and Correct Server B", 300, true),
                AuthRequest("Fast and Wrong Server C", 30, false)
        )

        val authResponse = FluentGenericAuthSpike.multiSubScribe(theCorrectServerIsUpAndHaveSlowServer)
        assertEquals("Medium Fast and Correct Server B", authResponse.message)
        val elapsed = Date().time - startTime
        Assert.assertTrue("expected < 1500ms, took" + elapsed, elapsed < 1500)
    }

    @Test
    fun `fails when all servers are up and the token doesn't match any of them`() {

        val startTime = Date().time
        val tokenBadForAllServers = listOf<AuthRequest>(
                AuthRequest("Slow and Wrong Server A", 3000, false),
                AuthRequest("Medium Fast and Wrong Server B", 300, false),
                AuthRequest("Fast and Wrong Server C", 30, false)
        )

        val authResponse = FluentGenericAuthSpike.multiSubScribe(tokenBadForAllServers)
        assertEquals("Token invalid for all Auth Servers", authResponse.message)
        Assert.assertTrue(Date().time - startTime > 3000)
    }

    @Test
    fun `fails with special message when a server is down and the token doesn't match any of the servers that are up`() {

        val theCorrectServerIsDown = listOf<AuthRequest>(
                AuthRequest("fast and Down Server A", 60, false, isError = true),
                AuthRequest("Medium Fast and Wrong Server B", 300, false),
                AuthRequest("Fast and Wrong Server C", 30, false)
        )

        val authResponse = FluentGenericAuthSpike.multiSubScribe(theCorrectServerIsDown)
        assertEquals("Token invalid, but 1 server(s) were down", authResponse.message)
    }

    @Test
    fun `returns token when servers are down, but the one with the token passed is up`() {

        val startTime = Date().time
        val incorrectServersAreDown = listOf<AuthRequest>(
                AuthRequest("fast and Down Server A", 60, false, isError = true),
                AuthRequest("Medium Fast and Correct Server B", 300, true),
                AuthRequest("Fast and Wrong Server C", 30, false, isError = true),
                AuthRequest("Slow and Wrong Server D", 3000, false, isError = true)
                )

        val authResponse = FluentGenericAuthSpike.multiSubScribe(incorrectServersAreDown)
        assertEquals("Medium Fast and Correct Server B", authResponse.message)
        val elapsed = Date().time - startTime
        Assert.assertTrue("expected < 1500, took " + elapsed, elapsed < 1500)
    }

}

object FluentGenericAuthSpike {

    fun multiSubScribe(mockAuthRequests: List<AuthRequest>): AuthResponse {

        val processAuthRequest: Function<AuthRequest, AuthResponse> = Function { authRequest ->
            println("calling " + authRequest)
            Thread.sleep(authRequest.delay)
            if(authRequest.isError) {
                throw IllegalStateException("whoops")
            }
            println("${authRequest} returned auth.")
            AuthResponse(authRequest.isCorrect, authRequest.serverName)
        }

        return FirstSuccessfulResponseFlow<AuthRequest, AuthResponse>()
                .processParallelRequests(mockAuthRequests)
                .abortWhenFirstRequestPasses(Predicate { r -> r.success})
                .executeForEachRequestInSeparateThread(processAuthRequest)
                .whenNoSuccessAndNoErrorsThenRespond(Supplier {
                    AuthResponse(false, "Token invalid for all Auth Servers")
                })
                .whenNoSucessAndSomeErrorsThenRespond(Function { errorCount ->
                    AuthResponse(false, "Token invalid, but " + errorCount + " server(s) were down")
                })
                .execute()
    }

}

class FirstSuccessfulResponseFlow<T, R> {

    fun processParallelRequests(requests: List<T>): FirstSuccessfulResponseFlowRunner<T, R> {
        return FirstSuccessfulResponseFlowRunner(requests)
    }
}

class FirstSuccessfulResponseFlowRunner<T, R>(val requests: List<T>) {

    lateinit var isSuccess: Predicate<R>
    lateinit var processFuncion: Function<T, R>
    lateinit var responseForNoSuccessAndNoError: Supplier<R>
    lateinit var responseForNoSucessAndSomeErrors: Function<Int, R>

    fun abortWhenFirstRequestPasses(isSuccess: Predicate<R>): FirstSuccessfulResponseFlowRunner<T, R> {
        this.isSuccess = isSuccess
        return this
    }

    fun executeForEachRequestInSeparateThread(processFuncion: Function<T, R>): FirstSuccessfulResponseFlowRunner<T, R> {
        this.processFuncion = processFuncion
        return this
    }

    fun whenNoSuccessAndNoErrorsThenRespond(responseForNoSuccessAndNoError: Supplier<R>): FirstSuccessfulResponseFlowRunner<T, R> {
        this.responseForNoSuccessAndNoError = responseForNoSuccessAndNoError
        return this
    }

    fun whenNoSucessAndSomeErrorsThenRespond(responseForNoSucessAndSomeErrors: Function<Int, R>): FirstSuccessfulResponseFlowRunner<T, R> {
        this.responseForNoSucessAndSomeErrors = responseForNoSucessAndSomeErrors
        return this
    }

    fun execute(): R {
        val bar = launchAuthRequestsInSeparateThreads(requests)
                .filter { response ->
                    !response.isSuccess ||isSuccess.test(response.response!!)
                }
                .toBlocking()

        var downCount = 0
        for (response in bar.toIterable()) {
            if (!response.isSuccess) {
                ++downCount
            } else {
                if (isSuccess.test(response.response!!)) {
                    return response.response
                }
            }
        }

        if (downCount == 0) {
            return responseForNoSuccessAndNoError.get()
        } else {
            return responseForNoSucessAndSomeErrors.apply(downCount)
        }

    }
    private fun launchAuthRequestsInSeparateThreads(authRequests: List<T>): Observable<ResponseWrapper<R>> {
        return Observable.merge(
                authRequests.map { authRequest ->
                    createAuthObservableWithDelay(authRequest).subscribeOn(Schedulers.io())
                })

    }

    private fun createAuthObservableWithDelay(authRequest: T): Observable<ResponseWrapper<R>> {
        return Observable.create { subscriber ->
            try {
                val response = processFuncion.apply(authRequest)
                subscriber.onNext(ResponseWrapper(true, response))
            } catch (ignoreAbandoningSlowResponses: InterruptedException) {
                //  Likely because we found the correct response already
            } catch (error: Exception) {
                subscriber.onNext(ResponseWrapper(false, null))
            }
            subscriber.onCompleted()

        }
    }

}

