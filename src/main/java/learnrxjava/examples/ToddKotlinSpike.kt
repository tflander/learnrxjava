package learnrxjava.examples


import rx.Observable
import rx.schedulers.Schedulers

data class AuthResponse(
        val success: Boolean,
        val serverName: String
)

data class AuthRequest(
        val serverName: String,
        val delay: Int,
        val isCorrect: Boolean,
        val isError: Boolean = false
)

object ToddKotlinSpike {

    @Throws(InterruptedException::class)
    @JvmStatic
    fun main(args: Array<String>) {

        multiSubScribe()

    }

    @Throws(InterruptedException::class)
    private fun multiSubScribe() {

        val mockAuthRequests = listOf<AuthRequest>(
                AuthRequest("Slow and Wrong Server A", 6000, false),
                AuthRequest("Medium Fast and Correct Server B", 300, true),
                AuthRequest("Fast and Wrong Server C", 30, false)
        )

        var authOrNot = launchAuthRequestsInSeparateThreads(mockAuthRequests)
                .filter { authResponse -> authResponse.success }
                .take(1)
                .toBlocking()
                .firstOrDefault(AuthResponse(false, "Token invalid for all Auth Servers"))

        if (authOrNot.success) {
            println("Token valid for server " + authOrNot.serverName)
        } else {
            println("Error: Token invalid")
        }
    }

    private fun launchAuthRequestsInSeparateThreads(authRequests: List<AuthRequest>): Observable<AuthResponse> {
        return Observable.merge(
                authRequests.map { authRequest ->
                    createAuthObservableWithDelay(authRequest).subscribeOn(Schedulers.io())
                })

    }

    private fun createAuthObservableWithDelay(authRequest: AuthRequest): Observable<AuthResponse> {
        return Observable.create { subscriber ->
            try {
                println("calling auth against ${authRequest.serverName}...")
                Thread.sleep(authRequest.delay.toLong())
                println("${authRequest.serverName} returned auth.")
            } catch (e: InterruptedException) {
                println("aborting auth from ${authRequest.serverName}")
            }

            subscriber.onNext(AuthResponse(success = authRequest.isCorrect, serverName = authRequest.serverName))
            subscriber.onCompleted()
        }
    }

}
