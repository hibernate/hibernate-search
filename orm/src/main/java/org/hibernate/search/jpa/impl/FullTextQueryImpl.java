/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.jpa.impl;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.LockTimeoutException;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.OptimisticLockException;
import javax.persistence.Parameter;
import javax.persistence.PersistenceException;
import javax.persistence.PessimisticLockException;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Sort;

import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.QueryException;
import org.hibernate.QueryTimeoutException;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.TransientObjectException;
import org.hibernate.TypeMismatchException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.hql.internal.QueryExecutionRequestException;
import org.hibernate.search.FullTextFilter;
import org.hibernate.search.SearchException;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.transform.ResultTransformer;

/**
 * Implements JPA 2 query interface and delegate the call to
 * a Hibernate Core FullTextQuery.
 * This has the consequence of "duplicating" the JPA 2 query logic in some areas.
 *
 * @author Emmanuel Bernard
 */
public class FullTextQueryImpl implements FullTextQuery {
	private final org.hibernate.search.FullTextQuery query;
	private final Session session;
	private Integer firstResult;
	private Integer maxResults;
	//initialized at 0 since we don't expect to use hints at this stage
	private final Map<String, Object> hints = new HashMap<String, Object>( 0 );
	private FlushModeType jpaFlushMode;

	public FullTextQueryImpl(org.hibernate.search.FullTextQuery query, Session session) {
		this.query = query;
		this.session = session;
	}

	@Override
	public FullTextQuery setSort(Sort sort) {
		query.setSort( sort );
		return this;
	}

	@Override
	public FullTextQuery setFilter(Filter filter) {
		query.setFilter( filter );
		return this;
	}

