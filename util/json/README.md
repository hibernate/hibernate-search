# Hibernate Search JSON utilities

`hibernate-search-util-json` separates library-neutral transport contracts from
Search's internal Jackson-native parsing, output, validation and codecs.
It is independent of Elasticsearch and HTTP.
The public `org.hibernate.search.util.json.JsonHandler<T>`
contract also supports library-independent JSON boundaries. Backend adapter factories
provide its Elasticsearch integration; this module provides no replacement tree or public accessors.

Transport integrators use `org.hibernate.search.util.json.spi`: replayable
`JsonBody`, resumable `JsonWriteCursor`, and `JsonResponseConsumer<T>` /
`JsonResponseConsumerFactory<T>`. Search supplies the body and response factory;
the transport sends bytes, feeds response bytes and returns the resulting `T`.
These contracts expose no Jackson types and require no JSON parser implementation.

The `org.hibernate.search.util.json.internal.spi` package supports the JSON utility implementation
and Elasticsearch backend only; JPMS grants access to the backend, without client or AWS grants. It contains Jackson-bound factories, codecs,
parsing and validation, plus internal output strategy selection. It is not an
application extension point. The Jackson examples below document implementation
behavior for Search contributors; they are not supported external SPI recipes.

This guide covers the module directly. For Hibernate Search backend integration,
see the [JSON reference chapter](../../documentation/src/main/asciidoc/public/reference/_backend-elasticsearch-json.adoc).
For a complete asynchronous HTTP example, see the [Vert.x streaming example](examples/vertx/README.md).

## Dependency and API map

Use the same version as the other Hibernate Search artifacts in your application:

```xml
<dependency>
    <groupId>org.hibernate.search</groupId>
    <artifactId>hibernate-search-util-json</artifactId>
    <version>${hibernate-search.version}</version>
</dependency>
```

The Java module name is `org.hibernate.search.util.json`.

| Need | API |
| --- | --- |
| Build or inspect a tree | Jackson `JsonNode`, `ObjectNode`, `ArrayNode` and standard scalar nodes |
| Validate external trees | `JsonTreeValidation.validateExternal(JsonNode)` |
| Construct exact numeric nodes | `JsonNumbers.fromNumber(Number)` |
| Parse an owned String | `JsonParsing.parse(String)` |
| Parse incoming bytes | `JsonParsing.createParser()` and `JsonParserSession` |
| Write or replay UTF-8 JSON | `JsonBodies`, `JsonBody`, `JsonWriteCursor` |
| Convert explicitly supported Java types | `JsonCodec`, `JsonCodecs`, `JsonCodecRegistry` |

`JsonBody` and `JsonWriteCursor` live in the external `spi` package.
The remaining services in this table live in `internal.spi`, for the JSON utility/backend implementation only.
Classes in `impl` are implementation details.

## Building trees and exact numeric values

```java
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

ObjectNode document = JsonNodeFactory.instance.objectNode();
document.put( "title", "Streaming JSON" );
document.put( "pages", 42 );
document.putNull( "summary" );
```

Objects and arrays are mutable; retain stable trees through processing and replay.
Use Jackson navigation for application code. Backend accessors are internal and
preserve its stricter missing/null and explicit singleton-array behavior.

Parsing selects cached IntNode where applicable, LongNode for other signed 64-bit
integers, and BigIntegerNode otherwise. Decimal input uses exact BigDecimal values;
negative decimal/exponent zero uses a standard DoubleNode(-0.0) to retain its sign.
Integer -0 may normalize. Numbers never pass through double before an explicitly
selected floating conversion. Numeric spelling, decimal scale and node class are
not round-trip guarantees.

Decimal exponent, scale and normalized exponent must fit ±2147483647; Integer.MIN_VALUE
scale is rejected. The 1000-character numeric input limit applies to text parsing,
not already constructed exact nodes. `JsonNumbers.toBigInteger(BigDecimal)` accepts
mathematically integral values, rejects fractions and limits negative-scale expansion
to 100000. Textual integer conversions remain strict. Accepted decimal storage can
have a larger exponent than this conversion supports.

