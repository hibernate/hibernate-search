/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.query;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Query;
import org.hibernate.QueryTimeoutException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.query.ParameterMetadata;
import org.hibernate.impl.AbstractQueryImpl;
import org.hibernate.search.FullTextFilter;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.SearchException;
import org.hibernate.search.engine.DocumentBuilder;
import org.hibernate.search.engine.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.DocumentExtractor;
import org.hibernate.search.engine.EntityInfo;
import org.hibernate.search.engine.FilterDef;
import org.hibernate.search.engine.Loader;
import org.hibernate.search.engine.ProjectionLoader;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.filter.ChainedFilter;
import org.hibernate.search.filter.FilterKey;
import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.filter.ShardSensitiveOnlyFilter;
import org.hibernate.search.filter.StandardFilterKey;
import org.hibernate.search.reader.ReaderProvider;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.util.ContextHelper;
import org.hibernate.search.util.LoggerFactory;
import org.hibernate.transform.ResultTransformer;

import static org.hibernate.search.reader.ReaderProviderHelper.getIndexReaders;
import static org.hibernate.search.util.FilterCacheModeTypeHelper.cacheInstance;
import static org.hibernate.search.util.FilterCacheModeTypeHelper.cacheResults;

/**
 * Implementation of {@link org.hibernate.search.FullTextQuery}.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @todo Implements setParameter()
 */
public class FullTextQueryImpl extends AbstractQueryImpl implements FullTextQuery {
	private static final Logger log = LoggerFactory.make();
	private final org.apache.lucene.search.Query luceneQuery;
	private Set<Class<?>> indexedTargetedEntities;
	private List<Class<?>> targetedEntities;
	private Set<Class<?>> classesAndSubclasses;
	//optimization: if we can avoid the filter clause (we can most of the time) do it as it has a significant perf impact
	private boolean needClassFilterClause;
	private Integer firstResult;
	private Integer maxResults;
	private Integer resultSize;
	private Sort sort;
	private Filter filter;
	private Filter userFilter;
	private Criteria criteria;
	private String[] indexProjection;
	private Set<String> idFieldNames;
	private boolean allowFieldSelectionInProjection = true;
	private ResultTransformer resultTransformer;
	private SearchFactoryImplementor searchFactoryImplementor;
	private final Map<String, FullTextFilterImpl> filterDefinitions = new HashMap<String, FullTextFilterImpl>();
	private int fetchSize = 1;
	private static final FullTextFilterImplementor[] EMPTY_FULL_TEXT_FILTER_IMPLEMENTOR = new FullTextFilterImplementor[0];
	private final TimeoutManager timeoutManager;


	/**
	 * Constructs a  <code>FullTextQueryImpl</code> instance.
	 *
	 * @param query The Lucene query.
	 * @param classes Array of classes (must be immutable) used to filter the results to the given class types.
	 * @param session Access to the Hibernate session.
	 * @param parameterMetadata Additional query metadata.
	 */
	public FullTextQueryImpl(org.apache.lucene.search.Query query, Class<?>[] classes, SessionImplementor session,
							 ParameterMetadata parameterMetadata) {
		//TODO handle flushMode
		super( query.toString(), null, session, parameterMetadata );
		this.luceneQuery = query;
		this.targetedEntities = Arrays.asList( classes );
		searchFactoryImplementor = getSearchFactoryImplementor();
		this.indexedTargetedEntities = searchFactoryImplementor.getIndexedTypesPolymorphic( classes );
		if ( classes != null && classes.length > 0 && indexedTargetedEntities.size() == 0 ) {
			String msg = "None of the specified entity types or any of their subclasses are indexed.";
			throw new IllegalArgumentException( msg );
		}
		timeoutManager = new TimeoutManager( );
	}

