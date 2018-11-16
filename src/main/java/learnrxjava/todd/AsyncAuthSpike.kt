package learnrxjava.todd


import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import rx.Observable
import rx.schedulers.Schedulers
import java.util.*


class AuthTests {

    @Test
    fun `gets token from right server and doesn't wait for slow server`() {

        val startTime = Date().time
        val theCorrectServerIsUpAndHaveSlowServer = listOf<AuthRequest>(
                AuthRequest("Slow and Wrong Server A", 6000, false),
                AuthRequest("Medium Fast and Correct Server B", 300, true),
                AuthRequest("Fast and Wrong Server C", 30, false)
        )

        val authResponse = AsyncAuthSpike.multiSubScribe(theCorrectServerIsUpAndHaveSlowServer)
        assertEquals("Medium Fast and Correct Server B", authResponse.message)
        Assert.assertTrue(Date().time - startTime < 600)
    }

    @Test
    fun `fails when all servers are up and the token doesn't match any of them`() {

        val startTime = Date().time
        val tokenBadForAllServers = listOf<AuthRequest>(
                AuthRequest("Slow and Wrong Server A", 3000, false),
                AuthRequest("Medium Fast and Wrong Server B", 300, false),
                AuthRequest("Fast and Wrong Server C", 30, false)
        )

        val authResponse = AsyncAuthSpike.multiSubScribe(tokenBadForAllServers)
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

        val authResponse = AsyncAuthSpike.multiSubScribe(theCorrectServerIsDown)
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

        val authResponse = AsyncAuthSpike.multiSubScribe(incorrectServersAreDown)
        assertEquals("Medium Fast and Correct Server B", authResponse.message)
        Assert.assertTrue(Date().time - startTime < 600)
    }

}

object AsyncAuthSpike {

    fun multiSubScribe(mockAuthRequests: List<AuthRequest>): DeprecatedAuthResponse {

        val blockingRequests = launchAuthRequestsInSeparateThreads(mockAuthRequests)
                .filter { authResponse -> (authResponse.success || !authResponse.isOnLine) }
                .toBlocking()

        var serverDownCount = 0
        for (response in blockingRequests.toIterable()) {
            println(": " + response.message)
            if (!response.isOnLine) {
                ++serverDownCount
            } else {
                if (response.success) {
                    return response
                }
            }
        }

        if (serverDownCount == 0) {
            return DeprecatedAuthResponse(false, "Token invalid for all Auth Servers", isOnLine = true)
        } else {
            return DeprecatedAuthResponse(false, "Token invalid, but " + serverDownCount + " server(s) were down", isOnLine = false)
        }

    }

    private fun launchAuthRequestsInSeparateThreads(authRequests: List<AuthRequest>): Observable<DeprecatedAuthResponse> {
        return Observable.merge(
                authRequests.map { authRequest ->
                    observeAuthRequestProcessing(authRequest).subscribeOn(Schedulers.io())
                })

    }

    private fun observeAuthRequestProcessing(authRequest: AuthRequest): Observable<DeprecatedAuthResponse> {
        return Observable.create { subscriber ->
            try {
                println("calling auth against ${authRequest.serverName}...")
                Thread.sleep(authRequest.delay.toLong())
                println("${authRequest.serverName} returned auth.")
            } catch (ignoreAbandoningSlowResponses: InterruptedException) {
                //  Likely because we found the correct auth server already
            }

            subscriber.onNext(DeprecatedAuthResponse(success = authRequest.isCorrect, message = authRequest.serverName, isOnLine = !authRequest.isError))
            subscriber.onCompleted()
        }
    }

}
