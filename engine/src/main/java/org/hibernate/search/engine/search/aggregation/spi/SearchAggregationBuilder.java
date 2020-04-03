/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.spi;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;

/**
 * A search aggregation builder, i.e. an object responsible for collecting parameters
 * and then building a search aggregation.
 *
 * @param <A> The type of resulting aggregations.
 */
public interface SearchAggregationBuilder<A> {

	SearchAggregation<A> build();

}
