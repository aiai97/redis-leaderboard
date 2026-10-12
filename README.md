# Redis Leaderboard

A leaderboard example built with **Spring Boot, Jedis, and Redis Sorted Sets (ZSETs)**. Each player's score is stored as the ZSET score, and the player ID is stored as the member. `ZREVRANK` returns a player's rank in descending score order.

## Getting Started

Requirements: Docker Compose, JDK 17+, and Maven 3.9+.

```bash
docker compose up -d
mvn spring-boot:run
```

Redis is mapped to port `6379` on the host, and the application listens on port `8080`. Override the default configuration with `REDIS_HOST`, `REDIS_PORT`, and `LEADERBOARD_KEY`.

When the application starts, it adds sample players (`alice`, `bob`, and `charlie`) if the configured leaderboard is empty. Existing leaderboard data is left unchanged.

## API

Set an absolute score:

```bash
curl -X PUT http://localhost:8080/api/leaderboard/players/alice/score \
  -H 'Content-Type: application/json' -d '{"score":1200}'
```

Increment a score (useful for game events):

```bash
curl -X POST http://localhost:8080/api/leaderboard/players/alice/increment \
  -H 'Content-Type: application/json' -d '{"delta":50}'
```

Get a leaderboard page (`page` is zero-based; `size` has a maximum of 100):

```bash
curl 'http://localhost:8080/api/leaderboard?page=0&size=20'
```

Get a player's score and rank:

```bash
curl http://localhost:8080/api/leaderboard/players/alice
```

Remove a player:

```bash
curl -X DELETE http://localhost:8080/api/leaderboard/players/alice
```

Example response:

```json
{"playerId":"alice","score":1250.0,"rank":1}
```

## Adding Sample Data Manually

You can also add leaderboard entries directly with Redis `ZADD`:

```bash
docker compose exec redis redis-cli ZADD leaderboard:global 1200 alice 980 bob 1500 charlie
```

Each score is followed by a player ID. Use the same key configured by `LEADERBOARD_KEY` (the default is `leaderboard:global`) so the application can read these entries. `ZADD` adds new players or updates the scores of existing players.

## Code Examples and Design Rationale for Common Use Cases

This project includes runnable examples of a leaderboard, counter, fixed-window rate limiter, distributed lock, batch job queue, Cache-Aside cache, and temporary sessions. The code is organized into Controllers (HTTP endpoints), Services (business decisions), and Stores (Jedis/Redis operations). When choosing a Redis data structure, consider whether Redis can perform the critical operation atomically under concurrent access.

### Leaderboard: Sorted Set

The leaderboard is implemented by `LeaderboardService` and `JedisLeaderboardStore`. A ZSET maps player IDs to scores and Redis maintains the ordering as scores change. `ZINCRBY` atomically increments a score, while `ZREVRANK` and `ZREVRANGE` retrieve a player's rank and leaderboard entries.

```bash
curl -X POST http://localhost:8080/api/leaderboard/players/alice/increment \
  -H 'Content-Type: application/json' -d '{"delta":50}'
curl 'http://localhost:8080/api/leaderboard?page=0&size=20'
```

### Counter: String + INCR

Counters such as page views are a good fit for `INCR`: the Redis command is atomic, so there is no read-modify-write sequence where concurrent requests can overwrite each other's updates. Counter keys use the `counter:` prefix to separate this use case from other data.

```bash
curl -X POST http://localhost:8080/api/counters/article:42:views/increment
curl http://localhost:8080/api/counters/article:42:views
```

Reading a counter that does not exist returns `0`. This example does not set an expiration, which is appropriate for counters that accumulate over time; temporary counters need an explicit cleanup or TTL policy. For authoritative values that must not be lost, such as financial or inventory data, use a database as the source of truth and design persistence or reconciliation accordingly.

### API Rate Limiting: Counter + Lua Fixed Window

The example counts requests by `clientId` and allows 10 requests every 60 seconds by default. A Lua script atomically increments the counter, sets its TTL on the first request, and reads the remaining window time. Splitting these steps across commands could leave the count and expiration out of sync under concurrency. Requests over the limit receive HTTP 429 and a `Retry-After` header.

```bash
curl -i -X POST http://localhost:8080/api/demo/rate-limit/client-1
```

Set `RATE_LIMIT_REQUESTS` and `RATE_LIMIT_WINDOW_SECONDS` environment variables to change the limit and window. A fixed window is simple to implement, but requests around a window boundary may briefly reach nearly twice the configured limit. A token bucket provides smoother traffic shaping. In production, identify clients using authenticated identities rather than IDs supplied at will by callers.

### Distributed Lock: SET NX PX + Token-Checked Release

