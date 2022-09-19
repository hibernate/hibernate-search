/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.query.impl;

import java.lang.invoke.MethodHandles;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.QueryTimeoutException;
import jakarta.persistence.TemporalType;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.QueryException;
import org.hibernate.ScrollMode;
import org.hibernate.TypeMismatchException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.internal.AbstractProducedQuery;
import org.hibernate.query.internal.ParameterMetadataImpl;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.spi.SearchQueryImplementor;
import org.hibernate.search.mapper.orm.loading.impl.EntityGraphHint;
import org.hibernate.search.mapper.orm.loading.impl.MutableEntityLoadingOptions;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.search.query.spi.HibernateOrmSearchQueryHints;
import org.hibernate.search.mapper.orm.search.query.spi.HibernateOrmSearchScrollableResultsAdapter;
import org.hibernate.search.mapper.orm.search.query.spi.HibernateOrmSearchScrollableResultsAdapter.ScrollHitExtractor;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.search.util.common.annotation.impl.SuppressForbiddenApis;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;

@SuppressForbiddenApis(reason = "We need to extend the internal AbstractProducedQuery"
		+ " in order to implement a org.hibernate.query.Query")
@SuppressWarnings("unchecked") // For some reason javac issues warnings for all methods returning this; IDEA doesn't.
public final class HibernateOrmSearchQueryAdapter<R> extends AbstractProducedQuery<R> {

	public static <R> HibernateOrmSearchQueryAdapter<R> create(SearchQuery<R> query) {
		return query.extension( HibernateOrmSearchQueryAdapterExtension.get() );
	}

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SearchQueryImplementor<R> delegate;
	private final MutableEntityLoadingOptions loadingOptions;

	private Integer firstResult;
	private Integer maxResults;

