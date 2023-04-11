/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.query.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.spi.AbstractQuery;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.spi.SearchQueryImplementor;
import org.hibernate.search.mapper.orm.loading.impl.EntityGraphHint;
import org.hibernate.search.mapper.orm.loading.impl.MutableEntityLoadingOptions;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.search.query.spi.HibernateOrmSearchQueryHints;
import org.hibernate.search.mapper.orm.search.query.spi.HibernateOrmSearchScrollableResultsAdapter;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.search.util.common.annotation.impl.SuppressForbiddenApis;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.QueryTimeoutException;

@SuppressForbiddenApis(reason = "We need to extend the internal AbstractProducedQuery"
		+ " in order to implement a org.hibernate.query.Query")
@SuppressWarnings("unchecked") // For some reason javac issues warnings for all methods returning this; IDEA doesn't.
public final class HibernateOrmSearchQueryAdapter<R> extends AbstractQuery<R> {

	public static <R> HibernateOrmSearchQueryAdapter<R> create(SearchQuery<R> query) {
		return query.extension( HibernateOrmSearchQueryAdapterExtension.get() );
	}

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SearchQueryImplementor<R> delegate;

	private final SessionImplementor sessionImplementor;
	private final MutableEntityLoadingOptions loadingOptions;
	private final QueryOptionsImpl queryOptions = new QueryOptionsImpl();

	HibernateOrmSearchQueryAdapter(SearchQueryImplementor<R> delegate, SessionImplementor sessionImplementor,
			MutableEntityLoadingOptions loadingOptions) {
		super( sessionImplementor );
		this.delegate = delegate;
		this.sessionImplementor = sessionImplementor;
		this.loadingOptions = loadingOptions;
	}

	@Override
	public String toString() {
		return "HibernateOrmSearchQueryAdapter(" + getQueryString() + ")";
	}

