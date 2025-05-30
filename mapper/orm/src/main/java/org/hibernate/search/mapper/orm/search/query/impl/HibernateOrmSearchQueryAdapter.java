/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.search.query.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.QueryTimeoutException;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.query.KeyedPage;
import org.hibernate.query.KeyedResultList;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.spi.AbstractQuery;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.spi.SearchQueryImplementor;
import org.hibernate.search.mapper.orm.loading.spi.EntityGraphHint;
import org.hibernate.search.mapper.orm.loading.spi.MutableEntityLoadingOptions;
import org.hibernate.search.mapper.orm.logging.impl.OrmMiscLog;
import org.hibernate.search.mapper.orm.search.query.spi.HibernateOrmSearchQueryHints;
import org.hibernate.search.mapper.orm.search.query.spi.HibernateOrmSearchScrollableResultsAdapter;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.search.util.common.annotation.impl.SuppressForbiddenApis;

@SuppressForbiddenApis(reason = "We need to use the internal QueryOptionsImpl"
		+ " in order to implement a org.hibernate.query.Query")
@SuppressWarnings("unchecked") // For some reason javac issues warnings for all methods returning this; IDEA doesn't.
public final class HibernateOrmSearchQueryAdapter<R> extends AbstractQuery<R> {

	public static <R> HibernateOrmSearchQueryAdapter<R> create(SearchQuery<R> query) {
		return query.extension( HibernateOrmSearchQueryAdapterExtension.get() );
	}

	private final SearchQueryImplementor<R> delegate;

	private final MutableEntityLoadingOptions loadingOptions;