	HibernateOrmSearchQueryAdapter(SearchQueryImplementor<R> delegate, SessionImplementor sessionImplementor,
			MutableEntityLoadingOptions loadingOptions) {
		super( sessionImplementor, new ParameterMetadataImpl( null, null ) );
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
	public <T> T unwrap(Class<T> type) {
		if ( type.equals( SearchQuery.class ) ) {
			return (T) delegate;
		}
		else {
			return super.unwrap( type );
		}
	}

	@Override
	public List<R> list() {
		/*
		 * Reproduce the behavior of AbstractProducedQuery.list() regarding exceptions,
		 * but without the beforeQuery/afterQuery calls.
		 * These beforeQuery/afterQuery calls make everything fail
		 * because they call methods related to parameters,
		 * which are not supported here.
		 */
		try {
			return doList();
		}
		catch (SearchTimeoutException e) {
			throw new QueryTimeoutException( e );
		}
		catch (QueryException he) {
			throw new IllegalStateException( he );
		}
		catch (TypeMismatchException e) {
			throw new IllegalArgumentException( e );
		}
		catch (HibernateException he) {
			throw getExceptionConverter().convert( he );
		}
	}

	@Override
	public HibernateOrmSearchQueryAdapter<R> setMaxResults(int maxResults) {
		if ( maxResults < 0L ) {
			throw new IllegalArgumentException(
					"Negative (" + maxResults + ") parameter passed in to setMaxResults"
			);
		}
		this.maxResults = maxResults;
		return this;
	}

	@Override
	public HibernateOrmSearchQueryAdapter<R> setFirstResult(int firstResult) {
		if ( firstResult < 0 ) {
			throw new IllegalArgumentException(
					"Negative (" + firstResult + ") parameter passed in to setFirstResult"
			);
		}
		this.firstResult = firstResult;
		return this;
	}

	@Override
	public int getMaxResults() {
		return maxResults == null ? Integer.MAX_VALUE : maxResults;
	}

	@Override
	public int getFirstResult() {
		return firstResult == null ? 0 : firstResult;
	}

	@Override
	public HibernateOrmSearchQueryAdapter<R> setFetchSize(int fetchSize) {
		loadingOptions.fetchSize( fetchSize );
		return this;
	}

	@Override
	public HibernateOrmSearchQueryAdapter<R> setFlushMode(FlushModeType flushModeType) {
		super.setFlushMode( flushModeType );
		return this;
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
	public ScrollableResultsImplementor scroll() {
		return scroll( ScrollMode.FORWARD_ONLY );
	}

	@Override
	public ScrollableResultsImplementor scroll(ScrollMode scrollMode) {
		if ( !ScrollMode.FORWARD_ONLY.equals( scrollMode ) ) {
			throw log.canOnlyUseScrollWithScrollModeForwardsOnly( scrollMode );
		}

		int chunkSize = loadingOptions.fetchSize();
		return new HibernateOrmSearchScrollableResultsAdapter<>( delegate.scroll( chunkSize ), getMaxResults(),
				ScrollHitExtractor.singleObject() );
	}

	@Override
	protected boolean isNativeQuery() {
		return false;
	}

	@Override
	protected List<R> doList() {
		return delegate.fetchHits( firstResult, maxResults );
	}

	//-------------------------------------------------------------
	// Unsupported ORM/JPA query methods
	//-------------------------------------------------------------

	/**
	 * Return an iterator on the results.
	 * Retrieve the object one by one (initialize it during the next() operation)
	 */
	@Override
	public Iterator<R> iterate() {
		throw new UnsupportedOperationException(
				"iterate() is not implemented in Hibernate Search queries. Use scroll() instead." );
	}

	@Override
	public Map<String, Object> getHints() {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public <P> HibernateOrmSearchQueryAdapter<R> setParameter(Parameter<P> tParameter, P t) {
		throw parametersNoSupported();
	}

	@Override
	public HibernateOrmSearchQueryAdapter<R> setParameter(Parameter<Calendar> calendarParameter, Calendar calendar,
			TemporalType temporalType) {
		throw parametersNoSupported();
	}

	@Override
	public HibernateOrmSearchQueryAdapter<R> setParameter(Parameter<Date> dateParameter, Date date, TemporalType temporalType) {
		throw parametersNoSupported();
	}

	@Override
	public HibernateOrmSearchQueryAdapter<R> setParameter(String name, Object value) {
		throw parametersNoSupported();
	}

	@Override
	public HibernateOrmSearchQueryAdapter<R> setParameter(String name, Date value, TemporalType temporalType) {
		throw parametersNoSupported();
	}

	@Override
	public HibernateOrmSearchQueryAdapter<R> setParameter(String name, Calendar value, TemporalType temporalType) {
		throw parametersNoSupported();
	}

	@Override
	public HibernateOrmSearchQueryAdapter<R> setParameter(int position, Object value) {
		throw parametersNoSupported();
	}

	@Override
	public HibernateOrmSearchQueryAdapter<R> setParameter(int position, Date value, TemporalType temporalType) {
		throw parametersNoSupported();
	}

	@Override
	public Set<Parameter<?>> getParameters() {
		return Collections.emptySet();
	}

	@Override
	protected QueryParameterBindings getQueryParameterBindings() {
		throw parametersNoSupported();
	}

	@Override
	public HibernateOrmSearchQueryAdapter<R> setParameter(int position, Calendar value, TemporalType temporalType) {
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

	@Override
	public <T> QueryParameter<T> getParameter(String name, Class<T> type) {
		throw parametersNoSupported();
	}

	@Override
	public <T> QueryParameter<T> getParameter(int position, Class<T> type) {
		throw parametersNoSupported();
	}

	@Override
	public boolean isBound(Parameter<?> param) {
		throw parametersNoSupported();
	}

	@Override
	public <T> T getParameterValue(Parameter<T> param) {
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
	public HibernateOrmSearchQueryAdapter<R> setLockOptions(LockOptions lockOptions) {
		throw lockOptionsNotSupported();
	}

	@Deprecated
	@Override
	public HibernateOrmSearchQueryAdapter<R> setResultTransformer(ResultTransformer transformer) {
		super.setResultTransformer( transformer );
		throw resultTransformerNotImplemented();
	}

	private UnsupportedOperationException resultTransformerNotImplemented() {
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
	public int executeUpdate() {
		throw new UnsupportedOperationException( "executeUpdate is not supported in Hibernate Search queries" );
	}

	@Override
	public HibernateOrmSearchQueryAdapter<R> setLockMode(String alias, LockMode lockMode) {
		throw lockOptionsNotSupported();
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
	public HibernateOrmSearchQueryAdapter<R> setEntity(int position, Object val) {
		throw new UnsupportedOperationException( "setEntity(int,Object) is not implemented in Hibernate Search queries" );
	}

	@Deprecated
	@Override
	public HibernateOrmSearchQueryAdapter<R> setEntity(String name, Object val) {
		throw new UnsupportedOperationException( "setEntity(String,Object) is not implemented in Hibernate Search queries" );
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

	private static RootGraph<?> hintValueToEntityGraph(Object value) {
		return (RootGraph<?>) value;
	}

}
