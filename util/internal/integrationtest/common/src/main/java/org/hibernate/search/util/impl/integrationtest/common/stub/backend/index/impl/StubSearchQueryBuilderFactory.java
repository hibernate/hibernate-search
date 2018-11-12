/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.query.spi.ReferenceHitCollector;
import org.hibernate.search.engine.search.query.spi.HitAggregator;
import org.hibernate.search.engine.search.query.spi.LoadingHitCollector;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilderFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubQueryElementCollector;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubSearchQueryBuilder;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubSearchWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.impl.StubSearchTargetModel;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.impl.StubObjectHitExtractorImpl;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.impl.StubProjectionHitExtractorImpl;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.impl.StubReferenceHitExtractorImpl;

class StubSearchQueryBuilderFactory implements SearchQueryBuilderFactory<StubQueryElementCollector> {
	private final StubBackend backend;
	private final StubSearchTargetModel targetModel;

	StubSearchQueryBuilderFactory(StubBackend backend, StubSearchTargetModel targetModel) {
		this.backend = backend;
		this.targetModel = targetModel;
	}

	@Override
	public <O> SearchQueryBuilder<O, StubQueryElementCollector> asObject(SessionContextImplementor sessionContext,
			HitAggregator<LoadingHitCollector, List<O>> hitAggregator) {
		return new StubSearchQueryBuilder<>(
				backend, targetModel, StubSearchWork.ResultType.OBJECTS,
				new StubObjectHitExtractorImpl<>( hitAggregator )
		);
	}

	@Override
	public <T> SearchQueryBuilder<T, StubQueryElementCollector> asReference(SessionContextImplementor sessionContext,
			HitAggregator<ReferenceHitCollector, List<T>> hitAggregator) {
		return new StubSearchQueryBuilder<>(
				backend, targetModel, StubSearchWork.ResultType.REFERENCES,
				new StubReferenceHitExtractorImpl<>( hitAggregator )
		);
	}

	@Override
	public <T> SearchQueryBuilder<T, StubQueryElementCollector> asProjections(SessionContextImplementor sessionContext,
			HitAggregator<ProjectionHitCollector, List<T>> hitAggregator, SearchProjection<?>... projections) {
		List<StubSearchProjection<?>> castedProjections =
				Arrays.stream( projections ).map( p -> (StubSearchProjection<?>) p ).collect( Collectors.toList() );
		return new StubSearchQueryBuilder<>(
				backend, targetModel, StubSearchWork.ResultType.PROJECTIONS,
				new StubProjectionHitExtractorImpl<>( hitAggregator, castedProjections, sessionContext )
		);
	}
}