`SET lock-key token NX PX lease` creates a lock with an expiration only if the key does not already exist. Each acquisition uses a unique token. On release, Lua compares the token before deleting the key, preventing an old owner from deleting a new lock after its own lease expires.

```bash
curl -i -X POST http://localhost:8080/api/locks/daily-report
# Copy the token from the response.
curl -i -X DELETE http://localhost:8080/api/locks/daily-report \
  -H 'Content-Type: application/json' -d '{"token":"token-from-response"}'
```

The default lease is 10 seconds and can be changed with `LOCK_LEASE_MILLIS`. The API returns HTTP 409 if the lock is already held or the token does not match. Since a lock is released automatically when its lease expires, protected work must finish within the lease or implement renewal. Systems requiring stronger consistency should also use fencing tokens and have downstream resources reject stale owners. These endpoints demonstrate the locking primitive; production systems should restrict access and acquire the lock as part of the same service flow as the protected operation.

### Batch Jobs: Redis Streams Consumer Groups

This pattern is suitable for asynchronous reports, batch calculations, or background jobs. An HTTP request atomically writes a batch of numeric tasks to a Stream and returns a batch ID. A consumer group processes the tasks asynchronously, while the status endpoint returns the total and processed counts, result, and status. Each Stream entry represents one task, and the consumer group distributes entries among workers.

```bash
curl -X POST http://localhost:8080/api/batch-jobs \
  -H 'Content-Type: application/json' -d '{"values":[2,3,5,8]}'
curl http://localhost:8080/api/batch-jobs/<batchId-from-response>
```

The result update, deduplication, and `XACK` run in one Lua script. If a process crashes after receiving a message, the message remains pending and another consumer can claim it after `BATCH_CLAIM_IDLE_MILLIS`. This provides at-least-once delivery, so real business side effects still need idempotency keys or deduplication. The example accepts up to 1,000 items per batch and keeps completed job status for 24 hours. Production batch processing should also plan for retries, dead-letter handling, cancellation, rate limiting, and durable long-term result storage.

### Hot Data: Cache-Aside

`GET /api/demo/products/{id}` checks Redis first. On a cache miss, it reads from the sample product catalog and populates the cache with a TTL. Updates write to the data source first and then delete the cache entry, which is rebuilt on the next read.

```bash
curl http://localhost:8080/api/demo/products/42
curl -X PUT http://localhost:8080/api/demo/products/42 \
  -H 'Content-Type: application/json' \
  -d '{"name":"Keyboard Pro","priceInCents":39900}'
```

Configure the cache TTL with `PRODUCT_CACHE_TTL_SECONDS`. The data source here is an in-memory catalog for demonstration purposes; sample data is restored after a restart, so production systems should replace it with a database repository. Cache invalidation can briefly expose stale data during concurrent writes. Strong consistency requires versioning or another coordination mechanism. Briefly caching missing products can also help mitigate cache penetration.

### Temporary Sessions: String with TTL

When creating a session, the example uses a random token as the Redis key, maps it to a user ID, and sets an expiration. Reads atomically retrieve the session and its remaining TTL; logout deletes the token.

```bash
curl -X POST http://localhost:8080/api/sessions \
  -H 'Content-Type: application/json' -d '{"userId":"user-42"}'
curl http://localhost:8080/api/sessions/<token-from-response>
curl -X DELETE http://localhost:8080/api/sessions/<token-from-response>
```

The default TTL is 1,800 seconds and can be changed with `SESSION_TTL_SECONDS`. This demo API can create a session for any syntactically valid user ID, so it must not be used as a production login endpoint. Production systems must authenticate users first, use secure and unpredictable credentials, and avoid exposing tokens in URLs, access logs, or error messages.

These examples demonstrate Redis concurrency primitives and should not be treated as replacements for database transactions, reliable message brokers, or durable business records.

## Testing

```bash
mvn test
docker compose down
```

The tests cover setting scores, atomic score increments, ranking across pages, player not found handling, counters, rate limits, locks, batch job processing behavior, and session expiration behavior.

## Logs

After starting the application, use another terminal to submit score updates and leaderboard queries. The application console logs business events, for example:

```text
leaderboard.score_set playerId=alice score=1200.0 rank=1
leaderboard.score_incremented playerId=alice delta=50.0 score=1250.0 rank=1
leaderboard.page_read page=0 size=20 returned=1 total=1
```

View the Redis container's startup and runtime logs:

```bash
docker compose logs -f redis
```

Inspect the ZSET in Redis directly (scores are ordered from highest to lowest):

```bash
docker compose exec redis redis-cli ZREVRANGE leaderboard:global 0 -1 WITHSCORES
```