`JsonTreeValidation.validateExternal` accepts standard acyclic JSON graphs, including
shared subtrees. It rejects POJO/raw/binary/custom/missing nodes, nonfinite numbers,
cycles, invalid member names and invalid raw container values, without serialization
or reflection. It checks the same decimal policy used by both parser routes.

## External validation and internal assurance

Public arbitrary-tree factories (`JsonBodies.of`, `ndjson`, `openCursor`) always
validate their input, as do backend application handler imports, transformer results
and backend-owned response/request construction boundaries. Keep retained trees stable afterward;
validation does not cache a valid bit or isolate later mutation.

The explicitly named internal SPI routes (`ofInternal`, `ndjsonInternal`,
`openCursorInternal`, request `bodyJsonFromInternal`, response `withInternalJsonBody`)
require ordinary acyclic trees constructed/parsed by Search or already validated
at their external boundary. They avoid repeated whole-tree walks in production.
They are caller contracts, not trust inferred from the Jackson node class.

Set Search configuration property `hibernate.search.json.internal_validation=true`
to enable internal invariant validation and writer cycle tracking for that Search context.
An explicitly configured value (including `false`) takes precedence over the JVM
property of the same name; the final default is `false`. Services, bodies and
cursors capture an immutable policy, so independent Search contexts can use different
modes in the same JVM. Standalone utility entry points snapshot the JVM property
when a service, body or cursor is created. Repository Surefire/Failsafe tests leave
it disabled by default, matching production. Enable it locally with `-Pjson-internal-validation` (or
`-Dtest.json.internal_validation=true`). The nightly JSON validation matrix entry
uses that profile for utility/backend unit tests and the existing Elasticsearch
integration-test executions for all four clients.
When disabled, internal validation does not traverse the tree and writers allocate
no cycle tracking state. Numeric fallback encoding may still use its fixed character
workspace, and NDJSON shares that encoding workspace across parts.

Debug checks provide assurance, not a proof of all internal invariants. With them
disabled, violating an internal route's acyclic/stable-tree contract may cause a
nonterminating traversal. External validation remains unconditional in both modes;
escaping, finite/exact number checks, output bounds and cleanup remain active.

## Parsing Strings and incoming bytes

For an already materialized String:

```java
import tools.jackson.databind.JsonNode;
import org.hibernate.search.util.json.internal.spi.JsonParsing;

JsonParsing parsing = new JsonParsing();
JsonNode tree = parsing.parse( "{\"title\":\"Streaming JSON\"}" );
```

This call parses characters directly without first encoding the whole String.
Parsing requires strict JSON and one root value: comments, single quotes,
non-finite numbers and trailing roots are rejected. A root can be an object,
array, scalar or JSON null.

For a sequence of UTF-8 transport chunks, keep one parser session alive from
first input through EOF:

```java
import java.nio.ByteBuffer;
import tools.jackson.databind.JsonNode;
import org.hibernate.search.util.json.internal.spi.JsonParserSession;
import org.hibernate.search.util.json.internal.spi.JsonParsing;

final class IncomingJson implements AutoCloseable {
    private final JsonParserSession parser;

    IncomingJson(JsonParsing parsing) {
        parser = parsing.createParser();
    }

    void onChunk(ByteBuffer input) {
        while ( input.hasRemaining() ) {
            parser.feed( input );
        }
    }

    JsonNode onEnd() {
        try {
            return parser.endOfInput();
        }
        finally {
            close();
        }
    }

    @Override
    public void close() {
        parser.close();
    }
}
```

The transport must also call `close()` on failure, cancellation or disconnect.
Do not use a try-with-resources block that closes the session immediately after
installing asynchronous handlers.

