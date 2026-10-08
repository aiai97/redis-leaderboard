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

## Testing

```bash
mvn test
docker compose down
```

The tests cover setting scores, atomic score increments, ranking across pages, players not found, and HTTP response serialization.

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
