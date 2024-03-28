/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.spi;

import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.common.NamedValues;

public interface WithParametersAggregationBuilder<T> extends SearchAggregationBuilder<T> {
	void creator(Function<? super NamedValues, ? extends AggregationFinalStep<T>> aggregationCreator);
}
