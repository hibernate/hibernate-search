/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.util.function.Function;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneQueryWorkOrchestrator;
import org.hibernate.search.backend.lucene.search.extraction.impl.ReusableDocumentStoredFieldVisitor;
import org.hibernate.search.backend.lucene.search.impl.LuceneQueries;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchQueryElementCollector;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchTargetModel;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
import org.hibernate.search.engine.search.query.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;

class LuceneSearchQueryBuilder<T> implements SearchQueryBuilder<T, LuceneSearchQueryElementCollector> {

	private final LuceneWorkFactory workFactory;
	private final LuceneQueryWorkOrchestrator queryOrchestrator;
	private final MultiTenancyStrategy multiTenancyStrategy;

	private final LuceneSearchTargetModel searchTargetModel;
	private final SessionContextImplementor sessionContext;

	private final ReusableDocumentStoredFieldVisitor storedFieldVisitor;
	private final ProjectionHitMapper<?, ?> projectionHitMapper;
	private final LuceneSearchProjection<?, T> rootProjection;
	private final LuceneSearchQueryElementCollector elementCollector;

	LuceneSearchQueryBuilder(
			LuceneWorkFactory workFactory,
			LuceneQueryWorkOrchestrator queryOrchestrator,
			MultiTenancyStrategy multiTenancyStrategy,
			LuceneSearchTargetModel searchTargetModel,
			SessionContextImplementor sessionContext,
			ReusableDocumentStoredFieldVisitor storedFieldVisitor,
			ProjectionHitMapper<?, ?> projectionHitMapper,
			LuceneSearchProjection<?, T> rootProjection) {
		this.workFactory = workFactory;
		this.queryOrchestrator = queryOrchestrator;
		this.multiTenancyStrategy = multiTenancyStrategy;

		this.searchTargetModel = searchTargetModel;
		this.sessionContext = sessionContext;

		this.elementCollector = new LuceneSearchQueryElementCollector();
		this.storedFieldVisitor = storedFieldVisitor;
		this.projectionHitMapper = projectionHitMapper;
		this.rootProjection = rootProjection;
	}

	@Override
	public LuceneSearchQueryElementCollector getQueryElementCollector() {
		return elementCollector;
	}

	@Override
	public void addRoutingKey(String routingKey) {
		// TODO see what to do with the routing key
		throw new UnsupportedOperationException( "Routing keys are not supported by the Lucene backend yet." );
	}

	private IndexSearchQuery<T> build() {
		LuceneSearchResultExtractor<T> searchResultExtractor = new LuceneSearchResultExtractorImpl<>(
				storedFieldVisitor, rootProjection, projectionHitMapper
		);

		BooleanQuery.Builder luceneQueryBuilder = new BooleanQuery.Builder();
		luceneQueryBuilder.add( elementCollector.toLuceneQueryPredicate(), Occur.MUST );
		luceneQueryBuilder.add( LuceneQueries.mainDocumentQuery(), Occur.FILTER );

		return new LuceneIndexSearchQuery<>(
				queryOrchestrator, workFactory,
				searchTargetModel.getIndexNames(), searchTargetModel.getReaderProviders(),
				sessionContext,
				multiTenancyStrategy.decorateLuceneQuery( luceneQueryBuilder.build(), sessionContext.getTenantIdentifier() ),
				elementCollector.toLuceneSort(),
				rootProjection, searchResultExtractor
		);
	}

	@Override
	public <Q> Q build(Function<IndexSearchQuery<T>, Q> searchQueryWrapperFactory) {
		return searchQueryWrapperFactory.apply( build() );
	}
}
