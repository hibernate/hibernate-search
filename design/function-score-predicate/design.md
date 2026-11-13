# Function Score Predicate — Design Document

## 1. Problem / Motivation

Hibernate Search currently has no way to modify document relevance scores using external
factors like popularity, recency, or business rules. Users who want to blend relevance
with numeric field values (e.g. "boost popular products") must drop to backend-specific
APIs (`fromJson()` / `fromLuceneQuery()`), losing portability.

The `functionScore()` predicate bridges Elasticsearch's `function_score` query and Lucene's
`FunctionScoreQuery`, providing a portable API for the most common scoring use cases while
leaving backend-specific functions (script, decay, random) to the ES extension.

## 2. Semantic Model

Backend-agnostic scoring contract. Both backends must produce identical results.

### Inner query

The `functionScore` predicate wraps an inner query (defaults to `matchAll`). The inner
query determines which documents match and produces their base relevance score (`queryScore`).

### Functions

Each function produces a raw score for matched documents. Functions can have:
- An optional **per-function weight** (default = 1) — multiplies the raw output
- An optional **filter** — the function only applies to documents matching the filter

```
weightedScore_i = rawFunctionScore_i × weight_i    (default weight_i = 1)
```

Available function types in the common API:
- **`fieldValueFactor`** — reads a numeric field value, applies modifier and factor:
  `modifier(factor × fieldValue)`. Supports `.missing(value)` fallback.
- **`weight(N)`** (standalone) — produces base score 1.0 with per-function weight = N
  (verified against ES 8.17). For sum/multiply this is equivalent to score=N, but for
  weighted avg the distinction matters: `weight(2) + weight(3)` avg = 1.0, not 2.5.

ES extension adds: `scriptScore`, `randomScore`, `decay` (gauss/exp/linear).

### Score modes (combining multiple functions)

Applied to the weighted scores of all functions whose filter matches:

```
multiply:  Π(weightedScore_i)
sum:       Σ(weightedScore_i)
avg:       Σ(weightedScore_i) / Σ(weight_i)          ← WEIGHTED average, NOT / N
first:     weightedScore of first function whose filter matches
max:       max(weightedScore_i)
min:       min(weightedScore_i)
```

Default: `multiply`.

Note: `avg` uses **weighted average** — the denominator is the sum of weights, not the
count of functions.

### Boost modes (merging function score with query score)

```
multiply:  queryScore × combinedFunctionScore         (default)
replace:   combinedFunctionScore
sum:       queryScore + combinedFunctionScore
avg:       (queryScore + combinedFunctionScore) / 2
max:       max(queryScore, combinedFunctionScore)
min:       min(queryScore, combinedFunctionScore)
```

### Complete scoring pipeline

```
1. Evaluate inner query → queryScore
2. For each function i whose filter matches:
     weightedScore_i = rawFunctionScore_i × weight_i
3. Combine via scoreMode:
     combinedFunctionScore = scoreMode(weightedScore_1, weightedScore_2, ...)
4. Merge with query via boostMode:
     finalScore = boostMode(queryScore, combinedFunctionScore)
5. Apply predicate-level boost/constantScore (if set):
     finalScore = finalScore × predicateBoost
```

### Score validity / edge cases

