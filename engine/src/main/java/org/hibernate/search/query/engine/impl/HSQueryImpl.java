/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.query.engine.impl;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.FullTextFilter;
import org.hibernate.search.ProjectionConstants;
import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.FieldCacheType;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.impl.FilterDef;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.filter.StandardFilterKey;
import org.hibernate.search.filter.impl.ChainedFilter;
import org.hibernate.search.filter.FilterKey;
import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.filter.ShardSensitiveOnlyFilter;
import org.hibernate.search.filter.impl.CachingWrapperFilter;
import org.hibernate.search.filter.impl.FullTextFilterImpl;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.query.collector.impl.FieldCacheCollectorFactory;
import org.hibernate.search.query.engine.spi.DocumentExtractor;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.engine.spi.TimeoutExceptionFactory;
import org.hibernate.search.query.engine.spi.TimeoutManager;
import org.hibernate.search.reader.impl.MultiReaderFactory;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import static org.hibernate.search.util.impl.CollectionHelper.newHashMap;
import static org.hibernate.search.util.impl.FilterCacheModeTypeHelper.cacheInstance;
import static org.hibernate.search.util.impl.FilterCacheModeTypeHelper.cacheResults;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Hardy Ferentschik <hardy@hibernate.org>
 */
public class HSQueryImpl implements HSQuery, Serializable {
	private static final Log log = LoggerFactory.make();
	private static final FullTextFilterImplementor[] EMPTY_FULL_TEXT_FILTER_IMPLEMENTOR = new FullTextFilterImplementor[0];

	private transient SearchFactoryImplementor searchFactoryImplementor;
	private Query luceneQuery;
	private List<Class<?>> targetedEntities;
	private transient TimeoutManagerImpl timeoutManager;
	private Set<Class<?>> indexedTargetedEntities;
	private boolean allowFieldSelectionInProjection = true;

	/**
	 * The  map of currently active/enabled filters.
	 */
	private final Map<String, FullTextFilterImpl> filterDefinitions = newHashMap();

	/**
	 * Combined chained filter to be applied to the query.
	 */
	private Filter filter;

	/**
	 * User specified filters. Will be combined into a single chained filter {@link #filter}.
	 */
	private Filter userFilter;
	private Sort sort;
	private String[] projectedFields;
	private int firstResult;
	private int maxResults;
	private boolean definedMaxResults = false;
	private transient Set<Class<?>> classesAndSubclasses;
	//optimization: if we can avoid the filter clause (we can most of the time) do it as it has a significant perf impact
	private boolean needClassFilterClause;
	private Set<String> idFieldNames;
	private boolean useFieldCacheOnClassTypes = false;
	private transient FacetManagerImpl facetManager;
	private transient TimeoutExceptionFactory timeoutExceptionFactory;
	private Coordinates spatialSearchCenter = null;
	private String spatialFieldName = null;

	/**
	 * The number of results for this query. This field gets populated once {@link #queryResultSize}, {@link #queryEntityInfos}
	 * or {@link #queryDocumentExtractor} is called.
	 */
	private Integer resultSize;


	public HSQueryImpl(SearchFactoryImplementor searchFactoryImplementor) {
		this.searchFactoryImplementor = searchFactoryImplementor;
		this.timeoutExceptionFactory = searchFactoryImplementor.getDefaultTimeoutExceptionFactory();
	}

	@Override
	public void afterDeserialise(SearchFactoryImplementor searchFactoryImplementor) {
		this.searchFactoryImplementor = searchFactoryImplementor;
	}

	@Override
	public HSQuery setSpatialParameters(Coordinates center, String fieldName) {
		spatialSearchCenter = center;
		spatialFieldName = fieldName;
		return this;
	}

	@Override
	public HSQuery luceneQuery(Query query) {
		clearCachedResults();
		this.luceneQuery = query;
		return this;
	}

