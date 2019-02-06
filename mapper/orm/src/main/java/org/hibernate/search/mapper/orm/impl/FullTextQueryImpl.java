/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.impl;

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
import org.hibernate.search.mapper.orm.hibernate.FullTextQuery;
import org.hibernate.search.mapper.orm.search.loading.impl.MutableObjectLoadingOptions;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;

public class FullTextQueryImpl<R> extends AbstractProducedQuery<R> implements FullTextQuery<R> {

	private final SearchQuery<R> searchQuery;

	private final MutableObjectLoadingOptions loadingOptions;

	private Integer firstResult;
	private Integer maxResults;

	public FullTextQueryImpl(SearchQuery<R> searchQuery, SessionImplementor sessionImplementor,
			MutableObjectLoadingOptions loadingOptions) {
		super( sessionImplementor, new ParameterMetadataImpl( null, null ) );
		this.searchQuery = searchQuery;
		this.loadingOptions = loadingOptions;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> type) {
		if ( type.equals( org.hibernate.search.mapper.orm.jpa.FullTextQuery.class ) ) {
			return (T) this;
		}
		else if ( type.equals( FullTextQuery.class ) ) {
			return (T) this;
		}
		else if ( type.equals( SearchQuery.class ) ) {
			return (T) searchQuery;
		}
		else {
			return super.unwrap( type );
		}
	}

	@Override
	public List<R> getResultList() {
		return list();
	}

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
	public List<R> list() {
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

	protected List<R> doHibernateSearchList() {
		// TODO handle timeouts
		final List<R> results = searchQuery.execute().getHits();
		// TODO apply the result transformer?
		return results;
	}

	@Override
	public long getResultSize() {
		try {
			return searchQuery.executeCount();
		}
		catch (QueryExecutionRequestException e) {
			throw new IllegalStateException( e );
		}
		catch (TypeMismatchException e) {
			throw new IllegalArgumentException( e );
		}
		catch (HibernateException he) {
			throw getExceptionConverter().convert( he );
		}
	}

	@Override
	public FullTextQueryImpl<R> setMaxResults(int maxResults) {
		if ( maxResults < 0 ) {
			throw new IllegalArgumentException(
					"Negative ("
							+ maxResults
							+ ") parameter passed in to setMaxResults"
			);
		}
		searchQuery.setMaxResults( (long) maxResults );
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
	public FullTextQueryImpl<R> setFirstResult(int firstResult) {
		if ( firstResult < 0 ) {
			throw new IllegalArgumentException(
					"Negative ("
							+ firstResult
							+ ") parameter passed in to setFirstResult"
			);
		}
		searchQuery.setFirstResult( (long) firstResult );
		this.firstResult = firstResult;
		return this;
	}

	@Override
	public int getFirstResult() {
		return firstResult == null ? 0 : firstResult;
	}

	@Override
	public FullTextQueryImpl<R> setHint(String hintName, Object value) {
		// TODO hints (javax.persistence.query.timeout hint in particular)
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public Map<String, Object> getHints() {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public <P> FullTextQueryImpl<R> setParameter(Parameter<P> tParameter, P t) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public FullTextQueryImpl<R> setParameter(Parameter<Calendar> calendarParameter, Calendar calendar, TemporalType temporalType) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public FullTextQueryImpl<R> setParameter(Parameter<Date> dateParameter, Date date, TemporalType temporalType) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public FullTextQueryImpl<R> setParameter(String name, Object value) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public FullTextQueryImpl<R> setParameter(String name, Date value, TemporalType temporalType) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public FullTextQueryImpl<R> setParameter(String name, Calendar value, TemporalType temporalType) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public FullTextQueryImpl<R> setParameter(int position, Object value) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public FullTextQueryImpl<R> setParameter(int position, Date value, TemporalType temporalType) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<Parameter<?>> getParameters() {
		return Collections.emptySet();
	}

	@Override
	protected QueryParameterBindings getQueryParameterBindings() {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public FullTextQueryImpl<R> setParameter(int position, Calendar value, TemporalType temporalType) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public QueryParameter<?> getParameter(String name) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public QueryParameter<?> getParameter(int position) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public <T> QueryParameter<T> getParameter(String name, Class<T> type) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public <T> QueryParameter<T> getParameter(int position, Class<T> type) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public boolean isBound(Parameter<?> param) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public <T> T getParameterValue(Parameter<T> param) {
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
	public FullTextQueryImpl<R> setFlushMode(FlushModeType flushModeType) {
		super.setFlushMode( flushModeType );
		return this;
	}

	@Override
	public FullTextQueryImpl<R> setFetchSize(int fetchSize) {
		super.setFetchSize( fetchSize );
		loadingOptions.setFetchSize( fetchSize );
		return this;
	}

	@Override
	public FullTextQueryImpl<R> setLockOptions(LockOptions lockOptions) {
		throw new UnsupportedOperationException( "Lock options are not implemented in Hibernate Search queries" );
	}

	@Deprecated
	@Override
	public FullTextQueryImpl<R> setResultTransformer(ResultTransformer transformer) {
		super.setResultTransformer( transformer );
		throw resultTransformerNotImplemented();
	}

	private UnsupportedOperationException resultTransformerNotImplemented() {
		// TODO result transformer
		return new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public FullTextQueryImpl<R> setLockMode(LockModeType lockModeType) {
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
	public FullTextQueryImpl<R> setLockMode(String alias, LockMode lockMode) {
		throw new UnsupportedOperationException( "Lock options are not implemented in Hibernate Search queries" );
	}

	@Override
	public FullTextQueryImpl<R> setTimeout(int timeout) {
		throw timeoutNotImplementedYet();
	}

	private UnsupportedOperationException timeoutNotImplementedYet() {
		// TODO add support for timeouts
		return new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public String getQueryString() {
		return searchQuery.getQueryString();
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
	public FullTextQueryImpl<R> setEntity(int position, Object val) {
		throw new UnsupportedOperationException( "setEntity(int,Object) is not implemented in Hibernate Search queries" );
	}

	@Deprecated
	@Override
	public FullTextQueryImpl<R> setEntity(String name, Object val) {
		throw new UnsupportedOperationException( "setEntity(String,Object) is not implemented in Hibernate Search queries" );
	}

	@Override
	public String toString() {
		return "FullTextQueryImpl(" + getQueryString() + ")";
	}
}