	/**
	 * {@inheritDoc}
	 */
	public FullTextQuery setSort(Sort sort) {
		this.sort = sort;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public FullTextQuery setFilter(Filter filter) {
		this.userFilter = filter;
		return this;
	}

	/**
	 * Return an iterator on the results.
	 * Retrieve the object one by one (initialize it during the next() operation)
	 */
	public Iterator iterate() throws HibernateException {
		//implement an iterator which keep the id/class for each hit and get the object on demand
		//cause I can't keep the searcher and hence the hit opened. I don't have any hook to know when the
		//user stops using it
		//scrollable is better in this area

		timeoutManager.start( luceneQuery );
		//find the directories
		IndexSearcherWithPayload searcher = buildSearcher( searchFactoryImplementor );
		if ( searcher == null ) {
			return new IteratorImpl( Collections.EMPTY_LIST, noLoader );
		}
		try {
			QueryHits queryHits = getQueryHits( searcher, calculateTopDocsRetrievalSize()  );
			int first = first();
			int max = max( first, queryHits.totalHits );

			int size = max - first + 1 < 0 ? 0 : max - first + 1;
			List<EntityInfo> infos = new ArrayList<EntityInfo>( size );
			DocumentExtractor extractor = new DocumentExtractor(
					queryHits, searchFactoryImplementor, indexProjection, idFieldNames, allowFieldSelectionInProjection
			);
			try {
				for ( int index = first; index <= max; index++ ) {
					infos.add( extractor.extract( index ) );
					//TODO should we measure on each extractor?
					if ( index % 10 == 0 ) timeoutManager.isTimedOut();
				}
			}
			catch ( QueryTimeoutException e ) {
				reactOnQueryTimeoutExceptionWhileExtracting( e );
			}
			Loader loader = getLoader();
			//stop timeout manager, the iterator pace is in the user's hands
			timeoutManager.stop();
			return new IteratorImpl( infos, loader );
		}
		catch ( IOException e ) {
			throw new HibernateException( "Unable to query Lucene index", e );
		}
		finally {
			try {
				closeSearcher( searcher.getSearcher(), searchFactoryImplementor.getReaderProvider() );
			}
			catch ( SearchException e ) {
				log.warn( "Unable to properly close searcher during lucene query: " + getQueryString(), e );
			}
		}
	}

	private void reactOnQueryTimeoutExceptionWhileExtracting(QueryTimeoutException e) {
		timeoutManager.reactOnQueryTimeoutExceptionWhileExtracting(e);
	}

	/**
	 * Decide which object loader to use depending on the targeted entities. If there is only a single entity targeted
	 * a <code>QueryLoader</code> can be used which will only execute a single query to load the entities. If more than
	 * one entity is targeted a <code>MultiClassesQueryLoader</code> must be used. We also have to consider whether
	 * projections or <code>Criteria</code> are used.
	 *
	 * @return The loader instance to use to load the results of the query.
	 */
	private Loader getLoader() {
		ObjectLoaderBuilder loaderBuilder = new ObjectLoaderBuilder()
				.criteria(criteria)
				.targetedEntities(targetedEntities)
				.indexedTargetedEntities(indexedTargetedEntities)
				.session(session)
				.searchFactory(searchFactoryImplementor)
				.timeoutManager(timeoutManager);
		if ( indexProjection != null ) {
			return getProjectionLoader( loaderBuilder );
		}
		else {
			return loaderBuilder.buildLoader();
		}
	}

	private Loader getProjectionLoader(ObjectLoaderBuilder loaderBuilder) {
		ProjectionLoader loader = new ProjectionLoader();
		loader.init( ( Session ) session, searchFactoryImplementor, resultTransformer, loaderBuilder, indexProjection, timeoutManager );
		return loader;
	}

	public ScrollableResults scroll() throws HibernateException {
		//keep the searcher open until the resultset is closed
		timeoutManager.start( luceneQuery );
		//find the directories
		IndexSearcherWithPayload searcher = buildSearcher( searchFactoryImplementor );
		//FIXME: handle null searcher
		try {
			QueryHits queryHits = getQueryHits( searcher, calculateTopDocsRetrievalSize() );
			int first = first();
			int max = max( first, queryHits.totalHits );
			DocumentExtractor extractor = new DocumentExtractor(
					queryHits, searchFactoryImplementor, indexProjection, idFieldNames, allowFieldSelectionInProjection
			);
			Loader loader = getLoader();
			//stop timeout manager, the iterator pace is in the user's hands
			timeoutManager.stop();
			return new ScrollableResultsImpl(
					searcher.getSearcher(), first, max, fetchSize, extractor, loader, searchFactoryImplementor, this.session
			);
		}
		catch ( IOException e ) {
			//close only in case of exception
			try {
				closeSearcher( searcher.getSearcher(), searchFactoryImplementor.getReaderProvider() );
			}
			catch ( SearchException ee ) {
				//we have the initial issue already
			}
			throw new HibernateException( "Unable to query Lucene index", e );
		}
	}

	public ScrollableResults scroll(ScrollMode scrollMode) throws HibernateException {
		//TODO think about this scrollmode
		return scroll();
	}

	public List list() throws HibernateException {
		//find the directories
		timeoutManager.start(luceneQuery);
		IndexSearcherWithPayload searcher = buildSearcher( searchFactoryImplementor );
		if ( searcher == null ) {
			return Collections.EMPTY_LIST;
		}
		try {
			QueryHits queryHits = getQueryHits( searcher, calculateTopDocsRetrievalSize() );
			int first = first();
			int max = max( first, queryHits.totalHits );

			int size = max - first + 1 < 0 ? 0 : max - first + 1;
			List<EntityInfo> infos = new ArrayList<EntityInfo>( size );
			DocumentExtractor extractor = new DocumentExtractor(
					queryHits, searchFactoryImplementor, indexProjection, idFieldNames, allowFieldSelectionInProjection
			);
			try {
				for ( int index = first; index <= max; index++ ) {
					infos.add( extractor.extract( index ) );
					//TODO should we measure on each extractor?
					if ( index % 10 == 0 ) timeoutManager.isTimedOut();
				}
			}
			catch ( QueryTimeoutException e ) {
				reactOnQueryTimeoutExceptionWhileExtracting( e );
			}
			Loader loader = getLoader();
			List list = loader.load( infos.toArray( new EntityInfo[infos.size()] ) );
			//no need to timeoutManager.isTimedOut from this point, we don't do anything intensive
			if ( resultTransformer == null || loader instanceof ProjectionLoader ) {
				//stay consistent with transformTuple which can only be executed during a projection
				return list;
			}
			else {
				return resultTransformer.transformList( list );
			}
		}
		catch ( IOException e ) {
			throw new HibernateException( "Unable to query Lucene index", e );
		}
		finally {
			try {
				closeSearcher( searcher.getSearcher(), searchFactoryImplementor.getReaderProvider() );
			}
			catch ( SearchException e ) {
				log.warn( "Unable to properly close searcher during lucene query: " + getQueryString(), e );
			}
		}
	}

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
			org.apache.lucene.search.Query query = filterQueryByClasses( luceneQuery );
			buildFilters();
			explanation = searcher.getSearcher().explain( query, documentId );
		}
		catch ( IOException e ) {
			throw new HibernateException( "Unable to query Lucene index and build explanation", e );
		}
		finally {
			//searcher cannot be null
			try {
				closeSearcher( searcher.getSearcher(), searchFactoryImplementor.getReaderProvider() );
			}
			catch ( SearchException e ) {
				log.warn( "Unable to properly close searcher during lucene query: " + getQueryString(), e );
			}
		}
		return explanation;
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
		org.apache.lucene.search.Query query = filterQueryByClasses( luceneQuery );
		buildFilters();
		QueryHits queryHits;