| Scenario | Behavior (verified against ES 8.17) |
|----------|----------|
| Function skipped (filter doesn't match) | Excluded from scoreMode combination |
| No functions match any filter | `combinedFunctionScore = 1.0` (regardless of scoreMode) |
| Field missing + `.missing(v)` set | Substitute v, apply modifier(factor × v) normally |
| Field missing + no `.missing()` | **Error** — query fails with "Missing value for field" |
| Inner query score = 0 | boostMode applies normally (multiply → 0, sum → functionScore, replace → functionScore) |
| Standalone `.weight(N)` | Base function score = 1.0, per-function weight = N (not score=N, weight=1) |

#### RESOLVED: No functions match any filter

**Verified against ES 8.17:** when all functions have filters and none match a document,
`combinedFunctionScore = 1.0` regardless of scoreMode. This is the identity for multiply
and the actual value for replace/sum/avg:
- `boostMode().multiply()`: `queryScore × 1.0 = queryScore` (document passes through)
- `boostMode().sum()`: `queryScore + 1.0`
- `boostMode().replace()`: `1.0`

Both backends must produce `combinedFunctionScore = 1.0` when no functions match.

#### RESOLVED: Field missing + no `.missing()` set

**Verified against ES 8.17:** ES throws a hard error: *"Missing value for field [popularity]"*.
It does NOT produce 0 or preserve the score — it fails the entire search.

Contract: when `fieldValueFactor` targets a field that is missing from a document and
no `.missing()` is set, the query must fail with a clear error. On the Lucene backend,
this means the `DoubleValuesSource` must detect the missing value and throw (not return
0 or fall through to Lucene's default behavior).

This resolves the contradiction between the semantic model and Lucene's `boostByValue()` —
neither "function produces 0" nor "preserve original score" is correct. The correct
behavior is to **throw an error**.

#### RESOLVED: Standalone weight semantics

**Verified against ES 8.17:** `{"weight": N}` is modeled as a function producing base
score 1.0 with per-function weight = N. This is critical for weighted avg:

```
weight(2) + weight(3), score_mode=avg:
  weighted scores: 1.0×2=2, 1.0×3=3
  avg = (2 + 3) / (2 + 3) = 5/5 = 1.0   ← NOT (2+3)/2 = 2.5

field_value_factor(pop=10, w=3) + weight(100), score_mode=avg:
  weighted scores: 10×3=30, 1×100=100
  avg = (30 + 100) / (3 + 100) = 130/103 ≈ 1.262
```

Both backends must model standalone `.weight(N)` as base score 1.0 with per-function
weight N, not as function score N with weight 1.

### Field value factor modifiers

Transform the field value before it becomes the function's raw score:

```
none:        value                (default)
log:         log₁₀(value)
log1p:       log₁₀(1 + value)
log2p:       log₁₀(2 + value)
ln:          ln(value)
ln1p:        ln(1 + value)
ln2p:        ln(2 + value)
square:      value²
sqrt:        √value
reciprocal:  1 / value
```

Full formula: `rawFunctionScore = modifier(factor × fieldValue)`

## 3. Common API

### Entry point

New method on `SearchPredicateFactory`:

```java
// With explicit inner predicate
f.functionScore(f.match().field("title").matching("robot"))
    .fieldValueFactor("popularity")
    .boostMode().multiply();

// Defaults to matchAll
f.functionScore()
    .fieldValueFactor("popularity")
    .boostMode().multiply();

// Lambda form (consistent with bool().must())
f.functionScore(pf -> pf.match().field("title").matching("robot"))
    .fieldValueFactor("popularity")
    .boostMode().multiply();
```

### Fluent DSL — bool-style clause pattern

Functions are added as clause methods on `FunctionScoreClausesStep`, following the same
flow as `bool().must().should()`. Each function method returns a config step that
**extends the clauses step**, so you can break out to add another function or set options
at any point (once required params are satisfied).

```
FunctionScoreClausesStep<SR, S extends ClausesStep<SR,S>>
    (extends PredicateFinalStep, PredicateScoreStep<S>)
  ├── .fieldValueFactor("field") → FieldValueFactorConfigStep extends ClausesStep
  │     ├── .modifier() → ModifierStep → .log1p()/... → FieldValueFactorConfigStep
  │     ├── .factor(float) → FieldValueFactorConfigStep
  │     ├── .missing(value) → FieldValueFactorConfigStep
  │     ├── .missing(value, ValueModel) → FieldValueFactorConfigStep
  │     ├── .weight(float) → FieldValueFactorConfigStep  ← per-function weight
  │     └── .filter(pred) / .filter(lambda) → FieldValueFactorConfigStep
  │
  ├── .weight(float) → WeightConfigStep extends ClausesStep  ← standalone weight function
  │     └── .filter(pred) / .filter(lambda) → ClausesStep
  │
  ├── .scoreMode() → ScoreModeStep<S> → .sum()/... → S
  ├── .boostMode() → BoostModeStep<S> → .multiply()/... → S
  ├── .boost(float) → S
  └── .constantScore() → S
```

Single self-typing follows bool's pattern — everything on one step, all methods return
`S`. Functions and options can be mixed in any order. No separate `OptionsStep`.

At least one function must be added — building with no functions is a build-time error.

Functions with required parameters beyond their method argument return a step that does
NOT extend the clauses step until those params are provided (compile-time enforcement).

### DSL methods over enums

Score modes, boost modes, and modifiers are DSL methods (sub-step pattern without
`.end()`, matching `sort.missing().first()`), not enums. This allows promoting features
from backend extensions to the common API incrementally — adding a method is non-breaking.

```java
.boostMode().multiply()    .scoreMode().multiply()    .modifier().none()
.boostMode().replace()     .scoreMode().sum()          .modifier().log()
.boostMode().sum()         .scoreMode().avg()          .modifier().log1p()
.boostMode().avg()         .scoreMode().first()        .modifier().log2p()
.boostMode().max()         .scoreMode().max()          .modifier().ln()
.boostMode().min()         .scoreMode().min()          .modifier().ln1p()
                                                       .modifier().ln2p()
                                                       .modifier().square()
                                                       .modifier().sqrt()
                                                       .modifier().reciprocal()
```

### Weight: dual role

`.weight()` mirrors ES's overloading of the `weight` JSON key:

- **On config steps** (e.g. after `.fieldValueFactor()`): per-function multiplier.
  Maps to `{"field_value_factor": {...}, "weight": 3}`.
- **On clauses step**: standalone weight function producing a constant score.
  Maps to `{"weight": 42}`.

### Defaults

Both `scoreMode` and `boostMode` default to `multiply` (matching ES/OpenSearch). If the
user doesn't call these methods, the DSL doesn't call the corresponding builder method.
ES backend: null fields not serialized (ES applies its own default). Lucene backend:
builder initializes to `multiply` internally.

### Predicate-level boost and constantScore

`FunctionScoreClausesStep` extends `PredicateScoreStep` — standard predicate-level score
modification, separate from `boostMode`. Applied on top of the entire `functionScore`
result, same as any other predicate.

### Complete usage examples

```java
// Simple: boost by popularity
f.functionScore(f.match().field("title").matching("robot"))
    .fieldValueFactor("popularity")
        .modifier().log1p()
        .factor(2)
        .missing(1)
    .boostMode().multiply();

// Multiple functions with per-function weights and weighted avg
f.functionScore(f.match().field("title").matching("robot"))
    .fieldValueFactor("popularity").modifier().log1p().factor(2).weight(3)
    .weight(42).filter(f.match().field("featured").matching(true))
    .scoreMode().avg()       // weighted avg: (fvf*3 + 42*1) / (3 + 1)
    .boostMode().multiply();

// Multiple functions, sum mode
f.functionScore(f.match().field("title").matching("robot"))
    .fieldValueFactor("popularity").modifier().log1p().factor(2)
    .weight(42).filter(f.match().field("featured").matching(true))
    .scoreMode().sum()
    .boostMode().multiply();

// Weighted blend: relevance + dampened rank boost
f.functionScore(f.match().field("title").matching("robot"))
    .fieldValueFactor("rank")
        .factor(0.5f)
        .modifier().log1p()
    .boostMode().sum();

// Type-safe field reference with enum via bridge
f.functionScore(f.match().field("title").matching("robot"))
    .fieldValueFactor(MyEntity_.ranking)
        .missing(Ranking.OK)
        .modifier().log1p()
    .boostMode().multiply();

// Standalone weight (constant multiplier for all docs)
f.functionScore(f.match().field("title").matching("robot"))
    .weight(2.5f)
    .boostMode().multiply();

// Conditional weight with filter
f.functionScore(f.match().field("title").matching("robot"))
    .weight(42).filter(f.match().field("featured").matching(true))
    .boostMode().multiply();

// fieldValueFactor with per-function filter
f.functionScore(f.match().field("title").matching("robot"))
    .fieldValueFactor("popularity")
        .modifier().log1p()
        .filter(f.match().field("category").matching("books"))
    .boostMode().multiply();
```

## 4. Backend Behavior

### 4.1 Elasticsearch / OpenSearch

The `function_score` query JSON is identical between Elasticsearch and OpenSearch — same
structure, same functions, same parameters. A single implementation works for both
distributions. No dialect-specific handling or version gating needed (available in all
supported versions: ES 7.10+, OpenSearch 1.3+).

Direct mapping to `function_score` query JSON:

```json
{
  "function_score": {
    "query": { ... },
    "functions": [
      {
        "field_value_factor": {
          "field": "popularity",
          "modifier": "log1p",
          "factor": 2,
          "missing": 1
        },
        "weight": 3,
        "filter": { ... }
      },
      { "weight": 42, "filter": { ... } }
    ],
    "score_mode": "avg",
    "boost_mode": "multiply"
  }
}
```

ES mapping for `.weight()`:
- `.fieldValueFactor("popularity").weight(3)` → `{"field_value_factor": {...}, "weight": 3}`
- `.weight(42)` → `{"weight": 42}` (ES treats `weight` as the function when alone)

`score_mode` and `boost_mode` omitted from JSON when using the default (`multiply`).

### 4.2 Lucene

Custom components needed:

1. **Composite `DoubleValuesSource`** — wraps N `DoubleValuesSource` instances, combines
   per `scoreMode`. Each source can have an optional filter (Lucene `Query`) and weight.
   Must preserve insertion order for `scoreMode().first()` (ordered evaluation with
   short-circuit on first matching filter).

2. **Boost mode handling** — Lucene's `FunctionScoreQuery` natively supports `multiply`
   (`boostByValue`) and `replace` (constructor). For `sum`/`avg`/`max`/`min`, use
   `FunctionScoreQuery` in **replace** mode with a `DoubleValuesSource` that internally
   reads the inner query score via `DoubleValuesSource.SCORES` and computes the final
   combined value itself (e.g. for `sum`: `SCORES + functionValue`). The source must
   return `needsScores() = true`. The "replace" is technical — the replacement value
   already incorporates the original score.

3. **`fieldValueFactor`** — **cannot** use `DoubleValuesSource.fromDoubleField()` directly.
   All Lucene numeric doc values are stored as raw longs with type-specific encoding
   (Integer: widened to long; Float: `NumericUtils.floatToSortableInt()`; Double:
   `NumericUtils.doubleToSortableLong()`). Must use type-aware decoding via
   `LuceneNumericDomain.sortedDocValueToDouble(long)`. Prefer the aggregation pattern
   (`codec::sortedDocValueToDouble` as `Function<Long, Double>`) — handles all numeric
   types in one method. **Revisit exact approach before starting implementation.**

   The `missing` value goes through the convert-and-encode pipeline (ValueModel) before
   use as fallback. `modifier` and `factor` applied as wrapping source:
   `modifier(factor × fieldValue)`.

4. **Missing-value semantics** — critical for correctness. Lucene's `FunctionScoreQuery`
   behaves differently depending on construction:
   - `new FunctionScoreQuery(query, source)` → score = **0** if source has no value
   - `boostByValue(query, source)` → **original score preserved** if source has no value

   **Verified ES behavior:** missing field + no `missing` param = **hard error**.
   Neither Lucene behavior matches — the Lucene implementation must detect the missing
   value in the custom `DoubleValuesSource` and throw an exception, rather than relying
   on `FunctionScoreQuery`'s built-in missing-value handling.

   For the "no functions match" case: `combinedFunctionScore = 1.0`. The composite
   `DoubleValuesSource` must return 1.0 when no functions' filters match, regardless
   of scoreMode.

5. **Nesting awareness** — if `fieldValueFactor` targets a field inside a nested object,
   the `DoubleValuesSource` must be nested-aware (nested docs are separate Lucene documents).
   Follow the pattern from `DoubleMultiValuesToSingleValuesSource` which accepts a
   `NestedDocsProvider`.

## 5. Field / Type Integration

### Type-safe field references

All field-accepting methods have overloads for both string paths and type-safe field
references:

```java
// String path
.fieldValueFactor("popularity")
    .missing(1)                                 // Object, ValueModel.MAPPING default

// Type-safe field reference
.fieldValueFactor(MyEntity_.popularity)
    .missing(Ranking.OK)                        // T, type-safe via field reference
    .missing(2.0, ValueModel.INDEX)             // raw index value
```

### ValueModel on `.missing()`

`.missing(value)` defaults to `ValueModel.MAPPING` — the value goes through the
convert-and-encode pipeline (e.g. enum → numeric via bridge). `.missing(value, ValueModel)`
allows explicit control for `INDEX`, `STRING`, or `RAW` modes.

### Field type validation — ScoreFunction type keys

Score functions are a new category of query element, at the same level as predicates,
sorts, projections, and aggregations:

```
IndexFieldTraits
  ├── Predicates        → PredicateTypeKeys
  ├── Sorts             → SortTypeKeys
  ├── Projections       → ProjectionTypeKeys
  ├── Aggregations      → AggregationTypeKeys
  └── ScoreFunctions    → ScoreFunctionTypeKeys     (FIELD_VALUE_FACTOR, DECAY, ...)
```

| Key | Registered on |
|-----|---------------|
| `FIELD_VALUE_FACTOR` | all numeric types (integer, long, float, double, BigDecimal, BigInteger) |
| `DECAY` (future) | numeric, date, geo_point |

The `functionScore` predicate itself is global (on `SearchPredicateBuilderFactory`), but
individual functions validate their target fields through these keys via the standard
`scope.fieldQueryElement(fieldPath, ScoreFunctionTypeKeys.FIELD_VALUE_FACTOR)` call.
Incompatible fields fail early with the standard error.

### Metamodel / field reference interfaces

New reference interfaces under `engine/.../reference/scorefunction/`:
- `ScoreFunctionFieldReference<SR> extends FieldReference<SR>` (base marker)
- `FieldValueFactorFieldReference<SR, T>` (TYPED_INPUT — `T` carries domain type for
  type-safe `.missing(T)` with ValueModel conversion)

New entry in `TraitReferenceMapping` maps the trait string to the reference interface.
Generated metamodel classes gain new label segments for numeric fields. No changes needed
to `TraitKind`, `MetamodelClassWriter`, or `TypedFieldReferenceDetails`.

## 6. Nested / Multi-valued Semantics

### Nested objects

Verified against ES 8.17: `field_value_factor` cannot see nested fields from the root
document context. The entire `function_score` query must be a child of a `nested` query
for the field to be visible. A `nested` **filter** on a function does NOT change the
field resolution context — only a `nested` **query** wrapping the `function_score` works.

#### Automatic nesting (same nested path)

When all functions in a `functionScore` predicate target fields at the **same nested path**
(or root level), the system automatically wraps the query in the appropriate nested join.
The user just writes:

```java
f.functionScore(f.matchAll())
    .fieldValueFactor("children.popularity")
    .boostMode().multiply();
```

The system detects `children.popularity` is inside nested object `children` and:
- **ES backend**: wraps the `function_score` inside a `nested` query for path `children`
- **Lucene backend**: uses `NestedDocsProvider` + `ToParentBlockJoinQuery`

Multiple nested children produce multiple values. Like multi-valued fields (decision #23),
`fieldValueFactor` uses the **first value**.

#### Different nested paths → error

When functions target fields at **different nested paths**, automatic wrapping is impossible
— there is no single `nested` query that can resolve both. The system throws a validation
error with guidance:

```java
// This throws an error:
f.functionScore(f.matchAll())
    .fieldValueFactor("authors.rating")     // nested path: authors
    .fieldValueFactor("chapters.pages")     // nested path: chapters  ← different!
```

The error message guides the user to split into separate `functionScore` predicates,
each inside its own `nested()`, combined via `bool`:

```java
f.bool()
    .should(f.nested("authors").add(
        f.functionScore(f.matchAll())
            .fieldValueFactor("authors.rating")
            .boostMode().replace()))
    .should(f.nested("chapters").add(
        f.functionScore(f.matchAll())
            .fieldValueFactor("chapters.pages")
            .boostMode().replace()));
```

#### Mixing nested and root-level functions

A `functionScore` with both nested and root-level functions also triggers the error —
the nested function needs wrapping but the root function doesn't:

```java
// This also throws an error:
f.functionScore(f.matchAll())
    .fieldValueFactor("rootPopularity")     // root level
    .fieldValueFactor("authors.rating")     // nested path: authors
```

#### Lucene implementation

The `FieldValueFactorFunctionBuilder` Lucene implementation captures
`field.nestedDocumentPath()` at construction (same as sorts and aggregations). The
`DoubleValuesSource` uses a `NestedDocsProvider` to iterate child documents for each
parent, applying the "first value" policy.

### Multi-valued fields

Same policy as nested: `fieldValueFactor` uses the first value, following ES behavior.
For `decay` (future), ES explicitly supports `multi_value_mode` (min/max/avg/sum).

## 7. Extension API

### Elasticsearch extension

Additional functions available only through the ES extension:

```java
f.extension(ElasticsearchExtension.get())
    .functionScore(f.match().field("title").matching("robot"))
    .fieldValueFactor("popularity").modifier().log1p()
    .decay("date").gauss()
        .origin("now").scale("10d").offset("2d").decay(0.5)
    .scriptScore("_score * doc['boost'].value")
    .randomScore().seed(42).field("_seq_no")
    .scoreMode().sum()
    .boostMode().multiply()
    .maxBoost(42)
    .minScore(5);
```

The ES extension is straightforward — single self-typing:

```
ElasticsearchFunctionScoreClausesStep<SR>
    extends FunctionScoreClausesStep<SR, ElasticsearchFunctionScoreClausesStep<SR>>
  └── .decay(), .scriptScore(), .randomScore(), .maxBoost(float), .minScore(float)
```

Config steps for ES-only functions carry 3 type params (`<SR, S, T>`).

## 8. Testing

Follow existing predicate testing patterns:

- **TCK tests** (common API) — nested test classes, top-level index definitions, iterate
  over all compatible field types. Cover: single function, multiple functions, all score
  modes, all boost modes, all modifiers, missing values, per-function filters, field type
  validation (error on non-numeric), boost/constantScore, interaction with nested predicates.
- **Backend-specific TCK tests** — ES extension features (scriptScore, randomScore, decay,
  maxBoost, minScore).
- **Unsupported type tests** — verify early failure with clear error message when using
  fieldValueFactor on keyword/text/date/geo fields.

### Score matrix tests (predicate-specific)

Mathematical tests that verify exact scores across both backends. These catch backend
discrepancies quickly.

**Single function — all boostMode combinations (queryScore=10, functionScore=2):**

| queryScore | functionScore | scoreMode | boostMode | expected |
|------------|---------------|-----------|-----------|----------|
| 10 | 2 | multiply | multiply | 20 |
| 10 | 2 | multiply | replace | 2 |
| 10 | 2 | multiply | sum | 12 |
| 10 | 2 | multiply | avg | 6 |
| 10 | 2 | multiply | max | 10 |
| 10 | 2 | multiply | min | 2 |

**Multiple functions — all scoreMode combinations (f1=2, f2=3):**

| f1 | f2 | scoreMode | combined |
|----|-----|-----------|----------|
| 2 | 3 | multiply | 6 |
| 2 | 3 | sum | 5 |
| 2 | 3 | avg | 2.5 |
| 2 | 3 | max | 3 |
| 2 | 3 | min | 2 |
| 2 | 3 | first | 2 (first matching) |

**Weighted average (f1=2 weight=3, f2=3 weight=4):**

| f1 | w1 | f2 | w2 | scoreMode | combined |
|----|----|----|-----|-----------|----------|
| 2 | 3 | 3 | 4 | avg | (2×3 + 3×4) / (3+4) = 18/7 ≈ 2.571 |
| 2 | 3 | 3 | 4 | sum | 2×3 + 3×4 = 18 |
| 2 | 3 | 3 | 4 | multiply | (2×3) × (3×4) = 72 |

**Edge cases (verified against ES 8.17):**

| scenario | scoreMode | boostMode | expected |
|----------|-----------|-----------|----------|
| field missing + `.missing(5)` | multiply | multiply | queryScore × modifier(factor × 5) |
| field missing + no `.missing()` | any | any | **ERROR** — query fails |
| filter doesn't match (1 of 2 fns) | sum | multiply | queryScore × matchingFunctionScore |
| no functions match any filter | any | multiply | queryScore × 1.0 = queryScore |
| no functions match any filter | any | replace | 1.0 |
| no functions match any filter | any | sum | queryScore + 1.0 |
| inner query score = 0 | multiply | sum | 0 + functionScore |
| standalone weight(N) | avg | replace | 1.0 (base=1.0, weight=N, avg=N/N=1.0) |

**Missing-value test matrix** — all combinations of:
- function matches / doesn't match (filter)
- field exists / missing (with and without `.missing()`)
- inner query score = 0 vs non-zero
- boostMode: multiply, sum, replace (at minimum)
- scoreMode: multiply, sum, avg (at minimum)

## 9. Decisions

| # | Decision | Rationale |
|---|----------|-----------|
| 1 | Global predicate, not field-scoped | `functionScore` wraps another predicate, like `bool`. Goes through `SearchPredicateBuilderFactory`, not `PredicateTypeKeys` |
| 2 | Bool-style clause flow | Functions are clause methods on a clauses step. Config steps extend clauses step for optional params, don't extend for required params |
| 3 | Methods over enums for modes/modifiers | Allows incremental promotion of features from ES extension to common API without breaking changes |
| 4 | Field references with overloads (TYPED_INPUT) | `.missing()` needs ValueModel conversion (e.g. enum via bridge → numeric) |
| 5 | All 6 boost modes in common API | Once custom Lucene `DoubleValuesSource` for `sum` exists, `avg`/`max`/`min` are trivial additions |
| 6 | All 6 score modes in common API | Same rationale — composite `DoubleValuesSource` handles all modes with a switch |
| 7 | New `ScoreFunctions` category in `IndexFieldTraits` | Score functions are their own concern. Own trait namespace (`score-function:*`), own `ScoreFunctionTypeKeys` registry |
| 8 | Inner predicate defaults to matchAll | Optional argument to `functionScore()` |
| 9 | At least one function required | Build-time error without functions — pointless otherwise |
| 10 | `.filter()` on all function types | Every function supports optional per-function filter, matching ES |
| 11 | `weight` dual role | `.weight()` on ClausesStep = standalone function. `.weight()` on config steps = per-function multiplier. Mirrors ES's overloading of the `weight` key |
| 12 | Defaults: scoreMode=multiply, boostMode=multiply | Match ES/OpenSearch defaults |
| 13 | `boost()`/`constantScore()` via PredicateScoreStep | Standard predicate-level score modification, separate from boostMode |
| 14 | `.missing()` with ValueModel overloads | Needed for enum-via-bridge scenarios |
| 15 | OpenSearch fully compatible | Identical JSON, single implementation |
| 16 | Single step, no ordering enforcement (bool pattern) | Functions and options mixable in any order. Avoids dual self-typing complexity |
| 17 | Single self-typing `<SR, S>` on ClausesStep | ES extension just extends and adds methods. Config steps carry 3 type params |
| 18 | Lambda support for inner predicate and filters | Consistent with `bool().must()` |
| 19 | Nesting: same path = auto-wrap, different paths = error | All functions must target the same nested path (or root). Auto-wraps in `nested` query. Different paths throw with guidance to split via `bool` + `nested()`. Verified against ES 8.17 |
| 20 | `float` for `factor` and `weight` | Consistent with `boost(float)` on `PredicateScoreStep` |
| 21 | `scoreMode().first()` preserves insertion order | Ordered evaluation with short-circuit |
| 22 | Sub-step pattern (without `.end()`) | Matches `sort.missing().first()` pattern |
| 23 | Multi-valued fields: use first value | Follows ES behavior. Future `decay` can add `multi_value_mode` |
| 24 | All numeric types including BigDecimal/BigInteger | `FIELD_VALUE_FACTOR` registered on all numeric field types |
| 25 | Explicit scoring semantic model | Full pipeline with weighted avg formula. Both backends must match |
| 26 | Missing-value / no-match semantics (verified ES 8.17) | No functions match → `combinedFunctionScore = 1.0`. Field missing + no `.missing()` → **error**. Standalone `weight(N)` = base score 1.0, per-fn weight N. Lucene must use custom handling, not built-in `FunctionScoreQuery` behavior |

### Discarded alternatives

| Alternative | Why discarded |
|-------------|---------------|
| Enums for score/boost modes | Less extensible; harder to promote from ES extension to common API |
| `.add(fn -> ...)` lambda pattern | Bool-style clause methods more natural |
| Single-function-only common API | Multi-function achievable on Lucene; no reason to limit |
| `script_score` in common API | Inherently ES-specific |
| `functionScore` as field-scoped predicate | Wraps a query, not a field |
| Score function keys in `PredicateTypeKeys` | Score functions are a distinct concern |
| UNTYPED field references | `.missing()` needs ValueModel conversion |
| Flat methods (`.modifierLog1p()`) | Sub-step groups options, keeps `ConfigStep` clean |
| `.constant(value)` function type | No clean ES mapping; `weight` mirrors ES directly |
| Functions-before-options ordering | Required dual self-typing (`<SR, S, O>`), 4-param generics. Bool pattern simpler |