	@Override
	public HSQuery targetedEntities(List<Class<?>> classes) {
		clearCachedResults();
		this.targetedEntities = classes == null ? new ArrayList<Class<?>>( 0 ) : new ArrayList<Class<?>>( classes );
		final Class[] classesAsArray = targetedEntities.toArray( new Class[targetedEntities.size()] );
		this.indexedTargetedEntities = searchFactoryImplementor.getIndexedTypesPolymorphic( classesAsArray );
		if ( targetedEntities.size() > 0 && indexedTargetedEntities.size() == 0 ) {
			String msg = "None of the specified entity types or any of their subclasses are indexed.";
			throw new IllegalArgumentException( msg );
		}
		return this;
	}

	@Override
	public HSQuery sort(Sort sort) {
		this.sort = sort;
		return this;
	}

	@Override
	public HSQuery filter(Filter filter) {
		clearCachedResults();
		this.userFilter = filter;
		return this;
	}

	@Override
	public HSQuery timeoutExceptionFactory(TimeoutExceptionFactory exceptionFactory) {
		this.timeoutExceptionFactory = exceptionFactory;
		return this;
	}

	@Override
	public HSQuery projection(String... fields) {
		if ( fields == null || fields.length == 0 ) {
			this.projectedFields = null;
		}
		else {
			this.projectedFields = fields;
		}
		return this;
	}

	@Override
	public HSQuery firstResult(int firstResult) {
		if ( firstResult < 0 ) {
			throw new IllegalArgumentException( "'first' pagination parameter less than 0" );
		}
		this.firstResult = firstResult;
		return this;
	}

	@Override
	public HSQuery maxResults(int maxResults) {
		if ( maxResults < 0 ) {
			throw new IllegalArgumentException( "'max' pagination parameter less than 0" );
		}
		this.maxResults = maxResults;
		this.definedMaxResults = true;
		return this;
	}

	/**
	 * List of targeted entities as described by the user
	 */
	@Override
	public List<Class<?>> getTargetedEntities() {
		return targetedEntities;
	}

	/**
	 * Set of indexed entities corresponding to the class hierarchy of the targeted entities
	 */
	@Override
	public Set<Class<?>> getIndexedTargetedEntities() {
		return indexedTargetedEntities;
	}

	@Override
	public String[] getProjectedFields() {
		return projectedFields;
	}

	private TimeoutManagerImpl getTimeoutManagerImpl() {
		if ( timeoutManager == null ) {
			if ( luceneQuery == null ) {
				throw new AssertionFailure( "Requesting TimeoutManager before setting luceneQuery()" );
			}
			timeoutManager = new TimeoutManagerImpl( luceneQuery, timeoutExceptionFactory, this.searchFactoryImplementor.getTimingSource() );
		}
		return timeoutManager;
	}

	@Override
	public TimeoutManager getTimeoutManager() {
		return getTimeoutManagerImpl();
	}

	@Override
	public FacetManagerImpl getFacetManager() {
		if ( facetManager == null ) {
			facetManager = new FacetManagerImpl( this );
		}
		return facetManager;
	}

	@Override
	public Query getLuceneQuery() {
		return luceneQuery;
	}

	@Override
	public List<EntityInfo> queryEntityInfos() {
		IndexSearcherWithPayload searcher = buildSearcher();
		if ( searcher == null ) {
			return Collections.emptyList();
		}
		try {
			QueryHits queryHits = getQueryHits( searcher, calculateTopDocsRetrievalSize() );
			int first = getFirstResultIndex();
			int max = max( first, queryHits.getTotalHits() );

			int size = max - first + 1 < 0 ? 0 : max - first + 1;
			if ( size == 0 ) {
				return Collections.emptyList();
			}
			List<EntityInfo> infos = new ArrayList<EntityInfo>( size );
			DocumentExtractor extractor = buildDocumentExtractor( searcher, queryHits, first, max );
			for ( int index = first; index <= max; index++ ) {
				infos.add( extractor.extract( index ) );
				//TODO should we measure on each extractor?
				if ( index % 10 == 0 ) {
					getTimeoutManager().isTimedOut();
				}
			}
			return infos;
		}
		catch (IOException e) {
			throw new SearchException( "Unable to query Lucene index", e );
		}
		finally {
			closeSearcher( searcher );
		}
	}

