/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.impl;

import static org.hibernate.search.util.impl.FilterCacheModeTypeHelper.cacheInstance;
import static org.hibernate.search.util.impl.FilterCacheModeTypeHelper.cacheResults;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.similarities.Similarity;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.engine.impl.FilterDef;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;
import org.hibernate.search.engine.metadata.impl.FacetMetadata;
import org.hibernate.search.engine.metadata.impl.PropertyMetadata;
import org.hibernate.search.engine.metadata.impl.SortableFieldMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.filter.FilterKey;
import org.hibernate.search.filter.StandardFilterKey;
import org.hibernate.search.filter.impl.CachingWrapperQuery;
import org.hibernate.search.filter.impl.DefaultFilterKey;
import org.hibernate.search.filter.impl.FullTextFilterImpl;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.query.engine.spi.DocumentExtractor;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.reader.impl.MultiReaderFactory;
import org.hibernate.search.spi.CustomTypeMetadata;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.IndexedTypeMap;
import org.hibernate.search.spi.IndexedTypeSet;
import org.hibernate.search.util.impl.CollectionHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class LuceneHSQuery extends AbstractHSQuery implements HSQuery {

	static final Log log = LoggerFactory.make();

	private static final Set<String> SUPPORTED_PROJECTION_CONSTANTS = Collections.unmodifiableSet(
			CollectionHelper.asSet(
					ProjectionConstants.DOCUMENT,
					ProjectionConstants.DOCUMENT_ID,
					ProjectionConstants.EXPLANATION,
					ProjectionConstants.ID,
					ProjectionConstants.OBJECT_CLASS,
					ProjectionConstants.SCORE,
					ProjectionConstants.SPATIAL_DISTANCE,
					ProjectionConstants.THIS
			)
	);

	private Query luceneQuery;

	private boolean allowFieldSelectionInProjection = true;

	private transient Map<String, EntityIndexBinding> targetedEntityBindingsByName;
	//optimization: if we can avoid the filter clause (we can most of the time) do it as it has a significant perf impact
	private boolean needClassFilterClause;
	private Set<String> idFieldNames;
	private transient FacetManagerImpl facetManager;

	/**
	 * The number of results for this query. This field gets populated once {@link #queryResultSize}, {@link #queryEntityInfos}
	 * or {@link #queryDocumentExtractor} is called.
	 */
	private Integer resultSize;

	public LuceneHSQuery(Query luceneQuery, ExtendedSearchIntegrator extendedIntegrator,
			IndexedTypeSet types) {
		super( extendedIntegrator, types );
		this.luceneQuery = luceneQuery;
	}

	public LuceneHSQuery(Query luceneQuery, ExtendedSearchIntegrator extendedIntegrator,
			IndexedTypeMap<CustomTypeMetadata> types) {
		super( extendedIntegrator, types );
		this.luceneQuery = luceneQuery;
	}

	@Override
	public HSQuery luceneQuery(Query query) {
		clearCachedResults();
		this.luceneQuery = query;
		return this;
	}

	@Override
	protected TimeoutManagerImpl buildTimeoutManager() {
		if ( luceneQuery == null ) {
			throw new AssertionFailure( "Requesting TimeoutManager before setting luceneQuery()" );
		}

		return new TimeoutManagerImpl(
				luceneQuery, timeoutExceptionFactory, this.extendedIntegrator.getTimingSource()
		);
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
		LazyQueryState searcher = buildSearcher();
		if ( searcher == null ) {
			return Collections.emptyList();
		}
		try {
			QueryHits queryHits = getQueryHits( searcher, calculateTopDocsRetrievalSize() );
			final int first = firstResult;
			final int max = max( first, queryHits.getTotalHits() );
			final int size = max - first + 1;
			if ( size <= 0 ) {
				return Collections.emptyList();
			}
			List<EntityInfo> infos = new ArrayList<EntityInfo>( size );
			DocumentExtractor extractor = buildDocumentExtractor( searcher, queryHits, first, max );
			for ( int index = first; index <= max; index++ ) {
				infos.add( extractor.extract( index ) );
				//Check for timeout each 16 elements:
				if ( (index & 0x000F) == 0 ) {
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

	private DocumentExtractor buildDocumentExtractor(LazyQueryState searcher, QueryHits queryHits, int first, int max) {
		return new DocumentExtractorImpl(
				queryHits,
				extendedIntegrator,
				projectedFields,
				idFieldNames,
				allowFieldSelectionInProjection,
				searcher,
				first,
				max,
				targetedEntityBindingsByName
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
		LazyQueryState openSearcher = buildSearcher();
		//FIXME: handle null searcher
		try {
			QueryHits queryHits = getQueryHits( openSearcher, calculateTopDocsRetrievalSize() );
			int max = max( firstResult, queryHits.getTotalHits() );
			return buildDocumentExtractor( openSearcher, queryHits, firstResult, max );
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
			LazyQueryState searcher = buildSearcher( extendedIntegrator, false );
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
		LazyQueryState searcher = buildSearcher( extendedIntegrator, true );
		if ( searcher == null ) {
			throw new SearchException(
					"Unable to build explanation for document id:"
							+ documentId + ". no index found"
			);
		}
		try {
			QueryFilters filters = createFilters();
			explanation = searcher.explain( filters, documentId );
		}
		catch (IOException e) {
			throw new SearchException( "Unable to query Lucene index and build explanation", e );
		}
		finally {
			closeSearcher( searcher );
		}
		return explanation;
	}

	private void closeSearcher(LazyQueryState searcherWithPayload) {
		if ( searcherWithPayload == null ) {
			return;
		}
		searcherWithPayload.close();
	}

	/**
	 * This class caches some of the query results and we need to reset the state in case something in the query
	 * changes (eg a new filter is set).
	 */
	@Override
	protected void clearCachedResults() {
		resultSize = null;
	}

	@Override
	protected void extractFacetResults() {
		DocumentExtractor queryDocumentExtractor = queryDocumentExtractor();
		queryDocumentExtractor.close();
	}

	@Override
	protected Set<String> getSupportedProjectionConstants() {
		return SUPPORTED_PROJECTION_CONSTANTS;
	}

	@Override
	protected Set<IndexManager> getIndexManagers(EntityIndexBinding binding) {
		Set<IndexManager> indexManagers = super.getIndexManagers( binding );

		for ( IndexManager indexManager : indexManagers ) {
			if ( !( indexManager instanceof DirectoryBasedIndexManager ) ) {
				throw log.cannotRunLuceneQueryTargetingEntityIndexedWithNonLuceneIndexManager(
						binding.getDocumentBuilder().getTypeIdentifier(),
						luceneQuery.toString()
				);
			}
		}

		return indexManagers;
	}

	/**
	 * Execute the lucene search and return the matching hits.
	 *
	 * @param searcher The index searcher.
	 * @param n Number of documents to retrieve
	 *
	 * @return An instance of {@code QueryHits} wrapping the Lucene query and the matching documents.
	 *
	 * @throws IOException in case there is an error executing the lucene search.
	 */
	private QueryHits getQueryHits(LazyQueryState searcher, Integer n) throws IOException {
		QueryFilters filters = createFilters();
		QueryHits queryHits;

		boolean stats = extendedIntegrator.getStatistics().isStatisticsEnabled();
		long startTime = 0;
		if ( stats ) {
			startTime = System.nanoTime();
		}

		Collection<FacetingRequest> facetingRequests = getFacetManager().getFacetRequests().values();
		Map<FacetingRequest, FacetMetadata> facetingRequestsAndMetadata =
				buildFacetingRequestsAndMetadata( facetingRequests, targetedEntityBindingsByName.values() );

		if ( n == null ) { // try to make sure that we get the right amount of top docs
			queryHits = new QueryHits(
					searcher,
					filters,
					sort,
					getTimeoutManager(),
					facetingRequestsAndMetadata,
					spatialSearchCenter,
					spatialFieldName
			);
		}
		else if ( 0 == n ) {
			queryHits = new QueryHits(
					searcher,
					filters,
					null,
					0,
					getTimeoutManager(),
					null,
					spatialSearchCenter,
					spatialFieldName
			);
		}
		else {
			queryHits = new QueryHits(
					searcher,
					filters,
					sort,
					n,
					getTimeoutManager(),
					facetingRequestsAndMetadata,
					spatialSearchCenter,
					spatialFieldName
			);
		}
		resultSize = queryHits.getTotalHits();

		if ( stats ) {
			extendedIntegrator.getStatisticsImplementor()
					.searchExecuted( searcher.describeQuery(), System.nanoTime() - startTime );
		}
		getFacetManager().setFacetResults( queryHits.getFacets() );
		return queryHits;
	}

	/**
	 * @return Calculates the number of <code>TopDocs</code> which should be retrieved as part of the query. If Hibernate's
	 *         pagination parameters are set returned value is <code>first + maxResults</code>. Otherwise <code>null</code> is
	 *         returned.
	 */
	private Integer calculateTopDocsRetrievalSize() {
		if ( maxResults == null ) {
			return null;
		}
		else {
			long tmpMaxResult = (long) firstResult + maxResults;
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

	private LazyQueryState buildSearcher() {
		return buildSearcher( extendedIntegrator, null );
	}

	/**
	 * Build the index searcher for this fulltext query.
	 *
	 * @param extendedIntegrator the search factory.
	 * @param forceScoring if true, force SCORE computation, if false, force not to compute score, if null used best choice
	 *
	 * @return the <code>IndexSearcher</code> for this query (can be <code>null</code>.
	 *         TODO change classesAndSubclasses by side effect, which is a mismatch with the Searcher return, fix that.
	 */
	private LazyQueryState buildSearcher(ExtendedSearchIntegrator extendedIntegrator, Boolean forceScoring) {
		Set<IndexManager> targetedIndexes = new HashSet<>();
		Set<String> idFieldNames = new HashSet<String>();
		Similarity searcherSimilarity = null;

		SortConfigurations.Builder sortConfigurations = new SortConfigurations.Builder();

		this.targetedEntityBindingsByName = buildTargetedEntityIndexBindingsByName();

		//TODO check if caching this work for the last n list of indexedTargetedEntities makes a perf boost
		for ( EntityIndexBinding entityIndexBinding : targetedEntityBindingsByName.values() ) {
			DocumentBuilderIndexedEntity builder = entityIndexBinding.getDocumentBuilder();
			IndexedTypeIdentifier typeIdentifier = builder.getTypeIdentifier();
			searcherSimilarity = checkSimilarity( searcherSimilarity, entityIndexBinding.getSimilarity() );
			if ( builder.getIdFieldName() != null ) {
				idFieldNames.add( builder.getIdFieldName() );
				allowFieldSelectionInProjection = allowFieldSelectionInProjection && builder.allowFieldSelectionInProjection();
			}

			Set<IndexManager> indexManagers = getIndexManagers( entityIndexBinding );
			targetedIndexes.addAll( indexManagers );
			Optional<CustomTypeMetadata> customTypeMetadata = getCustomTypeMetadata( typeIdentifier );
			collectSortableFields( sortConfigurations, indexManagers, builder.getTypeMetadata(), customTypeMetadata );
		}

		this.idFieldNames = idFieldNames;

		if ( targetedIndexes.isEmpty() ) {
			/*
			 * May happen when searching on indexes with dynamic sharding that haven't any shard yet.
			 */
			return null;
		}

		//compute optimization needClassFilterClause
		//if at least one DP contains one class that is not part of the targeted classesAndSubclasses we can't optimize
		if ( indexedTargetedEntities.size() > 0 ) {
			for ( IndexManager indexManager : targetedIndexes ) {
				final IndexedTypeSet classesInIndexManager = indexManager.getContainedTypes();
				// if an IndexManager contains only one class, we know for sure it's part of classesAndSubclasses
				if ( classesInIndexManager.size() > 1 ) {
					//risk of needClassFilterClause
					for ( IndexedTypeIdentifier clazz : classesInIndexManager ) {
						if ( !targetedEntityBindingsByName.containsKey( clazz.getName() ) ) {
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

		if ( this.sort != null ) {
			validateSortFields( targetedEntityBindingsByName.values() );
		}

		//set up the searcher
		final IndexManager[] indexManagers = targetedIndexes.toArray(
				new IndexManager[targetedIndexes.size()]
		);

		final IndexReader compoundReader = MultiReaderFactory.openReader(
				sortConfigurations.build(),
				sort,
				indexManagers,
				extendedIntegrator.isIndexUninvertingAllowed()
		);

		final Query filteredQuery = filterQueryByTenantId( filterQueryByClasses( luceneQuery ) );

		final QueryFilters facetingFilters = getFacetManager().getFacetFilters();

		//handle the sort and projection
		final String[] projection = this.projectedFields;
		Collection<EntityIndexBinding> targetedEntityBindings = targetedEntityBindingsByName.values();
		if ( Boolean.TRUE.equals( forceScoring ) ) {
			return new LazyQueryState(
					filteredQuery,
					facetingFilters,
					compoundReader,
					searcherSimilarity,
					extendedIntegrator,
					targetedEntityBindings,
					true,
					true
			);
		}
		else if ( Boolean.FALSE.equals( forceScoring ) ) {
			return new LazyQueryState(
					filteredQuery,
					facetingFilters,
					compoundReader,
					searcherSimilarity,
					extendedIntegrator,
					targetedEntityBindings,
					false, false
			);
		}
		else if ( this.sort != null ) {
			if ( projection != null ) {
				boolean activate = false;
				for ( String field : projection ) {
					if ( SCORE.equals( field ) ) {
						activate = true;
						break;
					}
				}
				if ( activate ) {
					return new LazyQueryState(
							filteredQuery,
							facetingFilters,
							compoundReader,
							searcherSimilarity,
							extendedIntegrator,
							targetedEntityBindings,
							true,
							false
					);
				}
			}
		}
		//default
		return new LazyQueryState(
				filteredQuery,
				facetingFilters,
				compoundReader,
				searcherSimilarity,
				extendedIntegrator,
				targetedEntityBindings,
				false,
				false
		);
	}

	/**
	 * Collects all sort fields declared on the properties of the given type or the properties of all the types it
	 * embeds into the given list.
	 */
	private void collectSortableFields(SortConfigurations.Builder sortConfigurations, Iterable<IndexManager> indexManagers, TypeMetadata typeMetadata,
			Optional<CustomTypeMetadata> customTypeMetadataOptional) {
		for ( IndexManager indexManager : indexManagers ) {
			sortConfigurations.setIndex( indexManager.getIndexName() );
			sortConfigurations.setEntityType( typeMetadata.getType() );

			sortConfigurations.addSortableFields( typeMetadata.getClassBridgeSortableFieldMetadata() );
			sortConfigurations.addSortableFields( typeMetadata.getIdPropertyMetadata().getSortableFieldMetadata() );

			for ( PropertyMetadata property : typeMetadata.getAllPropertyMetadata() ) {
				sortConfigurations.addSortableFields( property.getSortableFieldMetadata() );
			}

			for ( EmbeddedTypeMetadata embeddedType : typeMetadata.getEmbeddedTypeMetadata() ) {
				collectSortableFields( sortConfigurations, embeddedType );
			}

			if ( customTypeMetadataOptional.isPresent() ) {
				CustomTypeMetadata customTypeMetadata = customTypeMetadataOptional.get();
				for ( String fieldName : customTypeMetadata.getSortableFields() ) {
					sortConfigurations.addSortableField( new SortableFieldMetadata.Builder( fieldName ).build() );
				}
			}
		}
	}

	private void collectSortableFields(SortConfigurations.Builder sortConfigurations, EmbeddedTypeMetadata embeddedTypeMetadata) {
		sortConfigurations.addSortableFields( embeddedTypeMetadata.getClassBridgeSortableFieldMetadata() );

		for ( PropertyMetadata property : embeddedTypeMetadata.getAllPropertyMetadata() ) {
			sortConfigurations.addSortableFields( property.getSortableFieldMetadata() );
		}

		for ( EmbeddedTypeMetadata embeddedType : embeddedTypeMetadata.getEmbeddedTypeMetadata() ) {
			collectSortableFields( sortConfigurations, embeddedType );
		}
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

	private QueryFilters createFilters() {
		List<Query> filterQueries = new ArrayList<>();
		if ( !filterDefinitions.isEmpty() ) {
			for ( FullTextFilterImpl fullTextFilter : filterDefinitions.values() ) {
				Query filter = buildLuceneFilter( fullTextFilter );
				if ( filter != null ) {
					filterQueries.add( filter );
				}
			}
		}

		if ( userFilter != null ) {
			filterQueries.add( userFilter );
		}

		if ( filterQueries.isEmpty() ) {
			return QueryFilters.EMPTY_FILTERSET;
		}
		else {
			return new QueryFilters( filterQueries );
		}
	}

	/**
	 * Builds a Lucene filter using the given <code>FullTextFilter</code>.
	 *
	 * @param fullTextFilter the Hibernate Search specific <code>FullTextFilter</code>,
	 * referencing the filter to use and providing parameters
	 * @return the filter, as a Lucene <code>Query</code>.
	 */
	private Query buildLuceneFilter(FullTextFilterImpl fullTextFilter) {

		/*
		 * FilterKey implementations and Filter(Factory) do not have to be threadsafe wrt their parameter injection
		 * as FilterCachingStrategy ensure a memory barrier between concurrent thread calls
		 */
		FilterDef def = extendedIntegrator.getFilterDefinition( fullTextFilter.getName() );
		//def can never be null, it's guarded by enableFullTextFilter(String)

		if ( isPreQueryFilterOnly( def ) ) {
			return null;
		}

		if ( !cacheInstance( def.getCacheMode() ) ) {
			Object filterOrFactory = createFilterInstance( fullTextFilter, def );
			return createFilterQuery( def, filterOrFactory );
		}
		else {
			return createOrGetLuceneFilterFromCache( fullTextFilter, def );
		}
	}

	@SuppressWarnings("deprecation")
	private Query createOrGetLuceneFilterFromCache(FullTextFilterImpl fullTextFilter, FilterDef def) {
		// Avoiding the filter/factory instantiation, unless needed for key determination or actual filter creation
		boolean hasCustomKey = def.getKeyMethod() != null;
		Object filterOrFactory = hasCustomKey ? createFilterInstance( fullTextFilter, def ) : null;

		FilterKey key = createFilterKey( def, filterOrFactory, fullTextFilter );

		// try to get the filter out of the cache
		Query filterQuery = extendedIntegrator.getFilterCachingStrategy().getCachedFilter( key );

		if ( filterQuery == null ) {
			filterQuery = createFilterQuery( def, hasCustomKey ? filterOrFactory : createFilterInstance( fullTextFilter, def ) );
			extendedIntegrator.getFilterCachingStrategy().addCachedFilter( key, filterQuery );
		}

		return filterQuery;
	}

	private Query createFilterQuery(FilterDef def, Object filterOrFactory) {
		Query filterQuery;
		if ( def.getFactoryMethod() != null ) {
			try {
				filterQuery = (Query) def.getFactoryMethod().invoke( filterOrFactory );
			}
			catch (IllegalAccessException | InvocationTargetException e) {
				throw new SearchException(
						"Unable to access @Factory method: "
								+ def.getImpl().getName() + "." + def.getFactoryMethod().getName(), e
				);
			}
			catch (ClassCastException e) {
				throw new SearchException(
						"Factory method does not return a org.apache.lucene.search.Query class: "
								+ def.getImpl().getName() + "." + def.getFactoryMethod().getName(), e
				);
			}
		}
		else {
			try {
				filterQuery = (Query) filterOrFactory;
			}
			catch (ClassCastException e) {
				throw new SearchException(
						"Filter implementation does not extend the Query class: "
								+ def.getImpl().getName() + ". "
								+ ( def.getFactoryMethod() != null ? def.getFactoryMethod().getName() : "" ), e
				);
			}
		}

		return addCachingWrapper( filterQuery, def );
	}

	/**
	 * Decides whether to wrap the given filter around a <code>CachingWrapperQuery<code>.
	 *
	 * @param filterQuery the filter query which maybe gets wrapped.
	 * @param def The filter definition used to decide whether wrapping should occur or not.
	 *
	 * @return The original filter query or wrapped filter query depending on the information extracted from
	 *         <code>def</code>.
	 */
	private Query addCachingWrapper(Query filterQuery, FilterDef def) {
		if ( cacheResults( def.getCacheMode() ) ) {
			int cacheSize = extendedIntegrator.getFilterCacheBitResultsSize();
			filterQuery = new CachingWrapperQuery( filterQuery, cacheSize );
		}

		return filterQuery;
	}

	private FilterKey createFilterKey(FilterDef def, Object filterOrFactory, FullTextFilterImpl fullTextFilter) {
		FilterKey key = null;

		if ( def.getKeyMethod() == null ) {
			key = new DefaultFilterKey( def.getName(), fullTextFilter.getParameters() );
		}
		else {
			try {
				key = (FilterKey) def.getKeyMethod().invoke( filterOrFactory );
			}
			catch (IllegalAccessException | InvocationTargetException e) {
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

			key.setImpl( def.getImpl() );

			//Make sure Filters are isolated by filter def name
			StandardFilterKey wrapperKey = new StandardFilterKey();
			wrapperKey.addParameter( def.getName() );
			wrapperKey.addParameter( key );

			key = wrapperKey;
		}

		return key;
	}

	private org.apache.lucene.search.Query filterQueryByClasses(org.apache.lucene.search.Query luceneQuery) {
		if ( !needClassFilterClause ) {
			return luceneQuery;
		}
		else {
			//A query filter is more practical than a manual class filtering post query (esp on scrollable resultsets)
			//it also probably minimises the memory footprint
			BooleanQuery.Builder classFilterBuilder = new BooleanQuery.Builder();
			for ( String typeName : targetedEntityBindingsByName.keySet() ) {
				Term t = new Term( ProjectionConstants.OBJECT_CLASS, typeName );
				TermQuery termQuery = new TermQuery( t );
				classFilterBuilder.add( termQuery, BooleanClause.Occur.SHOULD );
			}
			BooleanQuery classFilter = classFilterBuilder.build();
			BooleanQuery.Builder combinedQueryBuilder = new BooleanQuery.Builder();
			combinedQueryBuilder.add( luceneQuery, BooleanClause.Occur.MUST );
			combinedQueryBuilder.add( classFilter, BooleanClause.Occur.FILTER );
			return combinedQueryBuilder.build();
		}
	}

	private org.apache.lucene.search.Query filterQueryByTenantId(org.apache.lucene.search.Query luceneQuery) {
		if ( tenantId == null ) {
			return luceneQuery;
		}
		else {
			TermQuery tenantIdFilter = new TermQuery( new Term( DocumentBuilderIndexedEntity.TENANT_ID_FIELDNAME, tenantId ) );
			// annihilate the scoring impact of TENANT_ID
			tenantIdFilter.setBoost( 0 );

			BooleanQuery.Builder combinedQueryBuilder = new BooleanQuery.Builder();
			combinedQueryBuilder.add( luceneQuery, BooleanClause.Occur.MUST );
			combinedQueryBuilder.add( tenantIdFilter, BooleanClause.Occur.FILTER );
			return combinedQueryBuilder.build();
		}
	}

	private int max(int first, int totalHits) {
		if ( maxResults == null ) {
			return totalHits - 1;
		}
		else {
			return maxResults + first < totalHits ?
					first + maxResults - 1 :
					totalHits - 1;
		}
	}

	@Override
	public String getQueryString() {
		return String.valueOf( luceneQuery );
	}

}
