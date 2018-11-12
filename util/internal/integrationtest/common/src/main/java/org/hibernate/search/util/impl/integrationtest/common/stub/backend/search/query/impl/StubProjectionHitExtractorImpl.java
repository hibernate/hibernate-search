/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.impl;

import java.util.List;

import org.hibernate.search.engine.backend.document.converter.runtime.spi.FromIndexFieldValueConvertContextImpl;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.query.spi.HitAggregator;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.StubHitExtractor;

public class StubProjectionHitExtractorImpl<T> implements StubHitExtractor<List<?>, T> {

	private final HitAggregator<ProjectionHitCollector, T> aggregator;
	private final List<StubSearchProjection<?>> projections;
	private final FromIndexFieldValueConvertContextImpl convertContext;

	public StubProjectionHitExtractorImpl(HitAggregator<ProjectionHitCollector, T> aggregator,
			List<StubSearchProjection<?>> projections,
			SessionContextImplementor sessionContext) {
		this.aggregator = aggregator;
		this.projections = projections;
		this.convertContext = new FromIndexFieldValueConvertContextImpl( sessionContext );
	}

	@Override
	public T extract(List<List<?>> hits) {
		aggregator.init( hits.size() );

		for ( List<?> hit : hits ) {
			ProjectionHitCollector collector = aggregator.nextCollector();
			int index = 0;
			if ( hit.size() != projections.size() ) {
				throw new IllegalStateException(
						"Illegal StubHit size for projection hits: expected " + projections.size()
								+ ", got these elements: " + hit
				);
			}
			for ( StubSearchProjection<?> projection : projections ) {
				projection.extract( collector, hit.get( index ), convertContext );
				++index;
			}
		}

		return aggregator.build();
	}
}
