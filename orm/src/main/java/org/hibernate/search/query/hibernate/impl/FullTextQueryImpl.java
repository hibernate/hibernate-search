/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.hibernate.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Sort;
import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.QueryTimeoutException;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.internal.AbstractProducedQuery;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.filter.FullTextFilter;
import org.hibernate.search.hcore.util.impl.ContextHelper;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.query.engine.spi.DocumentExtractor;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.engine.spi.TimeoutExceptionFactory;
import org.hibernate.search.query.engine.spi.TimeoutManager;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spatial.impl.Point;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;

/**
 * Implementation of {@link org.hibernate.search.FullTextQuery}.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@SuppressWarnings("rawtypes") // We extend the raw version of AbstractProducedQuery on purpose, see HSEARCH-2564
public class FullTextQueryImpl extends AbstractProducedQuery implements FullTextQuery {

	private static final Log log = LoggerFactory.make();

	private ObjectLookupMethod objectLookupMethod;
	private DatabaseRetrievalMethod databaseRetrievalMethod;

	private Criteria criteria;
	private ResultTransformer resultTransformer;
	private int fetchSize = 1;
	private final HSQuery hSearchQuery;
	private final SessionImplementor session;


	/**
	 * Constructs a  <code>FullTextQueryImpl</code> instance.
	 *
	 * @param hSearchQuery The query, with the {@link HSQuery#targetedEntities(List) targeted entities} already set if necessary.
	 * @param session Access to the Hibernate session.
	 * @param parameterMetadata Additional query metadata.
	 */
	public FullTextQueryImpl(HSQuery hSearchQuery,
			SessionImplementor session,
			ParameterMetadata parameterMetadata) {
		//TODO handle flushMode
		super( session, parameterMetadata );
		this.session = session;
		ExtendedSearchIntegrator extendedIntegrator = getExtendedSearchIntegrator();
		this.objectLookupMethod = extendedIntegrator.getDefaultObjectLookupMethod();
		this.databaseRetrievalMethod = extendedIntegrator.getDefaultDatabaseRetrievalMethod();

		this.hSearchQuery = hSearchQuery;
		this.hSearchQuery
				.timeoutExceptionFactory( exceptionFactory )
				.tenantIdentifier( session.getTenantIdentifier() );
	}

	@Override
	public FullTextQuery setSort(Sort sort) {
		hSearchQuery.sort( sort );
		return this;
	}

	@Override
	public FullTextQuery setFilter(Filter filter) {
		hSearchQuery.filter( filter );
		return this;
	}

	@Override
	public List getResultList() {
		return list();
	}

	/**
	 * Return an iterator on the results.
	 * Retrieve the object one by one (initialize it during the next() operation)
	 */
	@Override
	public Iterator iterate() {
		//implement an iterator which keep the id/class for each hit and get the object on demand
		//cause I can't keep the searcher and hence the hit opened. I don't have any hook to know when the
		//user stops using it
		//scrollable is better in this area

		hSearchQuery.getTimeoutManager().start();
		final List<EntityInfo> entityInfos = hSearchQuery.queryEntityInfos();
		//stop timeout manager, the iterator pace is in the user's hands
		hSearchQuery.getTimeoutManager().stop();
		//TODO is this no-loader optimization really needed?
		final Iterator<Object> iterator;
		if ( entityInfos.size() == 0 ) {
			iterator = new IteratorImpl( entityInfos, noLoader );
			return iterator;
		}
		else {
			Loader loader = getLoader();
			iterator = new IteratorImpl( entityInfos, loader );
		}
		hSearchQuery.getTimeoutManager().stop();
		return iterator;
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
				.criteria( criteria )
				.targetedEntities( hSearchQuery.getTargetedEntities() )
				.indexedTargetedEntities( hSearchQuery.getIndexedTargetedEntities() )
				.session( session )
				.searchFactory( hSearchQuery.getExtendedSearchIntegrator() )
				.timeoutManager( hSearchQuery.getTimeoutManager() )
				.lookupMethod( objectLookupMethod )
				.retrievalMethod( databaseRetrievalMethod );
		if ( hSearchQuery.getProjectedFields() != null ) {
			return getProjectionLoader( loaderBuilder );
		}
		else {
			return loaderBuilder.buildLoader();
		}
	}

	private Loader getProjectionLoader(ObjectLoaderBuilder loaderBuilder) {
		ProjectionLoader loader = new ProjectionLoader();
		loader.init(
				(Session) session,
				hSearchQuery.getExtendedSearchIntegrator(),
				resultTransformer,
				loaderBuilder,
				hSearchQuery.getProjectedFields(),
				hSearchQuery.getTimeoutManager(),
				hSearchQuery.hasThisProjection()
		);
		return loader;
	}

	@Override
	public ScrollableResultsImpl scroll() {
		//keep the searcher open until the resultset is closed

		hSearchQuery.getTimeoutManager().start();
		final DocumentExtractor documentExtractor = hSearchQuery.queryDocumentExtractor();
		//stop timeout manager, the iterator pace is in the user's hands
		hSearchQuery.getTimeoutManager().stop();
		Loader loader = getLoader();
		return new ScrollableResultsImpl(
				fetchSize,
				documentExtractor,
				loader,
				this.session,
				hSearchQuery.hasThisProjection()
		);
	}

	@Override
	public ScrollableResultsImplementor scroll(ScrollMode scrollMode) {
		//TODO think about this scrollmode
		return scroll();
	}

	@Override
	public List list() {
		hSearchQuery.getTimeoutManager().start();
		final List<EntityInfo> entityInfos = hSearchQuery.queryEntityInfos();
		Loader loader = getLoader();
		List list = loader.load( entityInfos );
		//no need to timeoutManager.isTimedOut from this point, we don't do anything intensive
		if ( resultTransformer == null || loader instanceof ProjectionLoader ) {
			//stay consistent with transformTuple which can only be executed during a projection
			//nothing to do
		}
		else {
			list = resultTransformer.transformList( list );
		}
		hSearchQuery.getTimeoutManager().stop();
		return list;
	}

	@Override
	public Explanation explain(int documentId) {
		return hSearchQuery.explain( documentId );
	}

	@Override
	public int getResultSize() {
		if ( getLoader().isSizeSafe() ) {
			return hSearchQuery.queryResultSize();
		}
		else {
			throw log.cannotGetResultSizeWithCriteriaAndRestriction( criteria.toString() );
		}
	}

	@Override
	public FullTextQuery setCriteriaQuery(Criteria criteria) {
		this.criteria = criteria;
		return this;
	}

	@Override
	public FullTextQuery setProjection(String... fields) {
		hSearchQuery.projection( fields );
		return this;
	}

	@Override
	public FullTextQuery setSpatialParameters(Coordinates center, String fieldName) {
		hSearchQuery.setSpatialParameters( center, fieldName );
		return this;
	}

	@Override
	public FullTextQuery setSpatialParameters(double latitude, double longitude, String fieldName) {
		setSpatialParameters( Point.fromDegrees( latitude, longitude ), fieldName );
		return this;
	}

	@Override
	public FullTextQuery setFirstResult(int firstResult) {
		hSearchQuery.firstResult( firstResult );
		return this;
	}

	@Override
	public FullTextQuery setMaxResults(int maxResults) {
		hSearchQuery.maxResults( maxResults );
		return this;
	}

	@Override
	public FullTextQuery setFetchSize(int fetchSize) {
		super.setFetchSize( fetchSize );
		if ( fetchSize <= 0 ) {
			throw new IllegalArgumentException( "'fetch size' parameter less than or equals to 0" );
		}
		this.fetchSize = fetchSize;
		return this;
	}

	@Override
	public QueryImplementor setLockOptions(LockOptions lockOptions) {
		throw new UnsupportedOperationException( "Lock options are not implemented in Hibernate Search queries" );
	}

	@Override
	public FullTextQuery setResultTransformer(ResultTransformer transformer) {
		super.setResultTransformer( transformer );
		this.resultTransformer = transformer;
		return this;
	}

	/*
	 * Implementation note: this method is defined as generic in the interface,
	 * but we must implement it without generics (otherwise it won't compile).
	 *
	 * The actual reason is a bit hard to explain: basically we implement
	 * javax.persistence.Query as a raw type at some point, and our superclass
	 * (also extended as a raw type) also implements this interface, but as a non-raw type.
	 * This seems to confuse the compiler, which thinks there are two different methods.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Object unwrap(Class type) {
		if ( type == org.apache.lucene.search.Query.class ) {
			return hSearchQuery.getLuceneQuery();
		}
		throw new IllegalArgumentException( "Cannot unwrap " + type.getName() );
	}

	@Override
	public LockOptions getLockOptions() {
		throw new UnsupportedOperationException( "Lock options are not implemented in Hibernate Search queries" );
	}

	@Override
	public int executeUpdate() {
		throw new UnsupportedOperationException( "executeUpdate is not supported in Hibernate Search queries" );
	}

	@Override
	public QueryImplementor setLockMode(String alias, LockMode lockMode) {
		throw new UnsupportedOperationException( "Lock options are not implemented in Hibernate Search queries" );
	}

	protected Map getLockModes() {
		throw new UnsupportedOperationException( "Lock options are not implemented in Hibernate Search queries" );
	}

	@Override
	public FullTextFilter enableFullTextFilter(String name) {
		return hSearchQuery.enableFullTextFilter( name );
	}

	@Override
	public void disableFullTextFilter(String name) {
		hSearchQuery.disableFullTextFilter( name );
	}

	@Override
	public FacetManager getFacetManager() {
		return hSearchQuery.getFacetManager();
	}

	@Override
	public FullTextQuery setTimeout(int timeout) {
		return setTimeout( timeout, TimeUnit.SECONDS );
	}

	@Override
	public FullTextQuery setTimeout(long timeout, TimeUnit timeUnit) {
		super.setTimeout( (int) timeUnit.toSeconds( timeout ) );
		hSearchQuery.getTimeoutManager().setTimeout( timeout, timeUnit );
		hSearchQuery.getTimeoutManager().raiseExceptionOnTimeout();
		return this;
	}

	@Override
	public FullTextQuery limitExecutionTimeTo(long timeout, TimeUnit timeUnit) {
		hSearchQuery.getTimeoutManager().setTimeout( timeout, timeUnit );
		hSearchQuery.getTimeoutManager().limitFetchingOnTimeout();
		return this;
	}

	@Override
	public boolean hasPartialResults() {
		return hSearchQuery.getTimeoutManager().hasPartialResults();
	}

	@Override
	public FullTextQuery initializeObjectsWith(ObjectLookupMethod lookupMethod, DatabaseRetrievalMethod retrievalMethod) {
		this.objectLookupMethod = lookupMethod;
		this.databaseRetrievalMethod = retrievalMethod;
		return this;
	}

	private ExtendedSearchIntegrator getExtendedSearchIntegrator() {
		return ContextHelper.getSearchIntegratorBySessionImplementor( session );
	}

	@Override
	public String getQueryString() {
		return hSearchQuery.getQueryString();
	}

	@Override
	protected boolean isNativeQuery() {
		return false;
	}

	@Override
	public Type[] getReturnTypes() {
		throw new UnsupportedOperationException( "getReturnTypes() is not implemented in Hibernate Search queries" );
	}

	@Override
	public String[] getReturnAliases() {
		throw new UnsupportedOperationException( "getReturnAliases() is not implemented in Hibernate Search queries" );
	}

	@Override
	public FullTextQuery setEntity(int position, Object val) {
		throw new UnsupportedOperationException( "setEntity(int,Object) is not implemented in Hibernate Search queries" );
	}

	@Override
	public FullTextQuery setEntity(String name, Object val) {
		throw new UnsupportedOperationException( "setEntity(String,Object) is not implemented in Hibernate Search queries" );
	}

	@Override
	public String toString() {
		return "FullTextQueryImpl(" + getQueryString() + ")";
	}

	private static final Loader noLoader = new Loader() {
		@Override
		public void init(Session session,
						ExtendedSearchIntegrator extendedIntegrator,
						ObjectInitializer objectInitializer,
						TimeoutManager timeoutManager) {
		}

		@Override
		public Object load(EntityInfo entityInfo) {
			throw new UnsupportedOperationException( "noLoader should not be used" );
		}

		@Override
		public Object loadWithoutTiming(EntityInfo entityInfo) {
			throw new UnsupportedOperationException( "noLoader should not be used" );
		}

		@Override
		public List load(List<EntityInfo> entityInfos) {
			throw new UnsupportedOperationException( "noLoader should not be used" );
		}

		@Override
		public boolean isSizeSafe() {
			return false;
		}
	};

	private static final TimeoutExceptionFactory exceptionFactory = new TimeoutExceptionFactory() {

		@Override
		public RuntimeException createTimeoutException(String message, String queryDescription) {
			return new QueryTimeoutException( message, null, queryDescription );
		}

	};
}