	private DocumentExtractor buildDocumentExtractor(IndexSearcherWithPayload searcher, QueryHits queryHits, int first, int max) {
		return new DocumentExtractorImpl(
				queryHits,
				searchFactoryImplementor,
				projectedFields,
				idFieldNames,
				allowFieldSelectionInProjection,
				searcher,
				luceneQuery,
				first,
				max,
				classesAndSubclasses
		);
	}

	/**
	 * DocumentExtractor returns a traverser over the full-text results (EntityInfo)
	 * This operation is lazy bound:
	 * - the query is executed
	 * - results are not retrieved until actually requested
	 *
	 * DocumentExtractor objects *must* be closed when the results are no longer traversed.
	 */
	@Override
	public DocumentExtractor queryDocumentExtractor() {
		//keep the searcher open until the resultset is closed
		//find the directories
		IndexSearcherWithPayload openSearcher = buildSearcher();
		//FIXME: handle null searcher
		try {
			QueryHits queryHits = getQueryHits( openSearcher, calculateTopDocsRetrievalSize() );
			int first = getFirstResultIndex();
			int max = max( first, queryHits.getTotalHits() );
			return buildDocumentExtractor( openSearcher, queryHits, first, max );
		}
		catch (IOException e) {
			closeSearcher( openSearcher );
			throw new SearchException( "Unable to query Lucene index", e );
		}
	}

	@Override
	public int queryResultSize() {
		if ( resultSize == null ) {
			//the timeoutManager does not need to be stopped nor reset as a start does indeed reset
			getTimeoutManager().start();
			//get result size without object initialization
			IndexSearcherWithPayload searcher = buildSearcher( searchFactoryImplementor, false );
			if ( searcher == null ) {
				resultSize = 0;
			}
			else {
				try {
					QueryHits queryHits = getQueryHits( searcher, 0 );
					resultSize = queryHits.getTotalHits();
				}
				catch (IOException e) {
					throw new SearchException( "Unable to query Lucene index", e );
				}
				finally {
					closeSearcher( searcher );
				}
			}
		}
		return this.resultSize;
	}

	@Override
	public Explanation explain(int documentId) {
		//don't use TimeoutManager here as explain is a dev tool when things are weird... or slow :)
		Explanation explanation = null;
		IndexSearcherWithPayload searcher = buildSearcher( searchFactoryImplementor, true );
		if ( searcher == null ) {
			throw new SearchException(
					"Unable to build explanation for document id:"
							+ documentId + ". no index found"
			);
		}
		try {
			org.apache.lucene.search.Query filteredQuery = filterQueryByClasses( luceneQuery );
			buildFilters();
			explanation = searcher.getSearcher().explain( filteredQuery, documentId );
		}
		catch (IOException e) {
			throw new SearchException( "Unable to query Lucene index and build explanation", e );
		}
		finally {
			closeSearcher( searcher );
		}
		return explanation;
	}

	@Override
	public FullTextFilter enableFullTextFilter(String name) {
		clearCachedResults();
		FullTextFilterImpl filterDefinition = filterDefinitions.get( name );
		if ( filterDefinition != null ) {
			return filterDefinition;
		}

		filterDefinition = new FullTextFilterImpl();
		filterDefinition.setName( name );
		FilterDef filterDef = searchFactoryImplementor.getFilterDefinition( name );
		if ( filterDef == null ) {
			throw log.unknownFullTextFilter( name );
		}
		filterDefinitions.put( name, filterDefinition );
		return filterDefinition;
	}

	@Override
	public void disableFullTextFilter(String name) {
		clearCachedResults();
		filterDefinitions.remove( name );
	}

	private void closeSearcher(IndexSearcherWithPayload searcherWithPayload) {
		if ( searcherWithPayload == null ) {
			return;
		}
		searcherWithPayload.closeSearcher( luceneQuery, searchFactoryImplementor );
	}

	/**
	 * This class caches some of the query results and we need to reset the state in case something in the query
	 * changes (eg a new filter is set).
	 */
	void clearCachedResults() {
		resultSize = null;
	}

