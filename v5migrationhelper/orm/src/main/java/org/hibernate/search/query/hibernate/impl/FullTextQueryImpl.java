/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.hibernate.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.QueryTimeoutException;
import jakarta.persistence.TemporalType;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.TypeMismatchException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.hql.internal.QueryExecutionRequestException;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.internal.AbstractProducedQuery;
import org.hibernate.query.internal.ParameterMetadataImpl;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.impl.V5MigrationOrmSearchIntegratorAdapter;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.orm.search.loading.dsl.SearchLoadingOptionsStep;
import org.hibernate.search.mapper.orm.search.query.spi.HibernateOrmSearchQueryHints;
import org.hibernate.search.mapper.orm.search.query.spi.HibernateOrmSearchScrollableResultsAdapter;
import org.hibernate.search.mapper.orm.search.query.spi.HibernateOrmSearchScrollableResultsAdapter.ScrollHitExtractor;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.engine.spi.V5MigrationSearchSession;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spatial.impl.Point;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

/**
 * Implementation of {@link org.hibernate.search.FullTextQuery}.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@SuppressWarnings("rawtypes") // We extend the raw version of AbstractProducedQuery on purpose, see HSEARCH-2564
public class FullTextQueryImpl extends AbstractProducedQuery implements FullTextQuery {

	private final V5MigrationSearchSession<SearchLoadingOptionsStep> searchSession;

	private final HSQuery hSearchQuery;

	private Integer firstResult;
	private Integer maxResults;
	//initialized at 0 since we don't expect to use hints at this stage
	private final Map<String, Object> hints = new HashMap<String, Object>( 0 );

	private Integer fetchSize = null;
	private EntityLoadingCacheLookupStrategy cacheLookupStrategy = null;
	private List<EntityGraphHint> entityGraphHints = new ArrayList<>();
	private final Consumer<SearchLoadingOptionsStep> loadingOptionsContributor = o -> {
		if ( cacheLookupStrategy != null ) {
			o.cacheLookupStrategy( cacheLookupStrategy );
		}
		if ( fetchSize != null ) {
			o.fetchSize( fetchSize );
		}
		for ( EntityGraphHint hint : entityGraphHints ) {
			o.graph( hint.graph, hint.semantic );
		}
	};

	private ResultTransformer resultTransformer;

	public FullTextQueryImpl(Query luceneQuery, SessionImplementor session,
			V5MigrationOrmSearchIntegratorAdapter searchIntegrator,
			V5MigrationSearchSession<SearchLoadingOptionsStep> searchSession,
			Class<?>... entities) {
		super( session, new ParameterMetadataImpl( null, null ) );
		this.searchSession = searchSession;
		this.hSearchQuery = searchIntegrator.createHSQuery( luceneQuery, searchSession,
				loadingOptionsContributor, entities );
	}

	@Override
	public FullTextQueryImpl setSort(Sort sort) {
		hSearchQuery.sort( sort );
		return this;
	}

	@Override
	public List getResultList() {
		return list();
	}

	@Override
	public Iterator iterate() {
		throw new UnsupportedOperationException(
				"iterate() is not implemented in Hibernate Search queries. Use scroll() instead." );
	}

	@Override
	public ScrollableResultsImplementor scroll() {
		SearchScroll<?> scroll = hSearchQuery.scroll(
				fetchSize != null ? fetchSize : 100 );
		return new HibernateOrmSearchScrollableResultsAdapter<>( scroll,
				maxResults != null ? maxResults : Integer.MAX_VALUE,
				Search5ScrollHitExtractor.INSTANCE );
	}

	@Override
	public ScrollableResultsImplementor scroll(ScrollMode scrollMode) {
		return scroll();
	}

	@Override
	public List list() {
		// Reproduce the behavior of AbstractProducedQuery.list() regarding exceptions
		try {
			return doHibernateSearchList();
		}
		catch (SearchTimeoutException e) {
			throw new QueryTimeoutException( e );
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
		List list = hSearchQuery.fetch();

		if ( resultTransformer != null ) {
			list = resultTransformer.transformList( list );
		}

		return list;
	}

	@Override
	public Explanation explain(Object entityId) {
		return hSearchQuery.explain( entityId );
	}

	@Override
	public int getResultSize() {
		try {
			return doGetResultSize();
		}
		catch (SearchTimeoutException e) {
			throw new QueryTimeoutException( e );
		}
		catch (HibernateException he) {
			throw getExceptionConverter().convert( he );
		}
	}

	public int doGetResultSize() {
		return hSearchQuery.getResultSize();
	}

	@Override
	public FullTextQueryImpl applyGraph(RootGraph graph, GraphSemantic semantic) {
		entityGraphHints.add( new EntityGraphHint<>( graph, semantic ) );
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
	@SuppressWarnings("deprecation")
	public FullTextQuery setHint(String hintName, Object value) {
		hints.put( hintName, value );
		switch ( hintName ) {
			case HibernateOrmSearchQueryHints.JAVAX_TIMEOUT:
			case HibernateOrmSearchQueryHints.JAKARTA_TIMEOUT:
				setTimeout( hintValueToInteger( value ), TimeUnit.MILLISECONDS );
				break;
			case HibernateOrmSearchQueryHints.HIBERNATE_TIMEOUT:
				setTimeout( hintValueToInteger( value ) );
				break;
			case HibernateOrmSearchQueryHints.JAVAX_FETCHGRAPH:
			case HibernateOrmSearchQueryHints.JAKARTA_FETCHGRAPH:
				applyGraph( hintValueToEntityGraph( value ), GraphSemantic.FETCH );
				break;
			case HibernateOrmSearchQueryHints.JAVAX_LOADGRAPH:
			case HibernateOrmSearchQueryHints.JAKARTA_LOADGRAPH:
				applyGraph( hintValueToEntityGraph( value ), GraphSemantic.LOAD );
				break;
			default:
				handleUnrecognizedHint( hintName, value );
				break;
		}
		return this;
	}

	@Override
	public Map<String, Object> getHints() {
		return hints;
	}

	@Override // No generics, see unwrap() (same issue)
	public FullTextQueryImpl setParameter(Parameter tParameter, Object t) {
		throw parametersNoSupported();
	}

	@Override // No generics, see unwrap() (same issue)
	public FullTextQueryImpl setParameter(Parameter calendarParameter, Calendar calendar, TemporalType temporalType) {
		throw parametersNoSupported();
	}

	@Override // No generics, see unwrap() (same issue)
	public FullTextQueryImpl setParameter(Parameter dateParameter, Date date, TemporalType temporalType) {
		throw parametersNoSupported();
	}

	@Override
	public FullTextQueryImpl setParameter(String name, Object value) {
		throw parametersNoSupported();
	}

	@Override
	public FullTextQueryImpl setParameter(String name, Date value, TemporalType temporalType) {
		throw parametersNoSupported();
	}

	@Override
	public FullTextQueryImpl setParameter(String name, Calendar value, TemporalType temporalType) {
		throw parametersNoSupported();
	}

	@Override
	public FullTextQueryImpl setParameter(int position, Object value) {
		throw parametersNoSupported();
	}

	@Override
	public FullTextQueryImpl setParameter(int position, Date value, TemporalType temporalType) {
		throw parametersNoSupported();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<Parameter<?>> getParameters() {
		return Collections.EMPTY_SET;
	}

	@Override
	protected QueryParameterBindings getQueryParameterBindings() {
		throw parametersNoSupported();
	}

	@Override
	public FullTextQueryImpl setParameter(int position, Calendar value, TemporalType temporalType) {
		throw parametersNoSupported();
	}

	@Override
	public QueryParameter<?> getParameter(String name) {
		throw parametersNoSupported();
	}

	@Override
	public QueryParameter<?> getParameter(int position) {
		throw parametersNoSupported();
	}

	@Override // No generics, see unwrap() (same issue)
	public QueryParameter getParameter(String name, Class type) {
		throw parametersNoSupported();
	}

	@Override // No generics, see unwrap() (same issue)
	public QueryParameter getParameter(int position, Class type) {
		throw parametersNoSupported();
	}

	@Override // No generics, see unwrap() (same issue)
	public boolean isBound(Parameter param) {
		throw parametersNoSupported();
	}

	@Override // No generics, see unwrap() (same issue)
	public Object getParameterValue(Parameter param) {
		throw parametersNoSupported();
	}

	@Override
	public Object getParameterValue(String name) {
		throw parametersNoSupported();
	}

	@Override
	public Object getParameterValue(int position) {
		throw parametersNoSupported();
	}

	private UnsupportedOperationException parametersNoSupported() {
		return new UnsupportedOperationException( "Parameters are not supported in Hibernate Search queries" );
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
		throw lockOptionsNotSupported();
	}

	@Deprecated
	@Override
	public FullTextQueryImpl setResultTransformer(ResultTransformer transformer) {
		resultTransformer = transformer;
		if ( transformer != null ) {
			hSearchQuery.tupleTransformer( (tuple, fields) -> resultTransformer.transformTuple( tuple, fields ) );
		}
		else {
			hSearchQuery.tupleTransformer( null );
		}
		return this;
	}

	/*
	 * Implementation note: this method is defined as generic in the interface,
	 * but we must implement it without generics (otherwise it won't compile).
	 *
	 * The actual reason is a bit hard to explain: basically we implement
	 * jakarta.persistence.Query as a raw type at some point, and our superclass
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
		throw lockOptionsNotSupported();
	}

	@Override
	public LockModeType getLockMode() {
		throw lockOptionsNotSupported();
	}

	@Override
	public LockOptions getLockOptions() {
		throw lockOptionsNotSupported();
	}

	private UnsupportedOperationException lockOptionsNotSupported() {
		return new UnsupportedOperationException( "Lock options are not supported in Hibernate Search queries" );
	}

	@Override
	public int executeUpdate() {
		throw new UnsupportedOperationException( "executeUpdate is not supported in Hibernate Search queries" );
	}

	@Override
	public QueryImplementor setLockMode(String alias, LockMode lockMode) {
		throw lockOptionsNotSupported();
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
		hSearchQuery.failAfter( timeout, timeUnit );
		return this;
	}

	@Override
	public FullTextQueryImpl limitExecutionTimeTo(long timeout, TimeUnit timeUnit) {
		hSearchQuery.truncateAfter( timeout, timeUnit );
		return this;
	}

	@Override
	public boolean hasPartialResults() {
		return hSearchQuery.hasPartialResults();
	}

	@Override
	public FullTextQueryImpl initializeObjectsWith(ObjectLookupMethod lookupMethod, DatabaseRetrievalMethod retrievalMethod) {
		switch ( lookupMethod ) {
			case SKIP:
				this.cacheLookupStrategy = EntityLoadingCacheLookupStrategy.SKIP;
				break;
			case PERSISTENCE_CONTEXT:
				this.cacheLookupStrategy = EntityLoadingCacheLookupStrategy.PERSISTENCE_CONTEXT;
				break;
			case SECOND_LEVEL_CACHE:
				this.cacheLookupStrategy = EntityLoadingCacheLookupStrategy.PERSISTENCE_CONTEXT_THEN_SECOND_LEVEL_CACHE;
				break;
		}
		return this;
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

	private static final class Search5ScrollHitExtractor
			implements ScrollHitExtractor<Object> {

		private static final Search5ScrollHitExtractor INSTANCE = new Search5ScrollHitExtractor();

		@Override
		public Object[] toArray(Object hit) {
			if ( hit instanceof Object[] ) {
				return (Object[]) hit;
			}
			else {
				return new Object[] { hit };
			}
		}

		@Override
		public Object toElement(Object hit, int index) {
			if ( hit instanceof Object[] ) {
				return ( (Object[]) hit )[index];
			}
			else if ( index != 0 ) {
				throw new IndexOutOfBoundsException();
			}
			else {
				return hit;
			}
		}
	}

	private static int hintValueToInteger(Object value) {
		if ( value instanceof Number ) {
			return ( (Number) value ).intValue();
		}
		else {
			return Integer.parseInt( String.valueOf( value ) );
		}
	}

	private static RootGraph<?> hintValueToEntityGraph(Object value) {
		return (RootGraph) value;
	}
}