Each `feed` accepts at most `maxInputChunkSize()` bytes, advances the buffer's
position by that slice on success and processes the available tokens before
returning. Loop until the chunk is consumed. A buffer can be heap-backed, direct
or read-only, and its position/limit select the offered region. The session does
not retain caller storage after the call, including on failure; buffer position
is unspecified after failure. Never mutate input during `feed`.

UTF-8 code points, JSON escapes and tokens can span chunks. Only `endOfInput()`
returns the root, after validating EOF and trailing input. Empty/whitespace input
throws `JsonEmptyInputException`; a transport can choose to interpret this as an
absent HTTP body. The JSON text `null` instead returns Jackson `NullNode.instance`.

Use `createParser(charset)` when the transport explicitly selects another source
charset. It transcodes incrementally with bounded scratch space. Charset selection,
HTTP body absence, decompression and retry policy belong to the transport.

### Constraints and memory

```java
import org.hibernate.search.util.json.internal.spi.JsonParsingOptions;

JsonParsing capped = new JsonParsing(
        JsonParsingOptions.DEFAULT.withMaxInputBytes( 1024 * 1024 ) );
```

| Default constraint | Value |
| --- | ---: |
| Nesting depth | 1000 |
| Decoded string length | 20,000,000 characters |
| Decoded name length | 50,000 UTF-8 bytes |
| Number token length | 1000, using Core's token-length counting |
| Bytes accepted per feed | 8192 |
| Total input bytes | Unlimited (`0`) |

The full `JsonParsingOptions` constructor configures all six values. The byte cap
counts original source bytes before charset transcoding. For String parsing it
counts the equivalent UTF-8 length. If the transport decompresses first, a parser
cap applies to the decompressed input it receives; compressed wire limits are a
separate transport policy.

Input slices and scratch buffers are bounded. The completed tree, names and scalar
Strings still grow with the document. This is incremental tree construction,
not a token subscription API or a constant-memory parser for arbitrarily large
documents. `feed` and EOF processing are synchronous CPU work; a bounded input
slice does not guarantee an event-loop latency bound.

## Streaming UTF-8 output and replay

```java
import java.nio.ByteBuffer;
import org.hibernate.search.util.json.internal.spi.JsonBodies;
import org.hibernate.search.util.json.spi.JsonBody;
import org.hibernate.search.util.json.spi.JsonWriteCursor;

JsonBody body = JsonBodies.of( document );
ByteBuffer buffer = ByteBuffer.allocate( 8192 );
try ( JsonWriteCursor cursor = body.openCursor() ) {
    while ( !cursor.isComplete() ) {
        buffer.clear();
        cursor.write( buffer );
        buffer.flip();
        // Your synchronous sink must consume the entire remaining region here.
        consumeFully( buffer );
    }
}
```

