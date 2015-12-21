/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.impl;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.similarities.Similarity;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.engine.impl.FilterDef;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;
import org.hibernate.search.engine.metadata.impl.PropertyMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.filter.FilterKey;
import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.filter.ShardSensitiveOnlyFilter;
import org.hibernate.search.filter.StandardFilterKey;
import org.hibernate.search.filter.impl.CachingWrapperFilter;
import org.hibernate.search.filter.impl.ChainedFilter;
import org.hibernate.search.filter.impl.DefaultFilterKey;
import org.hibernate.search.filter.impl.FullTextFilterImpl;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.metadata.FieldDescriptor;
import org.hibernate.search.metadata.FieldSettingsDescriptor.Type;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.query.engine.spi.DocumentExtractor;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.reader.impl.MultiReaderFactory;
import org.hibernate.search.spatial.DistanceSortField;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import static org.hibernate.search.util.impl.FilterCacheModeTypeHelper.cacheInstance;
import static org.hibernate.search.util.impl.FilterCacheModeTypeHelper.cacheResults;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class LuceneHSQuery extends AbstractHSQuery implements HSQuery {

	static final Log log = LoggerFactory.make();
	private static final FullTextFilterImplementor[] EMPTY_FULL_TEXT_FILTER_IMPLEMENTOR = new FullTextFilterImplementor[0];

	private Query luceneQuery;

	private boolean allowFieldSelectionInProjection = true;

	/**
	 * Combined chained filter to be applied to the query.
	 */
	private Filter filter;

	private transient Set<Class<?>> classesAndSubclasses;
	//optimization: if we can avoid the filter clause (we can most of the time) do it as it has a significant perf impact
	private boolean needClassFilterClause;
	private Set<String> idFieldNames;
	private transient FacetManagerImpl facetManager;

	/**
	 * The number of results for this query. This field gets populated once {@link #queryResultSize}, {@link #queryEntityInfos}
	 * or {@link #queryDocumentExtractor} is called.
	 */
	private Integer resultSize;

	public LuceneHSQuery(ExtendedSearchIntegrator extendedIntegrator) {
		super( extendedIntegrator );
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
			int first = firstResult;
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
			org.apache.lucene.search.Query filteredQuery = filterQueryByTenantId( filterQueryByClasses( luceneQuery ) );
			buildFilters();
			explanation = searcher.explain( filteredQuery, documentId );
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
		buildFilters();
		QueryHits queryHits;

		boolean stats = extendedIntegrator.getStatistics().isStatisticsEnabled();
		long startTime = 0;
		if ( stats ) {
			startTime = System.nanoTime();
		}

		if ( n == null ) { // try to make sure that we get the right amount of top docs
			queryHits = new QueryHits(
					searcher,
					filter,
					sort,
					getTimeoutManager(),
					facetManager.getFacetRequests(),
					this.timeoutExceptionFactory,
					spatialSearchCenter,
					spatialFieldName
			);
		}
		else if ( 0 == n) {
			queryHits = new QueryHits(
					searcher,
					filter,
					null,
					0,
					getTimeoutManager(),
					null,
					this.timeoutExceptionFactory,
					spatialSearchCenter,
					spatialFieldName
			);
		}
		else {
			queryHits = new QueryHits(
					searcher,
					filter,
					sort,
					n,
					getTimeoutManager(),
					facetManager.getFacetRequests(),
					this.timeoutExceptionFactory,
					spatialSearchCenter,
					spatialFieldName
			);
		}
		resultSize = queryHits.getTotalHits();

		if ( stats ) {
			extendedIntegrator.getStatisticsImplementor()
					.searchExecuted( searcher.describeQuery(), System.nanoTime() - startTime );
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
		Map<Class<?>, EntityIndexBinding> indexBindings = extendedIntegrator.getIndexBindings();
		Set<IndexManager> targetedIndexes = new HashSet<>();
		Set<String> idFieldNames = new HashSet<String>();
		Similarity searcherSimilarity = null;

		SortConfigurations.Builder sortConfigurations = new SortConfigurations.Builder();

		//TODO check if caching this work for the last n list of indexedTargetedEntities makes a perf boost
		if ( indexedTargetedEntities.size() == 0 ) {
			// empty indexedTargetedEntities array means search over all indexed entities,
			// but we have to make sure there is at least one
			if ( indexBindings.isEmpty() ) {
				throw new SearchException(
						"There are no mapped entities. Don't forget to add @Indexed to at least one class."
				);
			}

			for ( EntityIndexBinding entityIndexBinding : indexBindings.values() ) {
				DocumentBuilderIndexedEntity builder = entityIndexBinding.getDocumentBuilder();
				searcherSimilarity = checkSimilarity( searcherSimilarity, entityIndexBinding.getSimilarity() );
				if ( builder.getIdKeywordName() != null ) {
					idFieldNames.add( builder.getIdKeywordName() );
					allowFieldSelectionInProjection = allowFieldSelectionInProjection && builder.allowFieldSelectionInProjection();
				}

				List<IndexManager> indexManagers = getIndexManagers( entityIndexBinding );
				targetedIndexes.addAll( indexManagers );
				collectSortableFields( sortConfigurations, indexManagers, entityIndexBinding.getDocumentBuilder().getTypeMetadata() );
			}
			classesAndSubclasses = null;
		}
		else {
			Set<Class<?>> involvedClasses = new HashSet<Class<?>>( indexedTargetedEntities.size() );
			involvedClasses.addAll( indexedTargetedEntities );
			for ( Class<?> clazz : indexedTargetedEntities ) {
				EntityIndexBinding indexBinder = indexBindings.get( clazz );
				if ( indexBinder != null ) {
					DocumentBuilderIndexedEntity builder = indexBinder.getDocumentBuilder();
					involvedClasses.addAll( builder.getMappedSubclasses() );
				}
			}

			for ( Class clazz : involvedClasses ) {
				EntityIndexBinding entityIndexBinding = indexBindings.get( clazz );
				//TODO should we rather choose a polymorphic path and allow non mapped entities
				if ( entityIndexBinding == null ) {
					throw new SearchException( "Not a mapped entity (don't forget to add @Indexed): " + clazz );
				}
				DocumentBuilderIndexedEntity builder = entityIndexBinding.getDocumentBuilder();
				if ( builder.getIdKeywordName() != null ) {
					idFieldNames.add( builder.getIdKeywordName() );
					allowFieldSelectionInProjection = allowFieldSelectionInProjection && builder.allowFieldSelectionInProjection();
				}

				List<IndexManager> indexManagers = getIndexManagers( entityIndexBinding );
				targetedIndexes.addAll( indexManagers );
				collectSortableFields( sortConfigurations, indexManagers, builder.getTypeMetadata() );
				searcherSimilarity = checkSimilarity( searcherSimilarity, entityIndexBinding.getSimilarity() );
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
			this.classesAndSubclasses = extendedIntegrator.getIndexedTypes();
		}

		if ( this.sort != null ) {
			validateSortFields( extendedIntegrator );
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

		//handle the sort and projection
		final String[] projection = this.projectedFields;
		if ( Boolean.TRUE.equals( forceScoring ) ) {
			return new LazyQueryState(
					filteredQuery,
					compoundReader,
					searcherSimilarity,
					extendedIntegrator,
					classesAndSubclasses,
					true,
					true
			);
		}
		else if ( Boolean.FALSE.equals( forceScoring ) ) {
			return new LazyQueryState(
					filteredQuery,
					compoundReader,
					searcherSimilarity,
					extendedIntegrator,
					classesAndSubclasses,
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
							compoundReader,
							searcherSimilarity,
							extendedIntegrator,
							classesAndSubclasses,
							true,
							false
					);
				}
			}
		}
		//default
		return new LazyQueryState(
				filteredQuery,
				compoundReader,
				searcherSimilarity,
				extendedIntegrator,
				classesAndSubclasses,
				false,
				false
		);
	}

	/**
	 * Collects all sort fields declared on the properties of the given type or the properties of all the types it
	 * embeds into the given list.
	 */
	private void collectSortableFields(SortConfigurations.Builder sortConfigurations, Iterable<IndexManager> indexManagers, TypeMetadata typeMetadata) {
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

	private void validateSortFields(ExtendedSearchIntegrator extendedIntegrator) {
		SortField[] sortFields = sort.getSort();
		for ( SortField sortField : sortFields ) {
			if ( sortField instanceof DistanceSortField ) {
				validateDistanceSortField( extendedIntegrator, sortField );
			}
		}
	}

	private void validateDistanceSortField(ExtendedSearchIntegrator extendedIntegrator, SortField sortField) {
		boolean indexedField = false;
		String field = sortField.getField();
		if ( field != null ) {
			for ( Class<?> clazz : classesAndSubclasses ) {
				IndexedTypeDescriptor indexedTypeDescriptor = extendedIntegrator.getIndexedTypeDescriptor( clazz );
				FieldDescriptor fieldDescriptor = indexedTypeDescriptor.getIndexedField( field );
				if ( fieldDescriptor != null ) {
					indexedField = true;
					if ( fieldDescriptor.getType() != Type.SPATIAL ) {
						throw log.distanceSortRequiresSpatialField( field );
					}
					break;
				}
			}
		}
		if ( !indexedField ) {
			throw log.sortRequiresIndexedField( sortField.getClass(), field );
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

	private List<IndexManager> getIndexManagers(EntityIndexBinding binding) {
		FullTextFilterImplementor[] fullTextFilters = getFullTextFilters();
		List<IndexManager> indexManagers = Arrays.asList( binding.getSelectionStrategy().getIndexManagersForQuery( fullTextFilters ) );

		for ( IndexManager indexManager : indexManagers ) {
			if ( !( indexManager instanceof DirectoryBasedIndexManager ) ) {
				//TODO clarify intent:
				// A) improve the error message so that people understand what they should do
				// B) Is the point really to not support "directory-based" or rather non-Lucene native based ones?
				throw log.cannotRunLuceneQueryTargetingEntityIndexedWithNonDirectoryBasedIndexManager(
						binding.getDocumentBuilder().getBeanClass(),
						luceneQuery.toString()
				);
			}
		}

		return indexManagers;
	}

	private FullTextFilterImplementor[] getFullTextFilters() {
		FullTextFilterImplementor[] fullTextFilters;

		if ( filterDefinitions != null && !filterDefinitions.isEmpty() ) {
			fullTextFilters = filterDefinitions.values().toArray( new FullTextFilterImplementor[filterDefinitions.size()] );
		}
		else {
			// no filter get all shards
			fullTextFilters = EMPTY_FULL_TEXT_FILTER_IMPLEMENTOR;
		}
		return fullTextFilters;
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
		FilterDef def = extendedIntegrator.getFilterDefinition( fullTextFilter.getName() );
		//def can never be null, it's guarded by enableFullTextFilter(String)

		if ( isPreQueryFilterOnly( def ) ) {
			return null;
		}

		if ( !cacheInstance( def.getCacheMode() ) ) {
			Object filterOrFactory = createFilterInstance( fullTextFilter, def );
			return createFilter( def, filterOrFactory );
		}
		else {
			return createOrGetLuceneFilterFromCache( fullTextFilter, def );
		}
	}

	private Filter createOrGetLuceneFilterFromCache(FullTextFilterImpl fullTextFilter, FilterDef def) {
		// Avoiding the filter/factory instantiation, unless needed for key determination or actual filter creation
		boolean hasCustomKey = def.getKeyMethod() != null;
		Object filterOrFactory = hasCustomKey ? createFilterInstance( fullTextFilter, def ) : null;

		FilterKey key = createFilterKey( def, filterOrFactory, fullTextFilter );

		// try to get the filter out of the cache
		Filter filter = extendedIntegrator.getFilterCachingStrategy().getCachedFilter( key );

		if ( filter == null ) {
			filter = createFilter( def, hasCustomKey ? filterOrFactory : createFilterInstance( fullTextFilter, def ) );
			extendedIntegrator.getFilterCachingStrategy().addCachedFilter( key, filter );
		}

		return filter;
	}

	private boolean isPreQueryFilterOnly(FilterDef def) {
		return def.getImpl().equals( ShardSensitiveOnlyFilter.class );
	}

	private Filter createFilter(FilterDef def, Object filterOrFactory) {
		Filter filter;
		if ( def.getFactoryMethod() != null ) {
			try {
				filter = (Filter) def.getFactoryMethod().invoke( filterOrFactory );
			}
			catch (IllegalAccessException | InvocationTargetException e) {
				throw new SearchException(
						"Unable to access @Factory method: "
								+ def.getImpl().getName() + "." + def.getFactoryMethod().getName(), e
				);
			}
			catch (ClassCastException e) {
				throw new SearchException(
						"Factory method does not return a org.apache.lucene.search.Filter class: "
								+ def.getImpl().getName() + "." + def.getFactoryMethod().getName(), e
				);
			}
		}
		else {
			try {
				filter = (Filter) filterOrFactory;
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
			int cachingWrapperFilterSize = extendedIntegrator.getFilterCacheBitResultsSize();
			filter = new CachingWrapperFilter( filter, cachingWrapperFilterSize );
		}

		return filter;
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

	private Object createFilterInstance(FullTextFilterImpl fullTextFilter, FilterDef def) {
		final Object instance = ClassLoaderHelper.instanceFromClass( Object.class, def.getImpl(), "@FullTextFilterDef" );
		for ( Map.Entry<String, Object> entry : fullTextFilter.getParameters().entrySet() ) {
			def.invoke( entry.getKey(), instance, entry.getValue() );
		}
		return instance;
	}

	private org.apache.lucene.search.Query filterQueryByClasses(org.apache.lucene.search.Query luceneQuery) {
		if ( !needClassFilterClause ) {
			return luceneQuery;
		}
		else {
			//A query filter is more practical than a manual class filtering post query (esp on scrollable resultsets)
			//it also probably minimises the memory footprint
			BooleanQuery.Builder classFilterBuilder = new BooleanQuery.Builder();
			for ( Class clazz : classesAndSubclasses ) {
				Term t = new Term( ProjectionConstants.OBJECT_CLASS, clazz.getName() );
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
}