	HibernateOrmSearchQueryAdapter(SearchQueryImplementor<R> delegate, SessionImplementor sessionImplementor,
			MutableEntityLoadingOptions loadingOptions) {
		super( sessionImplementor );
		this.delegate = delegate;
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
				applyEntityGraphHint( hintName, value );
				break;
			default:
				OrmMiscLog.INSTANCE.ignoringUnrecognizedQueryHint( hintName );
				break;
		}
		return this;
	}

	@Override
	public HibernateOrmSearchQueryAdapter<R> setTimeout(int timeout) {
		delegate.failAfter( Long.valueOf( timeout ), TimeUnit.SECONDS );
		return this;
	}

	@Override
	public HibernateOrmSearchQueryAdapter<R> setTimeout(Integer timeout) {
		// TODO: this method comes from JPA and there timeout is in ms ?
		//  https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2-m2 ctrl+f "Set the query timeout, in milliseconds"
		// 	but the one above is in sec from ORM itself (CommonQueryContract)
		delegate.failAfter( timeout == null ? null : timeout.longValue(), TimeUnit.SECONDS );
		return this;
	}

	@Override
	@SuppressWarnings("rawtypes")
	@Deprecated(since = "8.0")
	public HibernateOrmSearchQueryAdapter<R> applyGraph(RootGraph graph, GraphSemantic semantic) {
		applyGraph( (RootGraphImplementor) graph, semantic );
		return this;
	}

	@Override
	public ScrollableResultsImplementor<R> scroll() {
		return scroll( ScrollMode.FORWARD_ONLY );
	}

	@Override
	public long getResultCount() {
		return delegate.fetchTotalHitCount();
	}

	@Override
	public KeyedResultList<R> getKeyedResultList(KeyedPage<R> page) {
		throw keyedResultListNoSupported();
	}

	private UnsupportedOperationException keyedResultListNoSupported() {
		return new UnsupportedOperationException( "Keyed result lists are not supported in Hibernate Search queries" );
	}

	@Override
	protected ScrollableResultsImplementor<R> doScroll(ScrollMode scrollMode) {
		if ( !ScrollMode.FORWARD_ONLY.equals( scrollMode ) ) {
			throw OrmMiscLog.INSTANCE.canOnlyUseScrollWithScrollModeForwardsOnly( scrollMode );
		}

		int chunkSize = loadingOptions.fetchSize();
		return new HibernateOrmSearchScrollableResultsAdapter<>( delegate.scroll( chunkSize ), getMaxResults(),
				Function.identity() );
	}

	@Override
	protected List<R> doList() {
		// Do not use getMaxRows()/getFirstRow() directly, they return weird values to comply with the JPA spec
		Integer limit = getQueryOptions().getLimit().getMaxRows();
		Integer offset = getQueryOptions().getLimit().getFirstRow();
		return delegate.fetchHits( offset, limit );
	}

	@Override
	protected void beforeQuery() {
		super.beforeQuery();

		extractQueryOptions();
	}

	private void extractQueryOptions() {
		MutableQueryOptions queryOptions = getQueryOptions();
		Integer queryFetchSize = queryOptions.getFetchSize();
		if ( queryFetchSize != null ) {
			loadingOptions.fetchSize( queryFetchSize );
		}
		Integer queryTimeout = queryOptions.getTimeout();
		if ( queryTimeout != null ) {
			delegate.failAfter( Long.valueOf( queryTimeout ), TimeUnit.SECONDS );
		}
		EntityGraphHint<?> entityGraphHint = null;
		if ( isGraphApplied( queryOptions ) ) {
			AppliedGraph appliedGraph = queryOptions.getAppliedGraph();
			RootGraph<?> graph = appliedGraph.getGraph();
			if ( graph != null ) {
				entityGraphHint = new EntityGraphHint<>( graph, appliedGraph.getSemantic() );
			}
		}
		loadingOptions.entityGraphHint( entityGraphHint, true );
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
		return QueryParameterBindings.empty();
	}

	@Override
	public QueryParameterBindings getQueryParameterBindings() {
		// parameters not supported in Hibernate Search queries
		return QueryParameterBindings.empty();
	}

	@Override
	protected <P> QueryParameterBinding<P> locateBinding(String name) {
		throw parametersNoSupported();
	}

	@Override
	protected <P> QueryParameterBinding<P> locateBinding(int position) {
		throw parametersNoSupported();
	}

	@Override
	protected <P> QueryParameterBinding<P> locateBinding(Parameter<P> parameter) {
		throw parametersNoSupported();
	}

	@Override
	protected <P> QueryParameterBinding<P> locateBinding(QueryParameterImplementor<P> parameter) {
		throw parametersNoSupported();
	}

	private UnsupportedOperationException parametersNoSupported() {
		return new UnsupportedOperationException( "Parameters are not supported in Hibernate Search queries" );
	}

	@Override
	public <T> QueryImplementor<T> setTupleTransformer(TupleTransformer<T> transformer) {
		throw resultOrTupleTransformerNotImplemented();
	}

	@Override
	public QueryImplementor<R> setResultListTransformer(ResultListTransformer<R> resultListTransformer) {
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

	@Deprecated(since = "8.0")
	@Override
	@SuppressWarnings("removal")
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
		throw new UnsupportedOperationException( "executeUpdate() is not supported in Hibernate Search queries" );
	}

	@Override
	public HibernateOrmSearchQueryAdapter<R> setLockMode(String alias, LockMode lockMode) {
		throw lockOptionsNotSupported();
	}

	private static Long hintValueToLong(Object value) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof Number ) {
			return ( (Number) value ).longValue();
		}
		else {
			return Long.parseLong( String.valueOf( value ) );
		}
	}

	private static Integer hintValueToInteger(Object value) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof Number ) {
			return ( (Number) value ).intValue();
		}
		else {
			return Integer.parseInt( String.valueOf( value ) );
		}
	}

	private static boolean isGraphApplied(MutableQueryOptions queryOptions) {
		final AppliedGraph appliedGraph = queryOptions.getAppliedGraph();
		return appliedGraph != null && appliedGraph.getSemantic() != null;
	}
}