`consumeFully` above represents your sink. A channel accepting a partial write
must finish the buffer's unread suffix before asking the cursor for more. An
asynchronous sink must finish using emitted storage before you reuse it, or receive
its own copy. The [Vert.x example](examples/vertx/README.md#writing-a-response)
shows that ownership transfer and waits for each write before producing another.

`write` fills only the destination's remaining region, advances its position and
leaves its limit unchanged. Zero capacity makes no progress; a read-only destination
is rejected. The cursor retains traversal state, not the destination buffer, and
can pause within a scalar or escape. Close/cancel it on errors and cancellation;
closing does not close the caller's sink.

`JsonBodies.of(root)` probes up to 8 KiB once to determine a small body's length
without retaining the probe's bytes. `contentLength()` is then stable: a larger
body returns `-1`, even after output or signing. Select appropriate HTTP framing
for unknown lengths. Use `JsonBodies.openCursor(root)` to skip the probe when
only a traversal is needed; `withContentLengthProbe` can wrap a custom body.

`JsonBody.writeTo(OutputStream)` uses the body's native writer when available and
falls back to an independent cursor for custom bodies. Search modules can select
`JsonWriteStrategy.CURSOR` through the internal `JsonWriting.writeTo` helper to
drain a cursor with bounded 8 KiB storage. Both strategies produce the same bytes and neither closes
nor flushes the destination. `openStream()` always drains an independent cursor
with 8 KiB storage for synchronous consumers and signing. Close each opened input
stream. Open a new cursor or stream for every retry; their traversal positions are
independent.

Keep source trees acyclic and unchanged from body creation through every read,
signing operation and retry. Bodies reference trees without freezing or copying
them. Share factories and bodies with stable source trees;
use each parser session/cursor/stream from one consumer at a time.

Output is UTF-8, preserves supported numeric values and object order, and escapes JSON
controls, HTML-sensitive characters and U+2028/U+2029. Valid surrogate pairs are
encoded as UTF-8; isolated surrogates are escaped. `JsonBodies.of` adds no newline or HTTP framing. `JsonBodies.ndjson(parts)` emits
one newline after each part and probes the whole framed body once, sharing a private
workspace between sequential parts. Each traversal has independent state. One parser
session accepts one JSON root, not multiple NDJSON records.

Six encoded pending bytes do not describe all memory. Primitive fallback retains
twenty unencoded characters; BigInteger/decimal output can derive and retain a String
proportional to digits. BigDecimal can cache its rendering in the immutable source
number. These routes do not promise scalar-independent total scratch or latency.

## Explicit Java/tree codecs

```java
import java.util.List;
import tools.jackson.databind.JsonNode;
import org.hibernate.search.util.json.internal.spi.JsonCodec;
import org.hibernate.search.util.json.internal.spi.JsonCodecs;

JsonCodec<List<String>> strings = JsonCodecs.list( JsonCodecs.STRING );
JsonNode encoded = strings.encode( List.of( "java", "async" ), false );
List<String> decoded = strings.decode( encoded );
```

`JsonCodecs` supplies scalar, tree, list and string-keyed map codecs. The
`serializeNulls` flag controls omission of null object members in codecs that
construct objects; null array positions remain present. `JsonCodecs.copy` applies
that policy recursively. Scalar codecs preserve explicit coercion behavior; they
do not provide automatic Java model mapping.

Search modules implement `JsonCodec<T>` for internal model types by explicitly
decoding/encoding known fields. `JsonCodecRegistry` holds explicit class registrations;
`reference(type)` defers lookup for recursive model graphs. Unknown types fail
instead of using reflective mapping. Codecs receive completed trees, independently
of byte parsing and output scheduling.

## Vert.x and other asynchronous transports

The [runnable Vert.x example](examples/vertx/README.md) receives request chunks,
parses them incrementally and streams the completed tree back. A short input
adapter uses the public Vert.x buffer API:

```java
// Inside a Vert.x request/response Buffer handler, with one session and scratch
// array per message; keep the session until endHandler or another terminal event.
for ( int offset = 0; offset < chunk.length(); ) {
    int count = Math.min( scratch.length, chunk.length() - offset );
    chunk.getBytes( offset, offset + count, scratch, 0 );
    ByteBuffer input = ByteBuffer.wrap( scratch, 0, count );
    while ( input.hasRemaining() ) {
        parser.feed( input );
    }
    offset += count;
}
```

The full example supplies EOF, failure and disconnect handlers, bounded copies,
HTTP framing and output flow control. There is no built-in Vert.x Elasticsearch
transport. Applications choose execution and demand policy. When offloading,
pause ingress, transfer owned bytes, serialize work for a session and resume on
the owning context only after consuming them; never queue unbounded chunks or
run overlapping feeds. Synchronous signing/interceptors need their own policy too.

## Building and verification

From the repository root:

```shell
./mvnw install -pl util/json -am -DskipTests -DskipITs -Dno-build-cache
./mvnw test -pl util/json -DskipITs -Dno-build-cache
```

The module's tests cover ordinary-node/numeric policies, parser fragmentation and limits,
output correctness/replay, buffer retention and API dependency boundaries.
See the [benchmark guide](../../integrationtest/performance/backend/README.md#server-independent-json-comparisons)
for timing and allocation experiments, separately from correctness checks.
