# Streaming JSON with Vert.x

This standalone example uses the [Hibernate Search JSON module](../../README.md)
with Vert.x Core. It receives UTF-8 JSON incrementally and streams the completed
Jackson tree back, with one output write in flight. It is an application-level HTTP echo
server, independent of Hibernate Search's Elasticsearch transports.

This is a contributor example: the echo server directly uses Search's internal
Jackson parser and body factories. Those `internal.spi` types are not external
extension points. A custom Search transport instead receives a library-neutral
`JsonBody` and `JsonResponseConsumerFactory<T>`, and passes the completed `T` back
without importing Jackson or implementing parsing.

The complete code is in
[`JsonEchoServer.java`](src/main/java/org/hibernate/search/util/json/example/JsonEchoServer.java).
The example POM supplies Vert.x; the JSON module itself has no Vert.x dependency.
It also includes an outgoing AWS-signed client using Search's asynchronous request
preparation SPI and AWS integration. Those dependencies belong to the standalone
example/backend integration; the JSON module gains no Vert.x or AWS dependency.

## Run it

Use JDK 17 or newer and run these commands from the repository root. Install the
branch's backend AWS artifact and its parents/dependencies before building the standalone
example, which is intentionally outside the main Maven reactor. A nightly CI
matrix entry compiles and tests this example with `verify`, using the reactor
artifacts installed by the initial build:

```shell
./mvnw clean install -pl backend/elasticsearch-aws -am -DskipTests -DskipITs -Dno-build-cache
./mvnw -f util/json/examples/vertx/pom.xml compile exec:java
```

The server listens on `127.0.0.1:8080`. Set a different port with
`-Dexec.args=8081`. Stop the process with Ctrl+C. The POM pins Vert.x 5.1.8;
the example also uses APIs available in Vert.x 4.5.

In another terminal:

```shell
curl -i http://127.0.0.1:8080/ \
  -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary '{"message":"Hello 🌍","values":[1,1.00,6.022e23]}'
# Malformed, empty or trailing input returns HTTP 400:
curl -i http://127.0.0.1:8080/ --data-binary '{"unfinished":'
curl -i http://127.0.0.1:8080/ --data-binary '{} {}'
```

The echo preserves supported numeric values. Numeric spelling and original whitespace can change.
JSON null is a valid root; empty input is rejected. The server expects uncompressed
UTF-8 bodies and applies a 1 MiB parser input cap. It does not implement content-type
negotiation, authentication, compression or Elasticsearch retry/error semantics.

To exercise fragmented uploads and multiple output buffers with a slow reader:

```shell
scratch_dir=$(mktemp -d)
jq -n '{message: ("🌍" * 30000), values: [range(1000)]}' > "$scratch_dir/request.json"
curl --http1.1 --limit-rate 32k \
  -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary @"$scratch_dir/request.json" \
  http://127.0.0.1:8080/ > "$scratch_dir/response.json"
diff <(jq -S . "$scratch_dir/request.json") <(jq -S . "$scratch_dir/response.json")
rm -r -- "$scratch_dir"
```

The last command uses Bash process substitution and `jq`. Requests larger than
1 MiB are rejected. The response does not need to fit in a single output buffer.

## Reading a request or client response

Create a shareable `JsonParsing` factory and one `JsonParserSession` per message.
`Exchange.start()` installs `handler`, `endHandler` and exception/disconnect
handlers before consuming request bytes. Do not install a body-collecting handler
before this handler.

`Exchange.feed()` copies at most 8192 bytes at a time from a Vert.x `Buffer` into
a per-request scratch array using `getBytes(start, end, destination, offset)`.
It loops on `parser.feed(ByteBuffer)` until the offered region is consumed.
This avoids a whole-body byte array and uses public Vert.x APIs without relying
on Netty internals. The same handler pattern applies to an `HttpClientResponse`.
For a client, select handling of status codes, absent bodies, charset and
compression before feeding or completing the parser.

The parser releases caller storage before `feed` returns, so the next slice can
reuse the scratch array. EOF is reported through `endOfInput()` in the end handler;
only this call returns a complete root. UTF-8 characters and tokens split across
network buffers are handled by the session.

Parse failures close the session, return HTTP 400 and discard subsequent request
chunks without retaining them. Disconnects and stream failures close unfinished
parser/cursor state and reset the response. Terminal guards prevent later
callbacks from reusing released state. A production server should additionally
apply its own connection, timeout and request admission policies.