	/**
	 * Execute the lucene search and return the matching hits.
	 *
	 * @param searcher The index searcher.
	 * @param n Number of documents to retrieve
	 *
	 * @return An instance of <code>QueryHits</code> wrapping the Lucene query and the matching documents.
	 *
	 * @throws IOException in case there is an error executing the lucene search.
	 */
	private QueryHits getQueryHits(IndexSearcherWithPayload searcher, Integer n) throws IOException {
		org.apache.lucene.search.Query filteredQuery = filterQueryByClasses( luceneQuery );
		buildFilters();
		QueryHits queryHits;

		boolean stats = searchFactoryImplementor.getStatistics().isStatisticsEnabled();
		long startTime = 0;
		if ( stats ) {
			startTime = System.nanoTime();
		}

		if ( n == null ) { // try to make sure that we get the right amount of top docs
			queryHits = new QueryHits(
					searcher,
					filteredQuery,
					filter,
					sort,
					getTimeoutManagerImpl(),
					facetManager.getFacetRequests(),
					useFieldCacheOnTypes(),
					getAppropriateIdFieldCollectorFactory(),
					this.timeoutExceptionFactory,
					spatialSearchCenter,
					spatialFieldName
			);
		}
		else if ( 0 == n) {
			queryHits = new QueryHits(
					searcher,
					filteredQuery,
					filter,
					null,
					0,
					getTimeoutManagerImpl(),
					null,
					false,
					null,
					this.timeoutExceptionFactory,
					spatialSearchCenter,
					spatialFieldName
			);
		}
		else {
			queryHits = new QueryHits(
					searcher,
					filteredQuery,
					filter,
					sort,
					n,
					getTimeoutManagerImpl(),
					facetManager.getFacetRequests(),
					useFieldCacheOnTypes(),
					getAppropriateIdFieldCollectorFactory(),
					this.timeoutExceptionFactory,
					spatialSearchCenter,
					spatialFieldName
			);
		}
		resultSize = queryHits.getTotalHits();

		if ( stats ) {
			searchFactoryImplementor.getStatisticsImplementor()
					.searchExecuted( filteredQuery.toString(), System.nanoTime() - startTime );
		}
		facetManager.setFacetResults( queryHits.getFacets() );
		return queryHits;
	}

	/**
	 * @return Calculates the number of <code>TopDocs</code> which should be retrieved as part of the query. If Hibernate's
	 *         pagination parameters are set returned value is <code>first + maxResults</code>. Otherwise <code>null</code> is
	 *         returned.
	 */
	private Integer calculateTopDocsRetrievalSize() {
		if ( ! definedMaxResults ) {
			return null;
		}
		else {
			long tmpMaxResult = (long) getFirstResultIndex() + maxResults;
			if ( tmpMaxResult >= Integer.MAX_VALUE ) {
				// don't return just Integer.MAX_VALUE due to a bug in Lucene - see HSEARCH-330
				return Integer.MAX_VALUE - 1;
			}
			if ( tmpMaxResult == 0 ) {
				return 1; // Lucene enforces that at least one top doc will be retrieved. See also HSEARCH-604
			}
			else {
				return (int) tmpMaxResult;
			}
		}
	}

	private int getFirstResultIndex() {
		return firstResult;
	}

	private IndexSearcherWithPayload buildSearcher() {
		return buildSearcher( searchFactoryImplementor, null );
	}

