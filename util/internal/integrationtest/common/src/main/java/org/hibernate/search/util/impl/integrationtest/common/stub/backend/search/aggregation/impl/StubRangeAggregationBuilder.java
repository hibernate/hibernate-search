/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.aggregation.impl;

import java.util.Map;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.util.common.data.Range;

class StubRangeAggregationBuilder<K>
		implements StubAggregationBuilder<Map<Range<K>, Long>>, RangeAggregationBuilder<K> {

	@Override
	public void range(Range<? extends K> range) {
		// No-op
	}

	@Override
	public SearchAggregation<Map<Range<K>, Long>> build() {
		return new StubSearchAggregation<>( this );
	}
}