		boolean stats = searchFactoryImplementor.getStatistics().isStatisticsEnabled();
		long startTime = 0;
		if ( stats ) {
			startTime = System.nanoTime();
		}

		if ( n == null ) { // try to make sure that we get the right amount of top docs
			queryHits = new QueryHits( searcher, query, filter, sort, timeoutManager );
		}
		else {
			queryHits = new QueryHits( searcher, query, filter, sort, n, timeoutManager );
		}
		resultSize = queryHits.totalHits;

		if ( stats ) {
			searchFactoryImplementor.getStatisticsImplementor().searchExecuted( query.toString(), System.nanoTime() - startTime );
		}

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
			long tmpMaxResult = ( long ) first() + maxResults;
			if ( tmpMaxResult >= Integer.MAX_VALUE ) {
				// don't return just Integer.MAX_VALUE due to a bug in Lucene - see HSEARCH-330
				return Integer.MAX_VALUE - 1;
			}
			if (tmpMaxResult == 0) {
				return 1; // Lucene enforces that at least one top doc will be retrieved. See also HSEARCH-604
			}
			else {
				return ( int ) tmpMaxResult;
			}
		}
	}

	private void buildFilters() {
		ChainedFilter chainedFilter = null;
		if ( !filterDefinitions.isEmpty() ) {
			chainedFilter = new ChainedFilter();
			for ( FullTextFilterImpl fullTextFilter : filterDefinitions.values() ) {
				Filter filter = buildLuceneFilter( fullTextFilter );
				if ( filter != null ) {
					chainedFilter.addFilter( filter );
				}
			}
		}

		if ( userFilter != null ) {
			//chainedFilter is not always necessary here but the code is easier to read
			if ( chainedFilter == null ) {
				chainedFilter = new ChainedFilter();
			}
			chainedFilter.addFilter( userFilter );
		}

		if ( chainedFilter == null || chainedFilter.isEmpty() ) {
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
				filter = ( Filter ) def.getFactoryMethod().invoke( instance );
			}
			catch ( IllegalAccessException e ) {
				throw new SearchException(
						"Unable to access @Factory method: "
								+ def.getImpl().getName() + "." + def.getFactoryMethod().getName(), e
				);
			}
			catch ( InvocationTargetException e ) {
				throw new SearchException(
						"Unable to access @Factory method: "
								+ def.getImpl().getName() + "." + def.getFactoryMethod().getName(), e
				);
			}
			catch ( ClassCastException e ) {
				throw new SearchException(
						"@Key method does not return a org.apache.lucene.search.Filter class: "
								+ def.getImpl().getName() + "." + def.getFactoryMethod().getName(), e
				);
			}
		}
		else {
			try {
				filter = ( Filter ) instance;
			}
			catch ( ClassCastException e ) {
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
			filter = new org.hibernate.search.filter.CachingWrapperFilter( filter, cachingWrapperFilterSize );
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
				public int hashCode() {
					return getImpl().hashCode();
				}

				public boolean equals(Object obj) {
					if ( !( obj instanceof FilterKey ) ) {
						return false;
					}
					FilterKey that = ( FilterKey ) obj;
					return this.getImpl().equals( that.getImpl() );
				}
			};
		}
		else {
			try {
				key = ( FilterKey ) def.getKeyMethod().invoke( instance );
			}
			catch ( IllegalAccessException e ) {
				throw new SearchException(
						"Unable to access @Key method: "
								+ def.getImpl().getName() + "." + def.getKeyMethod().getName()
				);
			}
			catch ( InvocationTargetException e ) {
				throw new SearchException(
						"Unable to access @Key method: "
								+ def.getImpl().getName() + "." + def.getKeyMethod().getName()
				);
			}
			catch ( ClassCastException e ) {
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

	private Object createFilterInstance(FullTextFilterImpl fullTextFilter,
										FilterDef def) {
		Object instance;
		try {
			instance = def.getImpl().newInstance();
		}
		catch ( InstantiationException e ) {
			throw new SearchException( "Unable to create @FullTextFilterDef: " + def.getImpl(), e );
		}
		catch ( IllegalAccessException e ) {
			throw new SearchException( "Unable to create @FullTextFilterDef: " + def.getImpl(), e );
		}
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
				Term t = new Term( DocumentBuilder.CLASS_FIELDNAME, clazz.getName() );
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
		if ( maxResults == null ) {
			return totalHits - 1;
		}
		else {
			return maxResults + first < totalHits ?
					first + maxResults - 1 :
					totalHits - 1;
		}
	}

	private int first() {
		return firstResult != null ?
				firstResult :
				0;
	}

	private IndexSearcherWithPayload buildSearcher(SearchFactoryImplementor searchFactoryImplementor) {
		return buildSearcher(searchFactoryImplementor, null);
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
		Map<Class<?>, DocumentBuilderIndexedEntity<?>> builders = searchFactoryImplementor.getDocumentBuildersIndexedEntities();
		List<DirectoryProvider> targetedDirectories = new ArrayList<DirectoryProvider>();
		Set<String> idFieldNames = new HashSet<String>();

		Similarity searcherSimilarity = null;
		//TODO check if caching this work for the last n list of indexedTargetedEntities makes a perf boost
		if ( indexedTargetedEntities.size() == 0 ) {
			// empty indexedTargetedEntities array means search over all indexed enities,
			// but we have to make sure there is at least one
			if ( builders.isEmpty() ) {
				throw new HibernateException(
						"There are no mapped entities. Don't forget to add @Indexed to at least one class."
				);
			}

			for ( DocumentBuilderIndexedEntity builder : builders.values() ) {
				searcherSimilarity = checkSimilarity( searcherSimilarity, builder );
				if ( builder.getIdKeywordName() != null ) {
					idFieldNames.add( builder.getIdKeywordName() );
					allowFieldSelectionInProjection = allowFieldSelectionInProjection && builder.allowFieldSelectionInProjection();
				}
				populateDirectories( targetedDirectories, builder );
			}
			classesAndSubclasses = null;
		}
		else {
			Set<Class<?>> involvedClasses = new HashSet<Class<?>>( indexedTargetedEntities.size() );
			involvedClasses.addAll( indexedTargetedEntities );
			for ( Class<?> clazz : indexedTargetedEntities ) {
				DocumentBuilderIndexedEntity<?> builder = builders.get( clazz );
				if ( builder != null ) {
					involvedClasses.addAll( builder.getMappedSubclasses() );
				}
			}

			for ( Class clazz : involvedClasses ) {
				DocumentBuilderIndexedEntity builder = builders.get( clazz );
				//TODO should we rather choose a polymorphic path and allow non mapped entities
				if ( builder == null ) {
					throw new HibernateException( "Not a mapped entity (don't forget to add @Indexed): " + clazz );
				}
				if ( builder.getIdKeywordName() != null ) {
					idFieldNames.add( builder.getIdKeywordName() );
					allowFieldSelectionInProjection = allowFieldSelectionInProjection && builder.allowFieldSelectionInProjection();
				}
				searcherSimilarity = checkSimilarity( searcherSimilarity, builder );
				populateDirectories( targetedDirectories, builder );
			}
			this.classesAndSubclasses = involvedClasses;
		}
		this.idFieldNames = idFieldNames;

		//compute optimization needClassFilterClause
		//if at least one DP contains one class that is not part of the targeted classesAndSubclasses we can't optimize
		if ( classesAndSubclasses != null ) {
			for ( DirectoryProvider dp : targetedDirectories ) {
				final Set<Class<?>> classesInDirectoryProvider = searchFactoryImplementor.getClassesInDirectoryProvider(
						dp
				);
				// if a DP contains only one class, we know for sure it's part of classesAndSubclasses
				if ( classesInDirectoryProvider.size() > 1 ) {
					//risk of needClassFilterClause
					for ( Class clazz : classesInDirectoryProvider ) {
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

		//set up the searcher
		final DirectoryProvider[] directoryProviders = targetedDirectories.toArray(
				new DirectoryProvider[targetedDirectories.size()]
		);
		IndexSearcher is = new IndexSearcher(
				searchFactoryImplementor.getReaderProvider().openReader(
						directoryProviders
				)
		);
		is.setSimilarity( searcherSimilarity );

		//handle the sort and projection
		final String[] projection = this.indexProjection;
		if ( Boolean.TRUE.equals( forceScoring ) ) {
			return new IndexSearcherWithPayload( is, true, true );
		}
		else if ( Boolean.FALSE.equals( forceScoring ) ) {
			return new IndexSearcherWithPayload( is, false, false );
		}
		else if ( this.sort != null && projection != null ) {
			boolean activate = false;
			for(String field : projection) {
				if ( SCORE.equals(field) ) {
					activate = true;
					break;
				}
			}
			if (activate) {
				return new IndexSearcherWithPayload( is, true, false );
			}
		}
		//default
		return new IndexSearcherWithPayload( is, false, false );
	}

	private void populateDirectories(List<DirectoryProvider> directories, DocumentBuilderIndexedEntity builder) {
		final IndexShardingStrategy indexShardingStrategy = builder.getDirectoryProviderSelectionStrategy();
		final DirectoryProvider[] directoryProviders;
		if ( filterDefinitions != null && !filterDefinitions.isEmpty() ) {
			directoryProviders = indexShardingStrategy.getDirectoryProvidersForQuery(
					filterDefinitions.values().toArray( new FullTextFilterImplementor[filterDefinitions.size()] )
			);
		}
		else {
			//no filter get all shards
			directoryProviders = indexShardingStrategy.getDirectoryProvidersForQuery( EMPTY_FULL_TEXT_FILTER_IMPLEMENTOR );
		}

		for ( DirectoryProvider provider : directoryProviders ) {
			if ( !directories.contains( provider ) ) {
				directories.add( provider );
			}
		}
	}

	private Similarity checkSimilarity(Similarity similarity, DocumentBuilderIndexedEntity builder) {
		if ( similarity == null ) {
			similarity = builder.getSimilarity();
		}
		else if ( !similarity.getClass().equals( builder.getSimilarity().getClass() ) ) {
			throw new HibernateException(
					"Cannot perform search on two entities with differing Similarity implementations (" + similarity.getClass()
							.getName() + " & " + builder.getSimilarity().getClass().getName() + ")"
			);
		}

		return similarity;
	}

	private void closeSearcher(Searcher searcher, ReaderProvider readerProvider) {
		Set<IndexReader> indexReaders = getIndexReaders( searcher );

		for ( IndexReader indexReader : indexReaders ) {
			readerProvider.closeReader( indexReader );
		}
	}

	public int getResultSize() {
		if ( resultSize == null ) {
			//the timeoutManager does not need to be stopped nor reset as a start does indeed reset
			timeoutManager.start( luceneQuery );
			//get result size without object initialization
			IndexSearcherWithPayload searcher = buildSearcher( searchFactoryImplementor, false );
			if ( searcher == null ) {
				resultSize = 0;
			}
			else {
				TopDocs hits;
				try {
					hits = getQueryHits(
							searcher, 1
					).topDocs; // Lucene enforces that at least one top doc will be retrieved.
					resultSize = hits.totalHits;
				}
				catch ( IOException e ) {
					throw new HibernateException( "Unable to query Lucene index", e );
				}
				finally {
					//searcher cannot be null
					try {
						closeSearcher( searcher.getSearcher(), searchFactoryImplementor.getReaderProvider() );
						//searchFactoryImplementor.getReaderProvider().closeReader( searcher.getIndexReader() );
					}
					catch ( SearchException e ) {
						log.warn( "Unable to properly close searcher during lucene query: " + getQueryString(), e );
					}
				}
			}
		}
		return this.resultSize;
	}

	public FullTextQuery setCriteriaQuery(Criteria criteria) {
		this.criteria = criteria;
		return this;
	}

	public FullTextQuery setProjection(String... fields) {
		if ( fields == null || fields.length == 0 ) {
			this.indexProjection = null;
		}
		else {
			this.indexProjection = fields;
		}
		return this;
	}

	public FullTextQuery setFirstResult(int firstResult) {
		if ( firstResult < 0 ) {
			throw new IllegalArgumentException( "'first' pagination parameter less than 0" );
		}
		this.firstResult = firstResult;
		return this;
	}

	public FullTextQuery setMaxResults(int maxResults) {
		if ( maxResults < 0 ) {
			throw new IllegalArgumentException( "'max' pagination parameter less than 0" );
		}
		this.maxResults = maxResults;
		return this;
	}

	public FullTextQuery setFetchSize(int fetchSize) {
		super.setFetchSize( fetchSize );
		if ( fetchSize <= 0 ) {
			throw new IllegalArgumentException( "'fetch size' parameter less than or equals to 0" );
		}
		this.fetchSize = fetchSize;
		return this;
	}

	public Query setLockOptions(LockOptions lockOptions) {
		throw new UnsupportedOperationException( "Lock options are not implemented in Hibernate Search queries" );
	}

	@Override
	public FullTextQuery setResultTransformer(ResultTransformer transformer) {
		super.setResultTransformer( transformer );
		this.resultTransformer = transformer;
		return this;
	}

	public <T> T unwrap(Class<T> type) {
		if ( type == org.apache.lucene.search.Query.class ) {
			return ( T ) luceneQuery;
		}
		throw new IllegalArgumentException( "Cannot unwrap " + type.getName() );
	}

	public LockOptions getLockOptions() {
		throw new UnsupportedOperationException( "Lock options are not implemented in Hibernate Search queries" );
	}

	public int executeUpdate() throws HibernateException {
		throw new UnsupportedOperationException( "executeUpdate is not supported in Hibernate Search queries" );
	}

	public Query setLockMode(String alias, LockMode lockMode) {
		throw new UnsupportedOperationException( "Lock options are not implemented in Hibernate Search queries" );
	}

	protected Map getLockModes() {
		throw new UnsupportedOperationException( "Lock options are not implemented in Hibernate Search queries" );
	}

	public FullTextFilter enableFullTextFilter(String name) {
		FullTextFilterImpl filterDefinition = filterDefinitions.get( name );
		if ( filterDefinition != null ) {
			return filterDefinition;
		}

		filterDefinition = new FullTextFilterImpl();
		filterDefinition.setName( name );
		FilterDef filterDef = searchFactoryImplementor.getFilterDefinition( name );
		if ( filterDef == null ) {
			throw new SearchException( "Unkown @FullTextFilter: " + name );
		}
		filterDefinitions.put( name, filterDefinition );
		return filterDefinition;
	}

	public void disableFullTextFilter(String name) {
		filterDefinitions.remove( name );
	}

	@Override
	public FullTextQuery setTimeout(int timeout) {
		return setTimeout( timeout, TimeUnit.SECONDS );
	}

	public FullTextQuery setTimeout(long timeout, TimeUnit timeUnit) {
		super.setTimeout( (int)timeUnit.toSeconds( timeout ) );
		timeoutManager.setTimeout( timeout, timeUnit );
		timeoutManager.raiseExceptionOnTimeout();
		return this;
	}

	public FullTextQuery limitFetchingTime(long timeout, TimeUnit timeUnit) {
		timeoutManager.setTimeout( timeout, timeUnit );
		timeoutManager.limitFetchingOnTimeout();
		return this;
	}

	public boolean hasPartialResults() {
		return timeoutManager.hasPartialResults();
	}

	private SearchFactoryImplementor getSearchFactoryImplementor() {
		if ( searchFactoryImplementor == null ) {
			searchFactoryImplementor = ContextHelper.getSearchFactoryBySFI( session );
		}
		return searchFactoryImplementor;
	}

	private static Loader noLoader = new Loader() {
		public void init(Session session, SearchFactoryImplementor searchFactoryImplementor, TimeoutManager timeoutManager) {
		}

		public Object load(EntityInfo entityInfo) {
			throw new UnsupportedOperationException( "noLoader should not be used" );
		}

		public Object loadWithoutTiming(EntityInfo entityInfo) {
			throw new UnsupportedOperationException( "noLoader should not be used" );
		}

		public List load(EntityInfo... entityInfos) {
			throw new UnsupportedOperationException( "noLoader should not be used" );
		}
	};
}
