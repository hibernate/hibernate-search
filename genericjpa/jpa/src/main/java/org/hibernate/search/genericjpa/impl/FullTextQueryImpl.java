/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.impl;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Parameter;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Sort;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.filter.FullTextFilter;
import org.hibernate.search.genericjpa.entity.EntityProvider;
import org.hibernate.search.genericjpa.query.HSearchQuery;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.query.engine.QueryTimeoutException;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.spatial.Coordinates;

/**
 * Implements JPA 2 query interface and delegate the call to a Hibernate Core FullTextQuery. This has the consequence of
 * "duplicating" the JPA 2 query logic in some areas.
 *
 * @author Martin Braun
 * @author Emmanuel Bernard
 */
final class FullTextQueryImpl implements FullTextQuery {

	private final HSearchQuery hsearchQuery;
	// initialized at 0 since we don't expect to use hints at this stage
	private final Map<String, Object> hints = new HashMap<>( 0 );
	private EntityProvider entityProvider;
	private Integer firstResult;
	private Integer maxResults;
	private String[] projection;
	private DatabaseRetrievalMethod databaseRetrievalMethod;
	private FlushModeType jpaFlushMode = FlushModeType.AUTO;

	public FullTextQueryImpl(HSearchQuery hsearchQuery, EntityProvider entityProvider) {
		this.hsearchQuery = hsearchQuery;
		this.entityProvider = entityProvider;
	}

	@Override
	public FullTextQuery setSort(Sort sort) {
		this.hsearchQuery.sort( sort );
		return this;
	}

	@Override
	public FullTextQuery setFilter(Filter filter) {
		this.hsearchQuery.filter( filter );
		return this;
	}

	@Override
	public int getResultSize() {
		try {
			return this.hsearchQuery.queryResultSize();
		}
		catch (QueryTimeoutException e) {
			throwQueryTimeoutException( e );
		}
		return 0;
	}

	private void throwQueryTimeoutException(QueryTimeoutException e) {
		throw new javax.persistence.QueryTimeoutException( e.getMessage(), e, this );
	}

	@Override
	public FullTextQuery setProjection(String... fields) {
		this.projection = fields;
		return this;
	}

	@Override
	public FullTextQuery setSpatialParameters(double latitude, double longitude, String fieldName) {
		this.hsearchQuery.setSpatialParameters( latitude, longitude, fieldName );
		return this;
	}

	@Override
	public FullTextQuery setSpatialParameters(Coordinates center, String fieldName) {
		this.hsearchQuery.setSpatialParameters( center, fieldName );
		return this;
	}

	@Override
	public FullTextFilter enableFullTextFilter(String name) {
		return this.hsearchQuery.enableFullTextFilter( name );
	}

