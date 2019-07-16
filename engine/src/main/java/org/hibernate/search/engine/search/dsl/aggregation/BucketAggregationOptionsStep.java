/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.aggregation;

import java.util.Map;

/**
 * The final step in an aggregation definition, where the aggregation can be retrieved.
 *
 * @param <K> The type of bucket keys for this aggregation.
 * For example {@code String} for a terms aggregation on a text field,
 * or {@code Range<Integer>} for a range aggregation on an integer field.
 * @param <V> The type of bucketed values for this aggregation.
 * For example {@code Integer} (the document count)
 * by default for most bucket aggregations.
 */
public interface BucketAggregationOptionsStep<K, V>
		extends AggregationFinalStep<Map<K, V>> {

}