## Writing a response

The output path creates `JsonBodies.of(root)` and opens one cursor. The small-body
probe determines whether `Content-Length` is known; otherwise the response enables
chunked HTTP/1.1 output. HTTP framing belongs to Vert.x, not the JSON cursor.

`Exchange.pump()` fills an 8192-byte `ByteBuffer`, then copies just the written
region into a fresh Vert.x `Buffer` with `appendBytes`. This copy transfers ownership:
the reusable scratch array and the pending asynchronous write cannot alias.

The next chunk is produced only after `response.write(buffer)` succeeds. A new
context turn schedules it, avoiding recursive callbacks and yielding between
output chunks. This serial write strategy needs neither an unbounded producer
queue nor a `drainHandler`: there is at most one application write outstanding.
A producer allowing multiple queued writes must instead stop when
`writeQueueFull()` reports fullness and resume from `drainHandler`.

Cursor completion closes traversal state and calls `response.end()`.
A failed write or peer disconnect closes the cursor and resets the response;
pending callbacks observe the terminal flag. A new response or retry would open
another cursor, never restart a consumed one. The tree must stay unchanged through
all output attempts.

## Execution and memory

Each exchange's callbacks run serially on its Vert.x context; parser sessions and
cursors are never shared between requests or used concurrently. Parsing and cursor
writes execute synchronously in those callbacks. This example is appropriate for
illustrating ownership and flow control; 8 KiB buffers do not guarantee bounded
CPU time on an event loop. Completing a large token or building its tree may still
be expensive.

The full tree and scalar values remain materialized. Scratch copies and pending
output are bounded; this is not a constant-memory transformation of an arbitrarily
large document, and output starts after input EOF. For stricter latency requirements,
choose an explicit worker policy: pause input, transfer owned slices, process one
session serially, and resume on its Vert.x context after each accepted unit of work.
Coordinate cancellation with worker ownership before closing state.

See the official Vert.x documentation for the transport contracts:

- [ReadStream](https://vertx.io/docs/apidocs/io/vertx/core/streams/ReadStream.html): handlers and input demand.
- [WriteStream](https://vertx.io/docs/apidocs/io/vertx/core/streams/WriteStream.html): write completion and queue flow control.
- [Buffer](https://vertx.io/docs/apidocs/io/vertx/core/buffer/Buffer.html): bounded byte transfers.
- [HttpServerResponse](https://vertx.io/docs/apidocs/io/vertx/core/http/HttpServerResponse.html): framing, completion and disconnect handlers.

## Signing outgoing requests with the Search SPI

[`SignedJsonClient`](src/main/java/org/hibernate/search/util/json/example/SignedJsonClient.java)
now consumes production Search APIs:

- `org.hibernate.search.backend.elasticsearch.client.common.authentication.spi.ElasticsearchRequestPreparation`
  describes an immutable destination attempt, a headers-only result and an
  integration-owned execution/cancellation capability.
- `org.hibernate.search.backend.elasticsearch.aws.spi.ElasticsearchAwsRequestSigning`
  resolves credentials and calls the synchronous AWS signer through that capability.
  It owns its bounded signing streams, creates no executor, and leaves directly
  supplied credentials owned by the caller.
- `ElasticsearchRequestPreparations.create(beanResolver, propertySource)` discovers
  configured asynchronous providers, composes them in order and returns a
  `BeanHolder` retained until client shutdown. The AWS role reads the existing
  `aws.signing.enabled`, region, credentials and distribution settings. Its created
  credentials are owned by that holder. Existing REST clients continue to discover
  the synchronous interceptor role and retain their existing interception placement.

The example-local `RequestPreparation` and `AwsRequestSigning` prototypes have been
removed. No `JsonBody` or cursor API change was needed.

```text
event loop: select destination → create Vert.x request → apply user configuration/framing
    → snapshot effective authority, method, encoded URI, complete headers and stable body
    → application workers: resolve credentials → sign through an independent bounded stream
    → original event loop: replace headers → transmit through a fresh cursor
```

With application-owned `vertx`, `httpClient` and `credentialsProvider`:

```java
var signing = new ElasticsearchAwsRequestSigning(
        credentialsProvider, "us-east-1", "es", Clock.systemUTC() );
var client = new SignedJsonClient( vertx, httpClient, signing );
vertx.runOnContext( ignored -> {
    JsonBody body = JsonBodies.of( new JsonParsing().parse( "{\"query\":{\"match_all\":{}}}" ) );
    var attempt = new ElasticsearchRequestPreparation.Request(
            URI.create( "https://your-endpoint.example/index/_search" ), body );
    SignedJsonClient.Call call = client.send( attempt, 10_000 );
    call.result().onSuccess( status -> System.out.println( "HTTP " + status ) )
            .onFailure( Throwable::printStackTrace );
    // Retain call if the application needs call.cancel().
} );
```

The default adapter explicitly uses the existing context's Vert.x worker pool.
To select another existing application-owned `WorkerExecutor`, pass it as the
fifth constructor argument after the request configuration callback. Neither
Search nor `SignedJsonClient` creates or closes a worker pool. The application
owns Vert.x, the HTTP client, directly supplied credentials and optional workers.
Admission limits/rejection, queue sizing and worker sizing remain integration policy.
Use `Context.isOnWorkerThread()` for worker checks; executeBlocking retains the
originating context even while its callable runs on a worker thread.

Each attempt snapshots the effective HTTP/1 Host/authority, method and encoded
path/query after request configuration. An explicit Host header takes precedence;
otherwise the snapshot uses `HttpClientRequest.authority()`. Header values are
copied before crossing to the worker. The AWS adapter excludes transport framing
headers from signing while preserving them in the complete result, removes stale
AWS authentication/session headers and generates new authentication. The event loop
replaces the complete header set; it never merges an expired session token back in.
Workers never access mutable Vert.x requests. The body is retained, not copied,
and must remain unchanged through signing, sending and every retry.

The deadline starts before connection acquisition and includes queued signing,
credentials, transmission and response consumption. Cancellation signals the
worker and resets transport on the event loop. Every signing read checks the
signal; only the signing thread closes its cursor. Late callbacks are rejected
with attempt-generation/terminal guards. An arbitrary blocking credentials provider
cannot be forcibly interrupted; cancellation can finish while that provider still
runs, but its later completion cannot start a send.

Signing traverses a body once and sending uses another independent traversal.
The complete tree remains materialized; bounded encoded buffering is not a bound
on scalar processing time. AWS SDK 2.55.4 `signAsync` is not used: its ordinary
payload-checksum path buffers the complete body. No supported precomputed-hash
shortcut is assumed.

### Verification and integration handoff

```shell
./mvnw -f util/json/examples/vertx/pom.xml test
```

The twelve local signing test executions cover fixed-clock `es`/`aoss` wire
signatures, exact wire bytes/checksums, bounded independent signing/sending cursors,
event-loop responsiveness, explicit replay, credential failure, cancellation and
deadlines during signing and retry preparation. They verify effective Host/authority,
method, encoded paths/query and multiple header values, fresh signing on another
local destination after 503, absent GET bodies, removal of expired session headers,
and selection/ownership of an existing worker executor. Credentials are synthetic.
These are local HTTP/1.1 protocol checks, not live AWS service validation or benchmarks.

The repository [dispatcher handoff](../../../../design/elasticsearch-async-request-preparation-vertx-handoff.md)
gives exact production APIs, provider configuration/lifecycle, hook ordering,
Buffer-to-streaming adaptation, retry/cancellation rules and the remaining checklist
for the separate dispatcher integration session.

The example matches the intended hook in
[`AbstractRequestDispatcher.sendToNode`](https://github.com/marko-bekhta/quarkus/blob/feat/elasticsearch-vertx-client/independent-projects/elasticsearch-rest-client/src/main/java/io/quarkus/elasticsearch/restclient/vertx/internal/AbstractRequestDispatcher.java):
after `httpClient.request` and `applyHeaders`, before transmission. It does not
compile against or modify that WIP dispatcher, its node selection or its resolver.
Creating a transport request can acquire a connection before signing; cancellation
must reset it even while the worker is still resolving credentials.

### Scope

Only uncompressed JSON is sent. Automatic redirects are disabled; the example
returns response status and discards response chunks. A null body means absent,
whereas JSON null is a real body. A finite caller-selected destination list retries
502/503/504 with a fresh request, snapshot, signature and cursor under the original
deadline. Node discovery, dead-node tracking, connection-failure retry policy,
proxy authentication, HTTP/2 and custom body transformations are not validated here.

Request gzip/compression is deferred; response decompression is a separate transport
concern. Resolver dispatcher wiring is out of scope. Existing REST compression and
synchronous signing behavior are unchanged. Existing renamed Gson/REST entity helpers
stay in their current packages; they do not need relocation for this hook.
