# Managing Complexity with RX Java

## Agenda

    - Problem Statement
        - need to test multiple servers for auth and stop on first success, ignoring slow wrong servers
        - need to report if servers are down and auth is invalid for the ones that are up
        - desire to separate threading and data flow from business logic
        - desire to only test business logic and re-use tested data flow
    - Business logic:
        - get configured auth servers to call with a given client token
        - pass through auth response on success
        - construct special message on failure when all servers are up
        - construct special message on failure when some servers down
    - First pass -- AsyncAuthSpike.kt - MVP without code separation
    - Second pass -- GenericAsyncAuthSpike.kt - Separate code with cryptic public interface
    - Last pass -- FluentGenericAsyncAuthSpike.kt - Refactor cryptic public interface into fluent style