	@Override
	public int getResultSize() {
		try {
			return query.getResultSize();
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
	public FullTextQuery setCriteriaQuery(Criteria criteria) {
		query.setCriteriaQuery( criteria );
		return this;
	}

	@Override
	public FullTextQuery setProjection(String... fields) {
		query.setProjection( fields );
		return this;
	}

	@Override
	public FullTextQuery setSpatialParameters(double latitude, double longitude, String fieldName) {
		query.setSpatialParameters( latitude, longitude, fieldName );
		return this;
	}

	@Override
	public FullTextQuery setSpatialParameters(Coordinates center, String fieldName) {
		query.setSpatialParameters( center, fieldName );
		return this;
	}

	@Override
	public FullTextFilter enableFullTextFilter(String name) {
		return query.enableFullTextFilter( name );
	}

	@Override
	public void disableFullTextFilter(String name) {
		query.disableFullTextFilter( name );
	}

	@Override
	public FullTextQuery setResultTransformer(ResultTransformer transformer) {
		query.setResultTransformer( transformer );
		return this;
	}

	@Override
	public List getResultList() {
		try {
			return query.list();
		}
		catch (QueryTimeoutException e) {
			throwQueryTimeoutException( e );
			return null; //never happens
		}
		catch (QueryExecutionRequestException he) {
			//TODO when an illegal state exception should be raised?
			throw new IllegalStateException( he );
		}
		catch (TypeMismatchException e) {
			//TODO when an illegal arg exception should be raised?
			throw new IllegalArgumentException( e );
		}
		catch (SearchException he) {
			throwPersistenceException( he );
			throw he;
		}
	}

	@Override
	public FacetManager getFacetManager() {
		return query.getFacetManager();
	}

	//TODO mutualize this code with the EM this will fix the rollback issues
	@SuppressWarnings( { "ThrowableInstanceNeverThrown" })
	private void throwPersistenceException(Exception e) {
		if ( e instanceof StaleStateException ) {
			PersistenceException pe = wrapStaleStateException( (StaleStateException) e );
			throwPersistenceException( pe );
		}
		else if ( e instanceof org.hibernate.OptimisticLockException ) {
			PersistenceException converted = wrapLockException( (HibernateException) e, null );
			throwPersistenceException( converted );
		}
		else if ( e instanceof org.hibernate.PessimisticLockException ) {
			PersistenceException converted = wrapLockException( (HibernateException) e, null );
			throwPersistenceException( converted );
		}
		else if ( e instanceof ConstraintViolationException ) {
			//FIXME this is bad cause ConstraintViolationException happens in other circumstances
			throwPersistenceException( new EntityExistsException( e ) );
		}
		else if ( e instanceof org.hibernate.QueryTimeoutException ) {
			javax.persistence.QueryTimeoutException converted = new javax.persistence.QueryTimeoutException(
					e.getMessage(), e
			);
			throwPersistenceException( converted );
		}
		else if ( e instanceof ObjectNotFoundException ) {
			throwPersistenceException( new EntityNotFoundException( e.getMessage() ) );
		}
		else if ( e instanceof org.hibernate.NonUniqueResultException ) {
			throwPersistenceException( new NonUniqueResultException( e.getMessage() ) );
		}
		else if ( e instanceof UnresolvableObjectException ) {
			throwPersistenceException( new EntityNotFoundException( e.getMessage() ) );
		}
		else if ( e instanceof QueryException ) {
			throw new IllegalArgumentException( e );
		}
		else if ( e instanceof TransientObjectException ) {
			//FIXME rollback
			throw new IllegalStateException( e ); //Spec 3.2.3 Synchronization rules
		}
		else {
			throwPersistenceException( new PersistenceException( e ) );
		}
	}

	public PersistenceException wrapLockException(HibernateException e, LockOptions lockOptions) {
		if ( OptimisticLockingCompatibilityHelper.isOptimisticLockException( e ) ) {
			throw OptimisticLockingCompatibilityHelper.convertToLockException( e );
		}
		else if ( e instanceof org.hibernate.PessimisticLockException ) {
			org.hibernate.PessimisticLockException ple = (org.hibernate.PessimisticLockException) e;
			if ( lockOptions != null && lockOptions.getTimeOut() > -1 ) {
				// assume lock timeout occurred if a timeout or NO WAIT was specified
				return new LockTimeoutException( ple.getMessage(), ple );
			}
			else {
				return new PessimisticLockException( ple.getMessage(), ple );
			}
		}
		else {
			return new OptimisticLockException( e.getMessage(), e );
		}
	}

	void throwPersistenceException(PersistenceException e) {
		if ( !( e instanceof NoResultException || e instanceof NonUniqueResultException ) ) {
			//FIXME rollback
		}
		throw e;
	}

	@SuppressWarnings({ "ThrowableInstanceNeverThrown" })
	PersistenceException wrapStaleStateException(StaleStateException e) {
		if ( e instanceof StaleObjectStateException ) {
			StaleObjectStateException sose = (StaleObjectStateException) e;
			Serializable identifier = sose.getIdentifier();
			if ( identifier != null ) {
				Object entity = session.load( sose.getEntityName(), identifier );
				if ( entity instanceof Serializable ) {
					//avoid some user errors regarding boundary crossing
					return new OptimisticLockException( null, e, entity );
				}
				else {
					return new OptimisticLockException( e );
				}
			}
			else {
				return new OptimisticLockException( e );
			}
		}
		else {
			return new OptimisticLockException( e );
		}
	}

	@Override
	@SuppressWarnings( { "ThrowableInstanceNeverThrown", "unchecked" })
	public Object getSingleResult() {
		try {
			List result = query.list();
			if ( result.size() == 0 ) {
				throwPersistenceException( new NoResultException( "No entity found for query" ) );
			}
			else if ( result.size() > 1 ) {
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
			return null; //should never happen
		}
		catch (QueryTimeoutException e) {
			throwQueryTimeoutException( e );
			return null; //never happens
		}
		catch (QueryExecutionRequestException he) {
			throw new IllegalStateException( he );
		}
		catch (TypeMismatchException e) {
			throw new IllegalArgumentException( e );
		}
		catch (HibernateException he) {
			throwPersistenceException( he );
			return null;
		}
	}

	@Override
	public Query setMaxResults(int maxResults) {
		if ( maxResults < 0 ) {
			throw new IllegalArgumentException(
					"Negative ("
							+ maxResults
							+ ") parameter passed in to setMaxResults"
			);
		}
		query.setMaxResults( maxResults );
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
	public Query setFirstResult(int firstResult) {
		if ( firstResult < 0 ) {
			throw new IllegalArgumentException(
					"Negative ("
							+ firstResult
							+ ") parameter passed in to setFirstResult"
			);
		}
		query.setFirstResult( firstResult );
		this.firstResult = firstResult;
		return this;
	}

	@Override
	public int getFirstResult() {
		return firstResult == null ? 0 : firstResult;
	}

	@Override
	public Explanation explain(int documentId) {
		return query.explain( documentId );
	}

	@Override
	public FullTextQuery limitExecutionTimeTo(long timeout, TimeUnit timeUnit) {
		query.limitExecutionTimeTo( timeout, timeUnit );
		return this;
	}

	@Override
	public boolean hasPartialResults() {
		return query.hasPartialResults();
	}

	@Override
	public FullTextQuery initializeObjectsWith(ObjectLookupMethod lookupMethod, DatabaseRetrievalMethod retrievalMethod) {
		query.initializeObjectsWith( lookupMethod, retrievalMethod );
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
				//nothing
			}
			else if ( value instanceof String ) {
				query.setTimeout( Long.parseLong( (String) value ), TimeUnit.MILLISECONDS );
			}
			else if ( value instanceof Number ) {
				query.setTimeout( ( (Number) value ).longValue(), TimeUnit.MILLISECONDS );
			}
		}
		return this;
	}

	@Override
	public Map<String, Object> getHints() {
		return hints;
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
		if ( flushMode == FlushModeType.AUTO ) {
			query.setFlushMode( FlushMode.AUTO );
		}
		else if ( flushMode == FlushModeType.COMMIT ) {
			query.setFlushMode( FlushMode.COMMIT );
		}
		return this;
	}

	@Override
	public FlushModeType getFlushMode() {
		if ( jpaFlushMode != null ) {
			return jpaFlushMode;
		}
		final FlushMode hibernateFlushMode = session.getFlushMode();
		if ( FlushMode.AUTO == hibernateFlushMode ) {
			return FlushModeType.AUTO;
		}
		else if ( FlushMode.COMMIT == hibernateFlushMode ) {
			return FlushModeType.COMMIT;
		}
		else {
			return null; //incompatible flush mode
		}
	}

	@Override
	public Query setLockMode(LockModeType lockModeType) {
		throw new UnsupportedOperationException( "lock modes not supported in fullText queries" );
	}

	@Override
	public LockModeType getLockMode() {
		throw new UnsupportedOperationException( "lock modes not supported in fullText queries" );
	}

	@Override
	public <T> T unwrap(Class<T> type) {
		//I've purposely decided not to return the underlying Hibernate FullTextQuery
		//as I see this as an implementation detail that should not be exposed.
		return query.unwrap( type );
	}

}
