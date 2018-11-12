/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.impl;

import java.util.List;

import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.spi.HitAggregator;
import org.hibernate.search.engine.search.query.spi.ReferenceHitCollector;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.StubHitExtractor;

public class StubReferenceHitExtractorImpl<T> implements StubHitExtractor<DocumentReference, T> {

	private final HitAggregator<ReferenceHitCollector, T> aggregator;

	public StubReferenceHitExtractorImpl(HitAggregator<ReferenceHitCollector, T> aggregator) {
		this.aggregator = aggregator;
	}

	@Override
	public T extract(List<DocumentReference> hits) {
		aggregator.init( hits.size() );
		for ( DocumentReference hit : hits ) {
			ReferenceHitCollector collector = aggregator.nextCollector();
			collector.collectReference( hit );
		}
		return aggregator.build();
	}

}
