/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.query.impl;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.TemporalType;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.TypeMismatchException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.hql.internal.QueryExecutionRequestException;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.internal.AbstractProducedQuery;
import org.hibernate.query.internal.ParameterMetadataImpl;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.search.loading.impl.MutableObjectLoadingOptions;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;

public final class HibernateOrmSearchQueryAdapter<R> extends AbstractProducedQuery<R> {

	public static <R> HibernateOrmSearchQueryAdapter<R> create(SearchQuery<R> query) {
		return query.extension( HibernateOrmSearchQueryAdapterExtension.get() );
	}

	private final SearchQuery<R> delegate;
	private final MutableObjectLoadingOptions loadingOptions;

	private Integer firstResult;
	private Integer maxResults;

	HibernateOrmSearchQueryAdapter(SearchQuery<R> delegate, SessionImplementor sessionImplementor,
			MutableObjectLoadingOptions loadingOptions) {
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
		loadingOptions.setFetchSize( fetchSize );
		return this;
	}

	@Override
	public HibernateOrmSearchQueryAdapter<R> setFlushMode(FlushModeType flushModeType) {
		super.setFlushMode( flushModeType );
		return this;
	}

	@Override
	public String getQueryString() {
		return delegate.getQueryString();
	}

	@Override
	protected boolean isNativeQuery() {
		return false;
	}

	@Override
	protected List<R> doList() {
		// TODO HSEARCH-3352 handle timeouts
		// TODO HSEARCH-3093 apply the result transformer?
		return delegate.fetchHits( maxResults, firstResult );
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
		throw resultStreamingNotImplemented();
	}

	@Override
	public ScrollableResultsImplementor scroll() {
		throw resultStreamingNotImplemented();
	}

	@Override
	public ScrollableResultsImplementor scroll(ScrollMode scrollMode) {
		throw resultStreamingNotImplemented();
	}

	private UnsupportedOperationException resultStreamingNotImplemented() {
		// TODO result streaming
		return new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public HibernateOrmSearchQueryAdapter<R> setHint(String hintName, Object value) {
		// TODO hints (javax.persistence.query.timeout hint in particular)
		throw new UnsupportedOperationException( "Not implemented yet" );
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
	public HibernateOrmSearchQueryAdapter<R> setParameter(Parameter<Calendar> calendarParameter, Calendar calendar, TemporalType temporalType) {
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
		return new UnsupportedOperationException( "parameters not supported in Hibernate Search queries" );
	}

	@Override
	public HibernateOrmSearchQueryAdapter<R> setLockOptions(LockOptions lockOptions) {
		throw new UnsupportedOperationException( "Lock options are not implemented in Hibernate Search queries" );
	}

	@Deprecated
	@Override
	public HibernateOrmSearchQueryAdapter<R> setResultTransformer(ResultTransformer transformer) {
		super.setResultTransformer( transformer );
		throw resultTransformerNotImplemented();
	}

	private UnsupportedOperationException resultTransformerNotImplemented() {
		// TODO result transformer
		return new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public HibernateOrmSearchQueryAdapter<R> setLockMode(LockModeType lockModeType) {
		throw new UnsupportedOperationException( "lock modes not supported in Hibernate Search queries" );
	}

	@Override
	public LockModeType getLockMode() {
		throw new UnsupportedOperationException( "lock modes not supported in Hibernate Search queries" );
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
	public HibernateOrmSearchQueryAdapter<R> setLockMode(String alias, LockMode lockMode) {
		throw new UnsupportedOperationException( "Lock options are not implemented in Hibernate Search queries" );
	}

	@Override
	public HibernateOrmSearchQueryAdapter<R> setTimeout(int timeout) {
		throw timeoutNotImplementedYet();
	}

	private UnsupportedOperationException timeoutNotImplementedYet() {
		// TODO add support for timeouts
		return new UnsupportedOperationException( "Not implemented yet" );
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
}