	//-------------------------------------------------------------
	// Supported ORM/JPA query methods
	//-------------------------------------------------------------

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> cls) {
		if ( cls.equals( SearchQuery.class ) ) {
			return (T) delegate;
		}
		else if ( cls.isInstance( this ) ) {
			return (T) this;
		}
		else {
			throw new PersistenceException( "Unrecognized unwrap type [" + cls.getName() + "]" );
		}
	}

	@Override
	public List<R> list() {
		try {
			return super.list();
		}
		catch (SearchTimeoutException e) {
			throw new QueryTimeoutException( e );
		}
	}

	@Override
	public String getQueryString() {
		return delegate.queryString();
	}

	@Override
	public QueryOptionsImpl getQueryOptions() {
		return queryOptions;
	}

	@Override
	public HibernateOrmSearchQueryAdapter<R> setHint(String hintName, Object value) {
		switch ( hintName ) {
			case HibernateOrmSearchQueryHints.JAVAX_TIMEOUT:
			case HibernateOrmSearchQueryHints.JAKARTA_TIMEOUT:
				delegate.failAfter( hintValueToLong( value ), TimeUnit.MILLISECONDS );
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
				log.ignoringUnrecognizedQueryHint( hintName );
				break;
		}
		return this;
	}

	@Override
	public HibernateOrmSearchQueryAdapter<R> setTimeout(int timeout) {
		delegate.failAfter( timeout, TimeUnit.SECONDS );
		return this;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public HibernateOrmSearchQueryAdapter<R> applyGraph(RootGraph graph, GraphSemantic semantic) {
		loadingOptions.entityGraphHint( new EntityGraphHint<>( graph, semantic ), true );
		return this;
	}

	@Override
	protected void applyEntityGraphQueryHint(String hintName, RootGraphImplementor<?> entityGraph) {
		GraphSemantic graphSemantic = GraphSemantic.fromJpaHintName( hintName );
		this.applyGraph( entityGraph, graphSemantic );
	}

	@Override
	public ScrollableResultsImplementor scroll() {
		return scroll( ScrollMode.FORWARD_ONLY );
	}

	@Override
	public ScrollableResultsImplementor scroll(ScrollMode scrollMode) {
		if ( !ScrollMode.FORWARD_ONLY.equals( scrollMode ) ) {
			throw log.canOnlyUseScrollWithScrollModeForwardsOnly( scrollMode );
		}

		extractQueryOptions();

		int chunkSize = loadingOptions.fetchSize();
		return new HibernateOrmSearchScrollableResultsAdapter<>( delegate.scroll( chunkSize ), getMaxResults(),
				Function.identity() );
	}

	@Override
	public SharedSessionContractImplementor getSession() {
		return sessionImplementor;
	}

	@Override
	protected List<R> doList() {
		// Do not use getMaxRows()/getFirstRow() directly, they return weird values to comply with the JPA spec
		Integer limit = getQueryOptions().getLimit().getMaxRows();
		Integer offset = getQueryOptions().getLimit().getFirstRow();
		return delegate.fetchHits( offset, limit );
	}

	@Override
	protected void beforeQuery(boolean requiresTxn) {
		super.beforeQuery( requiresTxn );

		extractQueryOptions();
	}

	private void extractQueryOptions() {
		Integer queryFetchSize = getQueryOptions().getFetchSize();
		if ( queryFetchSize != null ) {
			loadingOptions.fetchSize( queryFetchSize );
		}
		Integer queryTimeout = getQueryOptions().getTimeout();
		if ( queryTimeout != null ) {
			delegate.failAfter( queryTimeout, TimeUnit.SECONDS );
		}
	}

	//-------------------------------------------------------------
	// Unsupported ORM/JPA query methods
	//-------------------------------------------------------------

	@Override
	public Map<String, Object> getHints() {
		throw new UnsupportedOperationException( "Not implemented yet" );
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
	public HibernateOrmSearchQueryAdapter<R> setParameterList(String name, Object[] values) {
		throw parametersNoSupported();
	}

	@Override
	public QueryImplementor<R> setParameterList(String s, Collection collection, Class aClass) {
		throw parametersNoSupported();
	}

	@Override
	public QueryImplementor<R> setParameterList(int i, Collection collection, Class aClass) {
		throw parametersNoSupported();
	}

	private UnsupportedOperationException parametersNoSupported() {
		return new UnsupportedOperationException( "Parameters are not supported in Hibernate Search queries" );
	}

	@Override
	public QueryImplementor<R> setTupleTransformer(TupleTransformer transformer) {
		throw resultOrTupleTransformerNotImplemented();
	}

	@Override
	public QueryImplementor<R> setResultListTransformer(ResultListTransformer resultListTransformer) {
		throw resultOrTupleTransformerNotImplemented();
	}

	private UnsupportedOperationException resultOrTupleTransformerNotImplemented() {
		return new UnsupportedOperationException( "Result transformers are not supported in Hibernate Search queries" );
	}

	@Override
	public HibernateOrmSearchQueryAdapter<R> setLockMode(LockModeType lockModeType) {
		throw lockOptionsNotSupported();
	}

	@Override
	public LockModeType getLockMode() {
		throw lockOptionsNotSupported();
	}

	@Override
	public LockOptions getLockOptions() {
		/*
		 * Ideally we'd throw an UnsupportedOperationException,
		 * but we can't because getLockOptions is called
		 * when AbstractProducedQuery converts exceptions.
		 * So let's just return null, which at least seems acceptable for AbstractProducedQuery.
		 */
		return null;
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
	public HibernateOrmSearchQueryAdapter<R> setLockMode(String alias, LockMode lockMode) {
		throw lockOptionsNotSupported();
	}

	private static long hintValueToLong(Object value) {
		if ( value instanceof Number ) {
			return ( (Number) value ).longValue();
		}
		else {
			return Long.parseLong( String.valueOf( value ) );
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
		return (RootGraphImplementor<?>) value;
	}

}