	/**
	 * Build the index searcher for this fulltext query.
	 *
	 * @param searchFactoryImplementor the search factory.
	 * @param forceScoring if true, force SCORE computation, if false, force not to compute score, if null used best choice
	 *
	 * @return the <code>IndexSearcher</code> for this query (can be <code>null</code>.
	 *         TODO change classesAndSubclasses by side effect, which is a mismatch with the Searcher return, fix that.
	 */
	private IndexSearcherWithPayload buildSearcher(SearchFactoryImplementor searchFactoryImplementor, Boolean forceScoring) {
		Map<Class<?>, EntityIndexBinding> builders = searchFactoryImplementor.getIndexBindings();
		List<IndexManager> targetedIndexes = new ArrayList<IndexManager>();
		Set<String> idFieldNames = new HashSet<String>();

		Similarity searcherSimilarity = null;
		//TODO check if caching this work for the last n list of indexedTargetedEntities makes a perf boost
		if ( indexedTargetedEntities.size() == 0 ) {
			// empty indexedTargetedEntities array means search over all indexed entities,
			// but we have to make sure there is at least one
			if ( builders.isEmpty() ) {
				throw new SearchException(
						"There are no mapped entities. Don't forget to add @Indexed to at least one class."
				);
			}

			for ( EntityIndexBinding entityIndexBinding : builders.values() ) {
				DocumentBuilderIndexedEntity<?> builder = entityIndexBinding.getDocumentBuilder();
				searcherSimilarity = checkSimilarity( searcherSimilarity, entityIndexBinding.getSimilarity() );
				if ( builder.getIdKeywordName() != null ) {
					idFieldNames.add( builder.getIdKeywordName() );
					allowFieldSelectionInProjection = allowFieldSelectionInProjection && builder.allowFieldSelectionInProjection();
				}
				useFieldCacheOnClassTypes = useFieldCacheOnClassTypes || builder.getFieldCacheOption()
						.contains( FieldCacheType.CLASS );
				populateIndexManagers( targetedIndexes, entityIndexBinding.getSelectionStrategy() );
			}
			classesAndSubclasses = null;
		}
		else {
			Set<Class<?>> involvedClasses = new HashSet<Class<?>>( indexedTargetedEntities.size() );
			involvedClasses.addAll( indexedTargetedEntities );
			for ( Class<?> clazz : indexedTargetedEntities ) {
				EntityIndexBinding indexBinder = builders.get( clazz );
				if ( indexBinder != null ) {
					DocumentBuilderIndexedEntity<?> builder = indexBinder.getDocumentBuilder();
					involvedClasses.addAll( builder.getMappedSubclasses() );
				}
			}

			for ( Class clazz : involvedClasses ) {
				EntityIndexBinding entityIndexBinding = builders.get( clazz );
				//TODO should we rather choose a polymorphic path and allow non mapped entities
				if ( entityIndexBinding == null ) {
					throw new SearchException( "Not a mapped entity (don't forget to add @Indexed): " + clazz );
				}
				DocumentBuilderIndexedEntity<?> builder = entityIndexBinding.getDocumentBuilder();
				if ( builder.getIdKeywordName() != null ) {
					idFieldNames.add( builder.getIdKeywordName() );
					allowFieldSelectionInProjection = allowFieldSelectionInProjection && builder.allowFieldSelectionInProjection();
				}
				searcherSimilarity = checkSimilarity( searcherSimilarity, entityIndexBinding.getSimilarity() );
				useFieldCacheOnClassTypes = useFieldCacheOnClassTypes || builder.getFieldCacheOption()
						.contains( FieldCacheType.CLASS );
				populateIndexManagers( targetedIndexes, entityIndexBinding.getSelectionStrategy() );
			}
			this.classesAndSubclasses = involvedClasses;
		}
		this.idFieldNames = idFieldNames;

		//compute optimization needClassFilterClause
		//if at least one DP contains one class that is not part of the targeted classesAndSubclasses we can't optimize
		if ( classesAndSubclasses != null ) {
			for ( IndexManager indexManager : targetedIndexes ) {
				final Set<Class<?>> classesInIndexManager = indexManager.getContainedTypes();
				// if an IndexManager contains only one class, we know for sure it's part of classesAndSubclasses
				if ( classesInIndexManager.size() > 1 ) {
					//risk of needClassFilterClause
					for ( Class clazz : classesInIndexManager ) {
						if ( !classesAndSubclasses.contains( clazz ) ) {
							this.needClassFilterClause = true;
							break;
						}
					}
				}
				if ( this.needClassFilterClause ) {
					break;
				}
			}
		}
		else {
			this.classesAndSubclasses = searchFactoryImplementor.getIndexedTypes();
		}

		//set up the searcher
		final IndexManager[] indexManagers = targetedIndexes.toArray(
				new IndexManager[targetedIndexes.size()]
		);
		IndexSearcher is = new IndexSearcher(
				MultiReaderFactory.openReader( indexManagers )
		);
		is.setSimilarity( searcherSimilarity );

		//handle the sort and projection
		final String[] projection = this.projectedFields;
		if ( Boolean.TRUE.equals( forceScoring ) ) {
			return new IndexSearcherWithPayload( is, true, true );
		}
		else if ( Boolean.FALSE.equals( forceScoring ) ) {
			return new IndexSearcherWithPayload( is, false, false );
		}
		else if ( this.sort != null && projection != null ) {
			boolean activate = false;
			for ( String field : projection ) {
				if ( SCORE.equals( field ) ) {
					activate = true;
					break;
				}
			}
			if ( activate ) {
				return new IndexSearcherWithPayload( is, true, false );
			}
		}
		//default
		return new IndexSearcherWithPayload( is, false, false );
	}

