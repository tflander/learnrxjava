package learnrxjava.todd

data class AuthResponse(
        val success: Boolean,
        val message: String,
        val isOnLine: Boolean
)

data class AuthRequest(
        val serverName: String,
        val delay: Long,
        val isCorrect: Boolean,
        val isError: Boolean = false
)
