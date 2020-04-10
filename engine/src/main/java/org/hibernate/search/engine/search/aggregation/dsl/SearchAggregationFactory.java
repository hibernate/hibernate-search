/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import org.hibernate.search.util.common.SearchException;

/**
 * A factory for search aggregations.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
*/
public interface SearchAggregationFactory {

	/**
	 * Perform aggregation in range buckets.
	 * <p>
	 * Given a field and one or more ranges of values,
	 * this aggregation creates one bucket per range,
	 * and puts in each bucket every document for which
	 * the given field has a value that falls into the corresponding range.
	 * <p>
	 * For each bucket, the document count is computed,
	 * or more complex metrics or sub-aggregations for backends that support it.
	 *
	 * @return The next step.
	 */
	RangeAggregationFieldStep<?> range();

	/**
	 * Perform aggregation in term buckets.
	 * <p>
	 * Given a field,
	 * this aggregation creates one bucket per term of that field in the index,
	 * and puts in each bucket every document for which
	 * the given field matches the corresponding term.
	 * <p>
	 * For each bucket, the document count is computed,
	 * or more complex metrics or sub-aggregations for backends that support it.
	 *
	 * @return The next step.
	 */
	TermsAggregationFieldStep<?> terms();

	/**
	 * Extend the current factory with the given extension,
	 * resulting in an extended factory offering different types of aggregations.
	 *
	 * @param extension The extension to the aggregation DSL.
	 * @param <T> The type of factory provided by the extension.
	 * @return The extended factory.
	 * @throws SearchException If the extension cannot be applied (wrong underlying backend, ...).
	 */
	<T> T extension(SearchAggregationFactoryExtension<T> extension);

}
