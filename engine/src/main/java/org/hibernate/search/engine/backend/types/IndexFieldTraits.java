/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types;

/**
 * Constants for the names of various traits that can be exposed by index fields.
 * <p>
 * Traits are a representation of what a field can be used for at search time:
 * a particular predicate, sort, projections, aggregation...
 * <p>
 * See also backend-specific traits: {@code org.hibernate.search.backend.lucene.types.LuceneIndexFieldTraits},
 * {@code org.hibernate.search.backend.elasticsearch.types.ElasticsearchIndexFieldTraits}.
 *
 * @see Predicates
 * @see Sorts
 * @see Projections
 * @see Aggregations
 */
public final class IndexFieldTraits {

	private IndexFieldTraits() {
	}

	/**
	 * Constants for the names of predicate-related traits that can be exposed by index fields.
	 *
	 * @see IndexFieldTraits
	 */
	public static class Predicates {

		private Predicates() {
		}

		public static String named(String name) {
			return "predicate:named:" + name;
		}

		public static final String EXISTS = "predicate:exists";
		public static final String KNN = "predicate:knn";
		public static final String MATCH = "predicate:match";
		public static final String NESTED = "predicate:nested";
		public static final String PHRASE = "predicate:phrase";
		public static final String RANGE = "predicate:range";
		public static final String QUERY_STRING = "predicate:query-string";
		public static final String REGEXP = "predicate:regexp";
		public static final String SIMPLE_QUERY_STRING = "predicate:simple-query-string";
		public static final String SPATIAL_WITHIN_BOUNDING_BOX = "predicate:spatial:within-bounding-box";
		public static final String SPATIAL_WITHIN_CIRCLE = "predicate:spatial:within-circle";
		public static final String SPATIAL_WITHIN_POLYGON = "predicate:spatial:within-polygon";
		public static final String TERMS = "predicate:terms";
		public static final String WILDCARD = "predicate:wildcard";

	}

	/**
	 * Constants for the names of sort-related traits that can be exposed by index fields.
	 *
	 * @see IndexFieldTraits
	 */
	public static class Sorts {

		private Sorts() {
		}

		public static final String DISTANCE = "sort:distance";
		public static final String FIELD = "sort:field";

	}

	/**
	 * Constants for the names of projection-related traits that can be exposed by index fields.
	 *
	 * @see IndexFieldTraits
	 */
	public static class Projections {

		private Projections() {
		}


		public static final String DISTANCE = "projection:distance";
		public static final String FIELD = "projection:field";
		public static final String HIGHLIGHT = "projection:highlight";
		public static final String OBJECT = "projection:object";

	}

	/**
	 * Constants for the names of aggregation-related traits that can be exposed by index fields.
	 *
	 * @see IndexFieldTraits
	 */
	public static class Aggregations {

		private Aggregations() {
		}

		public static final String RANGE = "aggregation:range";
		public static final String TERMS = "aggregation:terms";

	}


}
