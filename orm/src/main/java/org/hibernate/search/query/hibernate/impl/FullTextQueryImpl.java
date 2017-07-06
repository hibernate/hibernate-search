/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.hibernate.impl;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.TemporalType;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Sort;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.TypeMismatchException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.hql.internal.QueryExecutionRequestException;
import org.hibernate.query.ParameterMetadata;
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

	private Integer firstResult;
	private Integer maxResults;
	//initialized at 0 since we don't expect to use hints at this stage
	private final Map<String, Object> hints = new HashMap<String, Object>( 0 );

	/**
	 * Constructs a  <code>FullTextQueryImpl</code> instance.
	 *
	 * @param hSearchQuery The query
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
				.timeoutExceptionFactory( new FullTextQueryTimeoutExceptionFactory() )
				.tenantIdentifier( session.getTenantIdentifier() );
	}

	@Override
	public FullTextQueryImpl setSort(Sort sort) {
		hSearchQuery.sort( sort );
		return this;
	}

	@Override
	@Deprecated
	public FullTextQueryImpl setFilter(Filter filter) {
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
				.indexedTargetedEntities( hSearchQuery.getIndexedTargetedEntities().toPojosSet() )
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
				session,
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
		// Reproduce the behavior of AbstractProducedQuery.list() regarding exceptions
		try {
			return doHibernateSearchList();
		}
		catch (QueryExecutionRequestException he) {
			throw new IllegalStateException( he );
		}
		catch (TypeMismatchException e) {
			throw new IllegalArgumentException( e );
		}
		catch (HibernateException he) {
			throw getExceptionConverter().convert( he );
		}
	}

	protected List doHibernateSearchList() {
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
		try {
			return doGetResultSize();
		}
		catch (HibernateException he) {
			throw getExceptionConverter().convert( he );
		}
	}

	public int doGetResultSize() {
		if ( getLoader().isSizeSafe() ) {
			return hSearchQuery.queryResultSize();
		}
		else {
			throw log.cannotGetResultSizeWithCriteriaAndRestriction( criteria.toString() );
		}
	}

	@Override
	public FullTextQueryImpl setCriteriaQuery(Criteria criteria) {
		this.criteria = criteria;
		return this;
	}

	@Override
	public FullTextQueryImpl setProjection(String... fields) {
		hSearchQuery.projection( fields );
		return this;
	}

	@Override
	public FullTextQueryImpl setSpatialParameters(Coordinates center, String fieldName) {
		hSearchQuery.setSpatialParameters( center, fieldName );
		return this;
	}

	@Override
	public FullTextQueryImpl setSpatialParameters(double latitude, double longitude, String fieldName) {
		setSpatialParameters( Point.fromDegrees( latitude, longitude ), fieldName );
		return this;
	}

	@Override
	public FullTextQuery setMaxResults(int maxResults) {
		if ( maxResults < 0 ) {
			throw new IllegalArgumentException(
					"Negative ("
							+ maxResults
							+ ") parameter passed in to setMaxResults"
			);
		}
		hSearchQuery.maxResults( maxResults );
		this.maxResults = maxResults;
		return this;
	}

	@Override
	public int getMaxResults() {
		return maxResults == null || maxResults == -1
				? Integer.MAX_VALUE
				: maxResults;
	}

	@Override
	public FullTextQuery setFirstResult(int firstResult) {
		if ( firstResult < 0 ) {
			throw new IllegalArgumentException(
					"Negative ("
							+ firstResult
							+ ") parameter passed in to setFirstResult"
			);
		}
		hSearchQuery.firstResult( firstResult );
		this.firstResult = firstResult;
		return this;
	}

	@Override
	public int getFirstResult() {
		return firstResult == null ? 0 : firstResult;
	}

	@Override
	public FullTextQuery setHint(String hintName, Object value) {
		hints.put( hintName, value );
		if ( "javax.persistence.query.timeout".equals( hintName ) ) {
			if ( value == null ) {
				//nothing
			}
			else if ( value instanceof String ) {
				setTimeout( Long.parseLong( (String) value ), TimeUnit.MILLISECONDS );
			}
			else if ( value instanceof Number ) {
				setTimeout( ( (Number) value ).longValue(), TimeUnit.MILLISECONDS );
			}
		}
		return this;
	}

	@Override
	public Map<String, Object> getHints() {
		return hints;
	}

	@Override // No generics, see unwrap() (same issue)
	public FullTextQueryImpl setParameter(Parameter tParameter, Object t) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override // No generics, see unwrap() (same issue)
	public FullTextQueryImpl setParameter(Parameter calendarParameter, Calendar calendar, TemporalType temporalType) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override // No generics, see unwrap() (same issue)
	public FullTextQueryImpl setParameter(Parameter dateParameter, Date date, TemporalType temporalType) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public FullTextQueryImpl setParameter(String name, Object value) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public FullTextQueryImpl setParameter(String name, Date value, TemporalType temporalType) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public FullTextQueryImpl setParameter(String name, Calendar value, TemporalType temporalType) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public FullTextQueryImpl setParameter(int position, Object value) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public FullTextQueryImpl setParameter(int position, Date value, TemporalType temporalType) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<Parameter<?>> getParameters() {
		return Collections.EMPTY_SET;
	}

	@Override
	public FullTextQueryImpl setParameter(int position, Calendar value, TemporalType temporalType) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public Parameter<?> getParameter(String name) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public Parameter<?> getParameter(int position) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override // No generics, see unwrap() (same issue)
	public Parameter getParameter(String name, Class type) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override // No generics, see unwrap() (same issue)
	public Parameter getParameter(int position, Class type) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override // No generics, see unwrap() (same issue)
	public boolean isBound(Parameter param) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override // No generics, see unwrap() (same issue)
	public Object getParameterValue(Parameter param) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public Object getParameterValue(String name) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public Object getParameterValue(int position) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public FullTextQueryImpl setFlushMode(FlushModeType flushModeType) {
		return (FullTextQueryImpl) super.setFlushMode( flushModeType );
	}

	@Override
	public FullTextQueryImpl setFetchSize(int fetchSize) {
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

	@Deprecated
	@Override
	public FullTextQueryImpl setResultTransformer(ResultTransformer transformer) {
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
		//I've purposely decided not to return the underlying Hibernate FullTextQuery
		//as I see this as an implementation detail that should not be exposed.
		if ( type == org.apache.lucene.search.Query.class ) {
			return hSearchQuery.getLuceneQuery();
		}
		throw new IllegalArgumentException( "Cannot unwrap " + type.getName() );
	}

	@Override
	public FullTextQueryImpl setLockMode(LockModeType lockModeType) {
		throw new UnsupportedOperationException( "lock modes not supported in fullText queries" );
	}

	@Override
	public LockModeType getLockMode() {
		throw new UnsupportedOperationException( "lock modes not supported in fullText queries" );
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
	public FullTextQueryImpl setTimeout(int timeout) {
		return setTimeout( timeout, TimeUnit.SECONDS );
	}

	@Override
	public FullTextQueryImpl setTimeout(long timeout, TimeUnit timeUnit) {
		super.setTimeout( (int) timeUnit.toSeconds( timeout ) );
		hSearchQuery.getTimeoutManager().setTimeout( timeout, timeUnit );
		hSearchQuery.getTimeoutManager().raiseExceptionOnTimeout();
		return this;
	}

	@Override
	public FullTextQueryImpl limitExecutionTimeTo(long timeout, TimeUnit timeUnit) {
		hSearchQuery.getTimeoutManager().setTimeout( timeout, timeUnit );
		hSearchQuery.getTimeoutManager().limitFetchingOnTimeout();
		return this;
	}

	@Override
	public boolean hasPartialResults() {
		return hSearchQuery.getTimeoutManager().hasPartialResults();
	}

	@Override
	public FullTextQueryImpl initializeObjectsWith(ObjectLookupMethod lookupMethod, DatabaseRetrievalMethod retrievalMethod) {
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

	@Deprecated
	@Override
	public Type[] getReturnTypes() {
		throw new UnsupportedOperationException( "getReturnTypes() is not implemented in Hibernate Search queries" );
	}

	@Deprecated
	@Override
	public String[] getReturnAliases() {
		throw new UnsupportedOperationException( "getReturnAliases() is not implemented in Hibernate Search queries" );
	}

	@Deprecated
	@Override
	public FullTextQueryImpl setEntity(int position, Object val) {
		throw new UnsupportedOperationException( "setEntity(int,Object) is not implemented in Hibernate Search queries" );
	}

	@Deprecated
	@Override
	public FullTextQueryImpl setEntity(String name, Object val) {
		throw new UnsupportedOperationException( "setEntity(String,Object) is not implemented in Hibernate Search queries" );
	}

	@Override
	public String toString() {
		return "FullTextQueryImpl(" + getQueryString() + ")";
	}

	private static final Loader noLoader = new Loader() {
		@Override
		public void init(SessionImplementor session,
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

	private class FullTextQueryTimeoutExceptionFactory implements TimeoutExceptionFactory {

		@Override
		public RuntimeException createTimeoutException(String message, String queryDescription) {
			return new javax.persistence.QueryTimeoutException( message, null, FullTextQueryImpl.this );
		}

	};
}