	private Similarity checkSimilarity(Similarity similarity, Similarity entitySimilarity) {
		if ( similarity == null ) {
			similarity = entitySimilarity;
		}
		else if ( !similarity.getClass().equals( entitySimilarity.getClass() ) ) {
			throw new SearchException(
					"Cannot perform search on two entities with differing Similarity implementations (" + similarity.getClass()
							.getName() + " & " + entitySimilarity.getClass().getName() + ")"
			);
		}

		return similarity;
	}

	private void populateIndexManagers(List<IndexManager> indexManagersTarget, final IndexShardingStrategy indexShardingStrategy) {
		final IndexManager[] indexManagersForQuery;
		if ( filterDefinitions != null && !filterDefinitions.isEmpty() ) {
			indexManagersForQuery = indexShardingStrategy.getIndexManagersForQuery(
					filterDefinitions.values().toArray( new FullTextFilterImplementor[filterDefinitions.size()] )
			);
		}
		else {
			//no filter get all shards
			indexManagersForQuery = indexShardingStrategy.getIndexManagersForQuery( EMPTY_FULL_TEXT_FILTER_IMPLEMENTOR );
		}

		for ( IndexManager indexManager : indexManagersForQuery ) {
			if ( !indexManagersTarget.contains( indexManager ) ) {
				indexManagersTarget.add( indexManager );
			}
		}
	}

	private void buildFilters() {
		ChainedFilter chainedFilter = new ChainedFilter();
		if ( !filterDefinitions.isEmpty() ) {
			for ( FullTextFilterImpl fullTextFilter : filterDefinitions.values() ) {
				Filter filter = buildLuceneFilter( fullTextFilter );
				if ( filter != null ) {
					chainedFilter.addFilter( filter );
				}
			}
		}

		if ( userFilter != null ) {
			chainedFilter.addFilter( userFilter );
		}

		if ( getFacetManager().getFacetFilter() != null ) {
			chainedFilter.addFilter( facetManager.getFacetFilter() );
		}

		if ( chainedFilter.isEmpty() ) {
			filter = null;
		}
		else {
			filter = chainedFilter;
		}
	}

	/**
	 * Builds a Lucene filter using the given <code>FullTextFilter</code>.
	 *
	 * @param fullTextFilter the Hibernate specific <code>FullTextFilter</code> used to create the
	 * Lucene <code>Filter</code>.
	 *
	 * @return the Lucene filter mapped to the filter definition
	 */
	private Filter buildLuceneFilter(FullTextFilterImpl fullTextFilter) {

		/*
		 * FilterKey implementations and Filter(Factory) do not have to be threadsafe wrt their parameter injection
		 * as FilterCachingStrategy ensure a memory barrier between concurrent thread calls
		 */
		FilterDef def = searchFactoryImplementor.getFilterDefinition( fullTextFilter.getName() );
		//def can never be null, ti's guarded by enableFullTextFilter(String)

		if ( isPreQueryFilterOnly( def ) ) {
			return null;
		}

		Object instance = createFilterInstance( fullTextFilter, def );
		FilterKey key = createFilterKey( def, instance );

		// try to get the filter out of the cache
		Filter filter = cacheInstance( def.getCacheMode() ) ?
				searchFactoryImplementor.getFilterCachingStrategy().getCachedFilter( key ) :
				null;

		if ( filter == null ) {
			filter = createFilter( def, instance );

			// add filter to cache if we have to
			if ( cacheInstance( def.getCacheMode() ) ) {
				searchFactoryImplementor.getFilterCachingStrategy().addCachedFilter( key, filter );
			}
		}
		return filter;
	}

