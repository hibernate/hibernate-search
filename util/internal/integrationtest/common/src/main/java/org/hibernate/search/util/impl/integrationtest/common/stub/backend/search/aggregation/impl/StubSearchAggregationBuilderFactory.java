/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.aggregation.impl;

import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.spi.AggregationTypeKeys;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilderFactory;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.impl.StubQueryElementCollector;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;

public class StubSearchAggregationBuilderFactory
		implements SearchAggregationBuilderFactory<StubQueryElementCollector> {

	private final StubSearchIndexScope scope;

	public StubSearchAggregationBuilderFactory(StubSearchIndexScope scope) {
		this.scope = scope;
	}

	@Override
	public <A> void contribute(StubQueryElementCollector collector, AggregationKey<A> key,
			SearchAggregation<A> aggregation) {
		// Just check the type and simulate collection
		((StubSearchAggregation) aggregation).simulateBuild();
		collector.simulateCollectCall();
	}

	@Override
	public <T> TermsAggregationBuilder<T> createTermsAggregationBuilder(String absoluteFieldPath, Class<T> expectedType,
			ValueConvert convert) {
		return scope.field( absoluteFieldPath ).queryElement( AggregationTypeKeys.TERMS, scope )
				.type( expectedType, convert );
	}

	@Override
	public <T> RangeAggregationBuilder<T> createRangeAggregationBuilder(String absoluteFieldPath, Class<T> expectedType,
			ValueConvert convert) {
		return scope.field( absoluteFieldPath ).queryElement( AggregationTypeKeys.RANGE, scope )
				.type( expectedType, convert );
	}

}