	@Override
	public void disableFullTextFilter(String name) {
		this.hsearchQuery.disableFullTextFilter( name );
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List getResultList() {
		try {
			if ( this.projection == null ) {
				return this.hsearchQuery.hints( this.hints ).query( this.entityProvider, this.getFetch() );
			}
			else {
				//passing hints is not really needed here, but it does no harm
				return this.hsearchQuery.hints( this.hints ).queryProjection( this.projection );
			}
		}
		catch (QueryTimeoutException e) {
			throwQueryTimeoutException( e );
			return null; // never happens
		}
		catch (SearchException he) {
			throwPersistenceException( he );
			throw he;
		}
	}

	@Override
	public FacetManager getFacetManager() {
		return this.hsearchQuery.getFacetManager();
	}

	@Override
	public String toString() {
		return this.hsearchQuery.toString();
	}

	private void throwPersistenceException(Exception e) {
		throwPersistenceException( new PersistenceException( e ) );
	}

	void throwPersistenceException(PersistenceException e) {
		if ( !(e instanceof NoResultException || e instanceof NonUniqueResultException) ) {
			// FIXME rollback
		}
		throw e;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public Object getSingleResult() {
		try {
			@SuppressWarnings("rawtypes")
			List result;
			if ( this.projection == null ) {
				result = this.hsearchQuery.query( this.entityProvider, this.getFetch() );
			}
			else {
				result = this.hsearchQuery.queryProjection( this.projection );
			}
			if ( result.size() == 0 ) {
				throwPersistenceException( new NoResultException( "No entity found for query" ) );
			}
			else if ( result.size() > 1 ) {
				@SuppressWarnings("rawtypes")
				Set uniqueResult = new HashSet( result );
				if ( uniqueResult.size() > 1 ) {
					throwPersistenceException( new NonUniqueResultException( "result returns " + uniqueResult.size() + " elements" ) );
				}
				else {
					return uniqueResult.iterator().next();
				}
			}
			else {
				return result.get( 0 );
			}
			return null; // should never happen
		}
		catch (QueryTimeoutException e) {
			throwQueryTimeoutException( e );
			return null; // never happens
		}
	}

	private HSearchQuery.Fetch getFetch() {
		return this.databaseRetrievalMethod == DatabaseRetrievalMethod.FIND_BY_ID ?
				HSearchQuery.Fetch.FIND_BY_ID :
				HSearchQuery.Fetch.BATCH;
	}

	@Override
	public Query setMaxResults(int maxResults) {
		if ( maxResults < 0 ) {
			throw new IllegalArgumentException( "Negative (" + maxResults + ") parameter passed in to setMaxResults" );
		}
		this.hsearchQuery.maxResults( maxResults );
		this.maxResults = maxResults;
		return this;
	}

	@Override
	public int getMaxResults() {
		return this.maxResults == null || this.maxResults == -1 ? Integer.MAX_VALUE : this.maxResults;
	}

	@Override
	public Query setFirstResult(int firstResult) {
		if ( firstResult < 0 ) {
			throw new IllegalArgumentException( "Negative (" + firstResult + ") parameter passed in to setFirstResult" );
		}
		this.hsearchQuery.firstResult( firstResult );
		this.firstResult = firstResult;
		return this;
	}

	@Override
	public int getFirstResult() {
		return this.firstResult == null ? 0 : this.firstResult;
	}

	@Override
	public Explanation explain(int documentId) {
		return this.hsearchQuery.explain( documentId );
	}

	@Override
	public FullTextQuery limitExecutionTimeTo(long timeout, TimeUnit timeUnit) {
		this.hsearchQuery.limitExecutionTimeTo( timeout, timeUnit );
		return this;
	}

	@Override
	public boolean hasPartialResults() {
		return this.hsearchQuery.hasPartialResults();
	}

	@Override
	public FullTextQuery initializeObjectsWith(
			ObjectLookupMethod lookupMethod,
			DatabaseRetrievalMethod retrievalMethod) {
		this.databaseRetrievalMethod = retrievalMethod;
		return this;
	}

	@Override
	public int executeUpdate() {
		throw new IllegalStateException( "Update not allowed in FullTextQueries" );
	}

	@Override
	public Query setHint(String hintName, Object value) {
		hints.put( hintName, value );
		if ( "javax.persistence.query.timeout".equals( hintName ) ) {
			if ( value == null ) {
				// nothing
			}
			// FIXME: this doesn't limit the execution time on the JPA fetches.
			else if ( value instanceof String ) {
				this.hsearchQuery.setTimeout( Long.parseLong( (String) value ), TimeUnit.MILLISECONDS );
			}
			else if ( value instanceof Number ) {
				this.hsearchQuery.setTimeout( ((Number) value).longValue(), TimeUnit.MILLISECONDS );
			}
		}
		return this;
	}

	@Override
	public Map<String, Object> getHints() {
		return this.hints;
	}

	@Override
	public <T> Query setParameter(Parameter<T> tParameter, T t) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public Query setParameter(Parameter<Calendar> calendarParameter, Calendar calendar, TemporalType temporalType) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public Query setParameter(Parameter<Date> dateParameter, Date date, TemporalType temporalType) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public Query setParameter(String name, Object value) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public Query setParameter(String name, Date value, TemporalType temporalType) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public Query setParameter(String name, Calendar value, TemporalType temporalType) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public Query setParameter(int position, Object value) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public Query setParameter(int position, Date value, TemporalType temporalType) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<Parameter<?>> getParameters() {
		return Collections.EMPTY_SET;
	}

	@Override
	public Query setParameter(int position, Calendar value, TemporalType temporalType) {
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

	@Override
	public <T> Parameter<T> getParameter(String name, Class<T> type) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries" );
	}

	@Override
	public <T> Parameter<T> getParameter(int position, Class<T> type) {
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
	public Query setFlushMode(FlushModeType flushMode) {
		this.jpaFlushMode = flushMode;
		return this;
	}

	@Override
	public FlushModeType getFlushMode() {
		return this.jpaFlushMode;
	}

	@Override
	public Query setLockMode(LockModeType lockModeType) {
		throw new UnsupportedOperationException( "lock modes not supported in fullText queries" );
	}

	@Override
	public LockModeType getLockMode() {
		throw new UnsupportedOperationException( "lock modes not supported in fullText queries" );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T unwrap(Class<T> type) {
		if ( FullTextQuery.class.equals( type ) || FullTextQueryImpl.class.equals( type ) ) {
			return (T) this;
		}
		throw new IllegalArgumentException( "cannot unwrap to: " + type );
	}

	@Override
	public FullTextQuery entityProvider(EntityProvider entityProvider) {
		this.entityProvider = entityProvider;
		return this;
	}

	@Override
	public <T> List<T> queryDto(Class<T> returnedType) {
		return this.hsearchQuery.queryDto( returnedType );
	}

	@Override
	public <T> List<T> queryDto(Class<T> returnedType, String profileName) {
		return this.hsearchQuery.queryDto( returnedType, profileName );
	}

}