	private boolean isPreQueryFilterOnly(FilterDef def) {
		return def.getImpl().equals( ShardSensitiveOnlyFilter.class );
	}

	private Filter createFilter(FilterDef def, Object instance) {
		Filter filter;
		if ( def.getFactoryMethod() != null ) {
			try {
				filter = (Filter) def.getFactoryMethod().invoke( instance );
			}
			catch (IllegalAccessException e) {
				throw new SearchException(
						"Unable to access @Factory method: "
								+ def.getImpl().getName() + "." + def.getFactoryMethod().getName(), e
				);
			}
			catch (InvocationTargetException e) {
				throw new SearchException(
						"Unable to access @Factory method: "
								+ def.getImpl().getName() + "." + def.getFactoryMethod().getName(), e
				);
			}
			catch (ClassCastException e) {
				throw new SearchException(
						"@Key method does not return a org.apache.lucene.search.Filter class: "
								+ def.getImpl().getName() + "." + def.getFactoryMethod().getName(), e
				);
			}
		}
		else {
			try {
				filter = (Filter) instance;
			}
			catch (ClassCastException e) {
				throw new SearchException(
						"Filter implementation does not implement the Filter interface: "
								+ def.getImpl().getName() + ". "
								+ ( def.getFactoryMethod() != null ? def.getFactoryMethod().getName() : "" ), e
				);
			}
		}

		filter = addCachingWrapperFilter( filter, def );
		return filter;
	}

	/**
	 * Decides whether to wrap the given filter around a <code>CachingWrapperFilter<code>.
	 *
	 * @param filter the filter which maybe gets wrapped.
	 * @param def The filter definition used to decide whether wrapping should occur or not.
	 *
	 * @return The original filter or wrapped filter depending on the information extracted from
	 *         <code>def</code>.
	 */
	private Filter addCachingWrapperFilter(Filter filter, FilterDef def) {
		if ( cacheResults( def.getCacheMode() ) ) {
			int cachingWrapperFilterSize = searchFactoryImplementor.getFilterCacheBitResultsSize();
			filter = new CachingWrapperFilter( filter, cachingWrapperFilterSize );
		}

		return filter;
	}

	private FilterKey createFilterKey(FilterDef def, Object instance) {
		FilterKey key = null;
		if ( !cacheInstance( def.getCacheMode() ) ) {
			return key; // if the filter is not cached there is no key!
		}

		if ( def.getKeyMethod() == null ) {
			key = new FilterKey() {
				@Override
				public int hashCode() {
					return getImpl().hashCode();
				}

				@Override
				public boolean equals(Object obj) {
					if ( !( obj instanceof FilterKey ) ) {
						return false;
					}
					FilterKey that = (FilterKey) obj;
					return this.getImpl().equals( that.getImpl() );
				}
			};
		}
		else {
			try {
				key = (FilterKey) def.getKeyMethod().invoke( instance );
			}
			catch (IllegalAccessException e) {
				throw new SearchException(
						"Unable to access @Key method: "
								+ def.getImpl().getName() + "." + def.getKeyMethod().getName()
				);
			}
			catch (InvocationTargetException e) {
				throw new SearchException(
						"Unable to access @Key method: "
								+ def.getImpl().getName() + "." + def.getKeyMethod().getName()
				);
			}
			catch (ClassCastException e) {
				throw new SearchException(
						"@Key method does not return FilterKey: "
								+ def.getImpl().getName() + "." + def.getKeyMethod().getName()
				);
			}
		}
		key.setImpl( def.getImpl() );

		//Make sure Filters are isolated by filter def name
		StandardFilterKey wrapperKey = new StandardFilterKey();
		wrapperKey.addParameter( def.getName() );
		wrapperKey.addParameter( key );
		return wrapperKey;
	}

