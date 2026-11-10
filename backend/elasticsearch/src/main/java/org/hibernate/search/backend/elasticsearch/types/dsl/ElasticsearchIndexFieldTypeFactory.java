package org.hibernate.search.backend.elasticsearch.types.dsl;

import org.hibernate.search.backend.elasticsearch.search.predicate.dsl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.backend.elasticsearch.search.sort.dsl.ElasticsearchSearchSortFactory;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.util.json.JsonHandler;

public interface ElasticsearchIndexFieldTypeFactory extends IndexFieldTypeFactory {

	/**
	 * Define a native field type.
	 * <p>
	 * A native field type has the following characteristics:
	 * <ul>
	 *     <li>Hibernate Search doesn't know its exact type, so it must be entirely defined as a JSON object,
	 *     provided in the subsequent {@code mapping(...)} step</li>
	 *     <li>When indexing, fields of this type must be populated with JSON.
	 *     The field uses Gson JSON values and can contain scalars, objects and arrays.
	 *     For compatibility, projected root arrays are split into multiple field values.</li>
	 *     <li>The predicate/sort/projection DSLs have only limited support for fields of this type.
	 *     Some features may not work and throw an exception, such as phrase predicates.
	 *     It is recommended to create the predicate/sort/projections targeting these fields from JSON
	 *     using {@link ElasticsearchSearchPredicateFactory#fromJson(String)}
	 *     or {@link ElasticsearchSearchSortFactory#fromJson(String)}</li>
	 * </ul>
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	@Deprecated
	ElasticsearchNativeIndexFieldTypeMappingStep asNative();

	/**
	 * Define a native field type using the given handler for field values.
	 * <p>
	 * The handler selects the Java representation {@code T} of values supplied for indexing
	 * and returned by projection. When using a value bridge, {@code T} must match the bridge's
	 * index-side type: a {@code ValueBridge<V, T>} converts between entity values of type {@code V}
	 * and JSON values of type {@code T}, while this handler converts between {@code T} and JSON.
	 * <p>
	 * The subsequent {@code mapping(...)} step defines the Elasticsearch field type and options.
	 * A handler passed to that step only reads the mapping definition and can use a different
	 * Java representation from this field value handler.
	 * <p>
	 * Root arrays are split into multiple field values by default, for both Gson and Jackson handlers.
	 * Call {@code mapping(...).arrayAsValue()} to preserve each array as one JSON value, including an empty array.
	 * In that mode, multi-valued fields store an outer array of separately indexed values and project each independently.
	 * Array-only handlers require this explicit option for array round trips.
	 * Field-level JSON null projects as Java null.
	 *
	 * @param handler The handler for field values, used for both indexing and projection.
	 * @param <T> The Java representation of field values, matching the index-side type of any bridge.
	 * @return A DSL step where the Elasticsearch mapping can be defined.
	 */
	<T> ElasticsearchNativeIndexFieldTypeTypedMappingStep<T> asNative(JsonHandler<T> handler);

}
