/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneReadWorkOrchestrator;
import org.hibernate.search.backend.lucene.search.aggregation.impl.LuceneSearchAggregation;
import org.hibernate.search.backend.lucene.search.extraction.impl.ReusableDocumentStoredFieldVisitor;
import org.hibernate.search.backend.lucene.search.impl.LuceneQueries;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchQueryElementCollector;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchQuery;
import org.hibernate.search.backend.lucene.search.timeout.impl.TimeoutManagerImpl;
import org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl.LuceneFieldComparatorSource;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class LuceneSearchQueryBuilder<H>
		implements SearchQueryBuilder<H, LuceneSearchQueryElementCollector>, LuceneSearchQueryElementCollector {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneWorkFactory workFactory;
	private final LuceneReadWorkOrchestrator queryOrchestrator;

	private final LuceneSearchContext searchContext;
	private final BackendSessionContext sessionContext;
	private final Set<String> routingKeys;

	private final ReusableDocumentStoredFieldVisitor storedFieldVisitor;
	private final LoadingContextBuilder<?, ?> loadingContextBuilder;
	private final LuceneSearchProjection<?, H> rootProjection;

	private List<LuceneFieldComparatorSource> nestedFieldSorts;

	private Query luceneQuery;
	private List<SortField> sortFields;
	private Map<AggregationKey<?>, LuceneSearchAggregation<?>> aggregations;
	private Long timeout;
	private TimeUnit timeUnit;

	public LuceneSearchQueryBuilder(
			LuceneWorkFactory workFactory,
			LuceneReadWorkOrchestrator queryOrchestrator,
			LuceneSearchContext searchContext,
			BackendSessionContext sessionContext,
			ReusableDocumentStoredFieldVisitor storedFieldVisitor,
			LoadingContextBuilder<?, ?> loadingContextBuilder,
			LuceneSearchProjection<?, H> rootProjection) {
		this.workFactory = workFactory;
		this.queryOrchestrator = queryOrchestrator;

		this.searchContext = searchContext;
		this.sessionContext = sessionContext;
		this.routingKeys = new HashSet<>();

		this.storedFieldVisitor = storedFieldVisitor;
		this.loadingContextBuilder = loadingContextBuilder;
		this.rootProjection = rootProjection;
	}

	@Override
	public LuceneSearchQueryElementCollector toQueryElementCollector() {
		return this;
	}

	@Override
	public void addRoutingKey(String routingKey) {
		this.routingKeys.add( routingKey );
	}

	@Override
	public void timeout(long timeout, TimeUnit timeUnit) {
		this.timeout = timeout;
		this.timeUnit = timeUnit;
	}

	@Override
	public void collectPredicate(Query luceneQuery) {
		this.luceneQuery = luceneQuery;
	}

	@Override
	public void collectSortField(SortField sortField) {
		if ( sortFields == null ) {
			sortFields = new ArrayList<>( 5 );
		}
		sortFields.add( sortField );
	}

	@Override
	public void collectSortField(SortField sortField, LuceneFieldComparatorSource nestedFieldSort) {
		collectSortField( sortField );
		if ( nestedFieldSort == null ) {
			return;
		}

		if ( nestedFieldSorts == null ) {
			nestedFieldSorts = new ArrayList<>( 5 );
		}
		nestedFieldSorts.add( nestedFieldSort );
	}

	@Override
	public void collectSortFields(SortField[] sortFields) {
		if ( sortFields == null || sortFields.length == 0 ) {
			return;
		}

		if ( this.sortFields == null ) {
			this.sortFields = new ArrayList<>( sortFields.length );
		}
		Collections.addAll( this.sortFields, sortFields );
	}

	@Override
	public <A> void collectAggregation(AggregationKey<A> key, LuceneSearchAggregation<A> aggregation) {
		if ( aggregations == null ) {
			aggregations = new LinkedHashMap<>();
		}
		Object previous = aggregations.put( key, aggregation );
		if ( previous != null ) {
			throw log.duplicateAggregationKey( key );
		}
	}

	@Override
	public LuceneSearchQuery<H> build() {
		LoadingContext<?, ?> loadingContext = loadingContextBuilder.build();

		BooleanQuery.Builder luceneQueryBuilder = new BooleanQuery.Builder();
		luceneQueryBuilder.add( luceneQuery, Occur.MUST );
		luceneQueryBuilder.add( LuceneQueries.mainDocumentQuery(), Occur.FILTER );

		Sort luceneSort = null;
		if ( sortFields != null && !sortFields.isEmpty() ) {
			luceneSort = new Sort( sortFields.toArray( new SortField[0] ) );
		}

		Query definitiveLuceneQuery = searchContext.decorateLuceneQuery(
				luceneQueryBuilder.build(), sessionContext.getTenantIdentifier()
		);

		if ( nestedFieldSorts != null ) {
			for ( LuceneFieldComparatorSource nestedField : nestedFieldSorts ) {
				nestedField.setOriginalParentQuery( definitiveLuceneQuery );
			}
		}

		LuceneSearchQueryRequestContext requestContext = new LuceneSearchQueryRequestContext(
				sessionContext, loadingContext, definitiveLuceneQuery, luceneSort
		);

		TimeoutManagerImpl timeoutManager = new TimeoutManagerImpl( definitiveLuceneQuery );
		if ( timeout != null && timeUnit != null ) {
			// TODO HSEARCH-3352 make timeout property immutable for a timeout manager
			timeoutManager.setTimeout( timeout, timeUnit );
			// TODO HSEARCH-3352 make type property immutable as well
			// TODO HSEARCH-3352 allow to use the other strategy: limitFetchingOnTimeout
			timeoutManager.raiseExceptionOnTimeout();
		}

		LuceneSearcherImpl<H> searcher = new LuceneSearcherImpl<>(
				requestContext,
				storedFieldVisitor,
				rootProjection,
				aggregations == null ? Collections.emptyMap() : aggregations,
				timeoutManager,
				searchContext.getTimingSource()
		);

		return new LuceneSearchQueryImpl<>(
				queryOrchestrator, workFactory,
				searchContext,
				sessionContext,
				loadingContext,
				routingKeys,
				timeoutManager,
				definitiveLuceneQuery,
				luceneSort,
				searcher
		);
	}
}