	private Object createFilterInstance(FullTextFilterImpl fullTextFilter, FilterDef def) {
		final Object instance = ClassLoaderHelper.instanceFromClass( Object.class, def.getImpl(), "@FullTextFilterDef" );
		for ( Map.Entry<String, Object> entry : fullTextFilter.getParameters().entrySet() ) {
			def.invoke( entry.getKey(), instance, entry.getValue() );
		}
		if ( cacheInstance( def.getCacheMode() ) && def.getKeyMethod() == null && fullTextFilter.getParameters()
				.size() > 0 ) {
			throw new SearchException( "Filter with parameters and no @Key method: " + fullTextFilter.getName() );
		}
		return instance;
	}

	private org.apache.lucene.search.Query filterQueryByClasses(org.apache.lucene.search.Query luceneQuery) {
		if ( !needClassFilterClause ) {
			return luceneQuery;
		}
		else {
			//A query filter is more practical than a manual class filtering post query (esp on scrollable resultsets)
			//it also probably minimise the memory footprint
			BooleanQuery classFilter = new BooleanQuery();
			//annihilate the scoring impact of DocumentBuilderIndexedEntity.CLASS_FIELDNAME
			classFilter.setBoost( 0 );
			for ( Class clazz : classesAndSubclasses ) {
				Term t = new Term( ProjectionConstants.OBJECT_CLASS, clazz.getName() );
				TermQuery termQuery = new TermQuery( t );
				classFilter.add( termQuery, BooleanClause.Occur.SHOULD );
			}
			BooleanQuery filteredQuery = new BooleanQuery();
			filteredQuery.add( luceneQuery, BooleanClause.Occur.MUST );
			filteredQuery.add( classFilter, BooleanClause.Occur.MUST );
			return filteredQuery;
		}
	}

	private int max(int first, int totalHits) {
		if ( ! definedMaxResults ) {
			return totalHits - 1;
		}
		else {
			return maxResults + first < totalHits ?
					first + maxResults - 1 :
					totalHits - 1;
		}
	}

	@Override
	public SearchFactoryImplementor getSearchFactoryImplementor() {
		return searchFactoryImplementor;
	}

	private boolean useFieldCacheOnTypes() {
		if ( classesAndSubclasses.size() <= 1 ) {
			// force it to false, as we won't need classes at all
			return false;
		}
		return useFieldCacheOnClassTypes;
	}

	/**
	 * @return The FieldCacheCollectorFactory to use for this query, or null to not use FieldCaches
	 */
	private FieldCacheCollectorFactory getAppropriateIdFieldCollectorFactory() {
		Map<Class<?>, EntityIndexBinding> builders = searchFactoryImplementor.getIndexBindings();
		Set<FieldCacheCollectorFactory> allCollectors = new HashSet<FieldCacheCollectorFactory>();
		// we need all documentBuilder to agree on type, fieldName, and enabling the option:
		FieldCacheCollectorFactory anyImplementation = null;
		for ( Class<?> clazz : classesAndSubclasses ) {
			EntityIndexBinding docBuilder = builders.get( clazz );
			FieldCacheCollectorFactory fieldCacheCollectionFactory = docBuilder.getIdFieldCacheCollectionFactory();
			if ( fieldCacheCollectionFactory == null ) {
				// some implementation disable it, so we won't use it
				return null;
			}
			anyImplementation = fieldCacheCollectionFactory;
			allCollectors.add( fieldCacheCollectionFactory );
		}
		if ( allCollectors.size() != 1 ) {
			// some implementations have different requirements
			return null;
		}
		else {
			// they are all the same, return any:
			return anyImplementation;
		}
	}
}
