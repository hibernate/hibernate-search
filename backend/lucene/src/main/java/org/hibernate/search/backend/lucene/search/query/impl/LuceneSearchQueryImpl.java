/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.HibernateSearchMultiReader;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneSyncWorkOrchestrator;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchIndexContext;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchQuery;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchResult;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchScroll;
import org.hibernate.search.backend.lucene.search.timeout.impl.LuceneTimeoutManager;
import org.hibernate.search.backend.lucene.work.impl.LuceneSearcher;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.backend.lucene.work.impl.ReadWork;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.query.SearchQueryExtension;
import org.hibernate.search.engine.search.query.spi.AbstractSearchQuery;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;


public class LuceneSearchQueryImpl<H> extends AbstractSearchQuery<H, LuceneSearchResult<H>>
		implements LuceneSearchQuery<H> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSyncWorkOrchestrator queryOrchestrator;
	private final LuceneWorkFactory workFactory;
	private final LuceneSearchContext searchContext;
	private final BackendSessionContext sessionContext;
	private final LoadingContext<?, ?> loadingContext;
	private final Set<String> routingKeys;
	private final Query luceneQuery;
	private final Sort luceneSort;
	private final LuceneSearcher<LuceneLoadableSearchResult<H>, LuceneExtractableSearchResult<H>> searcher;
	private final Integer totalHitsThreshold;

	private LuceneTimeoutManager timeoutManager;

	LuceneSearchQueryImpl(LuceneSyncWorkOrchestrator queryOrchestrator,
			LuceneWorkFactory workFactory, LuceneSearchContext searchContext,
			BackendSessionContext sessionContext,
			LoadingContext<?, ?> loadingContext,
			Set<String> routingKeys,
			LuceneTimeoutManager timeoutManager,
			Query luceneQuery, Sort luceneSort,
			LuceneSearcher<LuceneLoadableSearchResult<H>, LuceneExtractableSearchResult<H>> searcher,
			Integer totalHitsThreshold) {
		this.queryOrchestrator = queryOrchestrator;
		this.workFactory = workFactory;
		this.searchContext = searchContext;
		this.sessionContext = sessionContext;
		this.loadingContext = loadingContext;
		this.routingKeys = routingKeys;
		this.timeoutManager = timeoutManager;
		this.luceneQuery = luceneQuery;
		this.luceneSort = luceneSort;
		this.searcher = searcher;
		this.totalHitsThreshold = totalHitsThreshold;
	}

	@Override
	public String queryString() {
		return luceneQuery.toString();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[query=" + queryString() + ", sort=" + luceneSort + "]";
	}

	@Override
	public <Q> Q extension(SearchQueryExtension<Q, H> extension) {
		return DslExtensionState.returnIfSupported(
				extension, extension.extendOptional( this, loadingContext )
		);
	}

	@Override
	public LuceneSearchResult<H> fetch(Integer offset, Integer limit) {
		return doFetch( offset, limit, false );
	}

	@Override
	public List<H> fetchHits(Integer offset, Integer limit) {
		return doFetch( offset, limit, true ).hits();
	}

	@Override
	public long fetchTotalHitCount() {
		timeoutManager.start();
		ReadWork<Integer> work = workFactory.count( searcher );
		Integer result = doSubmit( work );
		timeoutManager.stop();
		return result;
	}

	@Override
	public LuceneSearchScroll<H> scroll(int chunkSize) {
		Set<String> indexNames = searchContext.indexes().indexNames();
		HibernateSearchMultiReader indexReader = HibernateSearchMultiReader.open(
				indexNames, searchContext.indexes().elements(), routingKeys );
		return new LuceneSearchScrollImpl<>( queryOrchestrator, workFactory, searchContext, routingKeys, timeoutManager,
				searcher, indexReader, chunkSize
		);
	}

	@Override
	public Explanation explain(Object id) {
		Contracts.assertNotNull( id, "id" );

		Map<String, ? extends LuceneSearchIndexContext> mappedTypeNameToIndex =
				searchContext.indexes().mappedTypeNameToIndex();
		if ( mappedTypeNameToIndex.size() != 1 ) {
			throw log.explainRequiresTypeName( mappedTypeNameToIndex.keySet() );
		}

		Map.Entry<String, ? extends LuceneSearchIndexContext> entry = mappedTypeNameToIndex.entrySet().iterator()
				.next();
		String typeName = entry.getKey();
		LuceneSearchIndexContext index = entry.getValue();
		String documentId = toDocumentId( index, id );

		return doExplain( typeName, documentId );
	}

	@Override
	public Explanation explain(String typeName, Object id) {
		Contracts.assertNotNull( typeName, "typeName" );
		Contracts.assertNotNull( id, "id" );

		Map<String, ? extends LuceneSearchIndexContext> mappedTypeNameToIndex =
				searchContext.indexes().mappedTypeNameToIndex();
		LuceneSearchIndexContext index = mappedTypeNameToIndex.get( typeName );
		if ( !mappedTypeNameToIndex.containsKey( typeName ) ) {
			throw log.explainRequiresTypeTargetedByQuery( mappedTypeNameToIndex.keySet(), typeName );
		}

		String documentId = toDocumentId( index, id );

		return doExplain( typeName, documentId );
	}

	@Override
	public Sort luceneSort() {
		return luceneSort;
	}

	@Override
	public void failAfter(long timeout, TimeUnit timeUnit) {
		// replace the timeout manager on already created query instance
		timeoutManager = searchContext.createTimeoutManager( luceneQuery, timeout, timeUnit, true );
		searcher.setTimeoutManager( timeoutManager );
	}

	private LuceneSearchResult<H> doFetch(Integer offset, Integer limit, boolean skipTotalHitCount) {
		timeoutManager.start();
		ReadWork<LuceneLoadableSearchResult<H>> work = workFactory.search( searcher, offset, limit,
				totalHitsThreshold( skipTotalHitCount )
		);
		LuceneSearchResult<H> result = doSubmit( work )
				/*
				 * WARNING: the following call must run in the user thread.
				 * If we introduce async processing, we will have to add a loadAsync method here,
				 * as well as in ProjectionHitMapper and EntityLoader.
				 * This method may not be easy to implement for blocking mappers,
				 * so we may choose to throw exceptions for those.
				 */
				.loadBlocking();
		timeoutManager.stop();
		return result;
	}

	private Explanation doExplain(String typeName, String id) {
		timeoutManager.start();
		Query filter = searchContext.filterOrNull( sessionContext.tenantIdentifier() );
		ReadWork<Explanation> work = workFactory.explain(
				searcher, typeName, id, filter
		);
		Explanation explanation = doSubmit( work );
		timeoutManager.stop();
		return explanation;
	}

	private <T> T doSubmit(ReadWork<T> work) {
		return queryOrchestrator.submit(
				searchContext.indexes().indexNames(),
				searchContext.indexes().elements(),
				routingKeys,
				work
		);
	}

	private int totalHitsThreshold(boolean skipTotalHitCount) {
		if ( skipTotalHitCount ) {
			return 0;
		}
		if ( totalHitsThreshold == null ) {
			return Integer.MAX_VALUE;
		}
		return totalHitsThreshold;
	}

	private String toDocumentId(LuceneSearchIndexContext index, Object id) {
		ToDocumentIdentifierValueConverter<?> converter = index.idDslConverter();
		ToDocumentIdentifierValueConvertContext convertContext =
				searchContext.toDocumentIdentifierValueConvertContext();
		return converter.convertUnknown( id, convertContext );
	}
}
