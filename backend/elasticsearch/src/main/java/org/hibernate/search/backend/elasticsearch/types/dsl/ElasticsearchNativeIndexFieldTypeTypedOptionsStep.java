package org.hibernate.search.backend.elasticsearch.types.dsl;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeOptionsStep;

/** Options for a native JSON field whose indexing input and projection output are both {@code T}. */
public interface ElasticsearchNativeIndexFieldTypeTypedOptionsStep<
		S extends ElasticsearchNativeIndexFieldTypeTypedOptionsStep<?, T>,
		T>
		extends IndexFieldTypeOptionsStep<S, T> {
	/**
	 * Treat each indexed array as one JSON value, preserving empty arrays and nested arrays.
	 * By default root arrays represent multiple field values, for both Gson and Jackson handlers.
	 * Array-only handlers require this option to project their arrays. This option participates in
	 * cross-index field compatibility.
	 * @return This step for chaining.
	 */
	S arrayAsValue();
}
