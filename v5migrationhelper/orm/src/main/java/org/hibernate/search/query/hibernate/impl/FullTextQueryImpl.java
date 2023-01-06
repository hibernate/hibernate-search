/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.hibernate.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.QueryTimeoutException;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.TypeMismatchException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.spi.AbstractQuery;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.impl.V5MigrationOrmSearchIntegratorAdapter;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.orm.search.loading.dsl.SearchLoadingOptionsStep;
import org.hibernate.search.mapper.orm.search.query.spi.HibernateOrmSearchQueryHints;
import org.hibernate.search.mapper.orm.search.query.spi.HibernateOrmSearchScrollableResultsAdapter;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.engine.spi.V5MigrationSearchSession;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spatial.impl.Point;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.transform.ResultTransformer;

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
public class FullTextQueryImpl extends AbstractQuery implements FullTextQuery {

	private final V5MigrationSearchSession<SearchLoadingOptionsStep> searchSession;

	private final HSQuery hSearchQuery;

	private final SessionImplementor sessionImplementor;
	private final QueryOptionsImpl queryOptions = new QueryOptionsImpl();
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

	public FullTextQueryImpl(Query luceneQuery, SessionImplementor sessionImplementor,
			V5MigrationOrmSearchIntegratorAdapter searchIntegrator,
			V5MigrationSearchSession<SearchLoadingOptionsStep> searchSession,
			Class<?> ... entities) {
		super( sessionImplementor );
		this.sessionImplementor = sessionImplementor;
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
	public ScrollableResultsImplementor scroll() {
		extractQueryOptions();
		SearchScroll<?> scroll = hSearchQuery.scroll( fetchSize != null ? fetchSize : 100 );
		Integer maxResults = hSearchQuery.maxResults();
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
		try {
			return super.list();
		}
		catch (SearchTimeoutException e) {
			throw new QueryTimeoutException( e );
		}
	}

	@Override
	protected List doList() {
		List list = hSearchQuery.fetch();
		if ( resultTransformer != null ) {
			list = resultTransformer.transformList( list );
		}
		return list;
	}

	@Override
	protected void beforeQuery(boolean requiresTxn) {
		super.beforeQuery( requiresTxn );

		extractQueryOptions();
	}

	private void extractQueryOptions() {
		Integer limit = getQueryOptions().getLimit().getMaxRows();
		hSearchQuery.maxResults( limit );
		Integer offset = getQueryOptions().getLimit().getFirstRow();
		hSearchQuery.firstResult( offset == null ? 0 : offset );
		Integer queryFetchSize = getQueryOptions().getFetchSize();
		if ( queryFetchSize != null ) {
			fetchSize = queryFetchSize;
		}
		Integer queryTimeout = getQueryOptions().getTimeout();
		if ( queryTimeout != null ) {
			hSearchQuery.failAfter( queryTimeout, TimeUnit.SECONDS );
		}
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
		catch (IllegalQueryOperationException e) {
			throw new IllegalStateException( e );
		}
		catch (TypeMismatchException e) {
			throw new IllegalArgumentException( e );
		}
		catch (HibernateException he) {
			throw getSession().getExceptionConverter().convert( he, getLockOptions() );
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
		super.setMaxResults( maxResults );
		return this;
	}

	@Override
	public FullTextQuery setFirstResult(int firstResult) {
		super.setFirstResult( firstResult );
		return this;
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
			case HibernateOrmSearchQueryHints.JAVAX_LOADGRAPH:
			case HibernateOrmSearchQueryHints.JAKARTA_LOADGRAPH:
				applyEntityGraphQueryHint( hintName, hintValueToEntityGraph( value ) );
				break;
			default:
				break;
		}
		return this;
	}

	@Override
	protected void applyEntityGraphQueryHint(String hintName, RootGraphImplementor entityGraph) {
		GraphSemantic graphSemantic = GraphSemantic.fromJpaHintName( hintName );
		this.applyGraph( entityGraph, graphSemantic );
	}

	@Override
	public Map<String, Object> getHints() {
		return hints;
	}

	@Override
	public ParameterMetadataImplementor getParameterMetadata() {
		throw parametersNoSupported();
	}

	@Override
	public QueryParameterBindings getParameterBindings() {
		// parameters not supported in Hibernate Search queries
		return QueryParameterBindings.NO_PARAM_BINDINGS;
	}

	@Override
	protected QueryParameterBindings getQueryParameterBindings() {
		// parameters not supported in Hibernate Search queries
		return QueryParameterBindings.NO_PARAM_BINDINGS;
	}

	@Override
	protected QueryParameterBinding locateBinding(String name) {
		throw parametersNoSupported();
	}

	@Override
	protected QueryParameterBinding locateBinding(int position) {
		throw parametersNoSupported();
	}

	@Override
	protected QueryParameterBinding locateBinding(Parameter parameter) {
		throw parametersNoSupported();
	}

	@Override
	protected QueryParameterBinding locateBinding(QueryParameterImplementor parameter) {
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
		return (FullTextQueryImpl) super.setFetchSize( fetchSize );
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
		else if ( type.isInstance( this ) ) {
			return this;
		}
		else {
			throw new PersistenceException( "Unrecognized unwrap type [" + type.getName() + "]" );
		}
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
	public int executeUpdate() throws HibernateException {
		return doExecuteUpdate();
	}

	@Override
	protected int doExecuteUpdate() {
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
	public QueryOptionsImpl getQueryOptions() {
		return queryOptions;
	}

	@Override
	public SharedSessionContractImplementor getSession() {
		return sessionImplementor;
	}

	@Override
	public String toString() {
		return "FullTextQueryImpl(" + getQueryString() + ")";
	}

	private static final class Search5ScrollHitExtractor
			implements Function<Object, Object[]> {

		private static final Search5ScrollHitExtractor INSTANCE = new Search5ScrollHitExtractor();

		@Override
		public Object[] apply(Object hit) {
			if ( hit instanceof Object[] ) {
				return (Object[]) hit;
			}
			else {
				return new Object[] { hit };
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

	private static RootGraphImplementor<?> hintValueToEntityGraph(Object value) {
		return (RootGraphImplementor) value;
	}
}
