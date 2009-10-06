/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.persistence.FlushModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;

import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.Explanation;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.QueryException;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.TransientObjectException;
import org.hibernate.TypeMismatchException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.hql.QueryExecutionRequestException;
import org.hibernate.search.FullTextFilter;
import org.hibernate.search.SearchException;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.transform.ResultTransformer;

/**
 * @author Emmanuel Bernard
 */
public class FullTextQueryImpl implements FullTextQuery {
	private final org.hibernate.search.FullTextQuery query;
	private final Session session;

	public FullTextQueryImpl(org.hibernate.search.FullTextQuery query, Session session) {
		this.query = query;
		this.session = session;
	}

	public FullTextQuery setSort(Sort sort) {
		query.setSort( sort );
		return this;
	}

	public FullTextQuery setFilter(Filter filter) {
		query.setFilter( filter );
		return this;
	}

	public int getResultSize() {
		return query.getResultSize();
	}

	public FullTextQuery setCriteriaQuery(Criteria criteria) {
		query.setCriteriaQuery( criteria );
		return this;
	}

	public FullTextQuery setProjection(String... fields) {
		query.setProjection( fields );
		return this;
	}

	public FullTextFilter enableFullTextFilter(String name) {
		return query.enableFullTextFilter( name );
	}

	public void disableFullTextFilter(String name) {
		query.disableFullTextFilter( name );
	}

	public FullTextQuery setResultTransformer(ResultTransformer transformer) {
		query.setResultTransformer( transformer );
		return this;
	}

	public List getResultList() {
		try {
			return query.list();
		}
		catch (QueryExecutionRequestException he) {
			//TODO when an illegal state exception should be raised?
			throw new IllegalStateException(he);
		}
		catch( TypeMismatchException e ) {
			//TODO when an illegal arg exception should be raised?
			throw new IllegalArgumentException(e);
		}
		catch (SearchException he) {
			throwPersistenceException( he );
			throw he;
		}
	}

	//TODO mutualize this code with the EM this will fix the rollback issues
	@SuppressWarnings( { "ThrowableInstanceNeverThrown" } )
	private void throwPersistenceException(Exception e) {
		if ( e instanceof StaleStateException ) {
			PersistenceException pe = wrapStaleStateException( (StaleStateException) e );
			throwPersistenceException( pe );
		}
		else if ( e instanceof ConstraintViolationException ) {
			//FIXME this is bad cause ConstraintViolationException happens in other circumstances
			throwPersistenceException( new EntityExistsException( e ) );
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

	void throwPersistenceException(PersistenceException e) {
		if ( ! ( e instanceof NoResultException || e instanceof NonUniqueResultException ) ) {
			//FIXME rollback
		}
		throw e;
	}

	@SuppressWarnings( { "ThrowableInstanceNeverThrown" } )
	PersistenceException wrapStaleStateException(StaleStateException e) {
		PersistenceException pe;
		if ( e instanceof StaleObjectStateException ) {
			StaleObjectStateException sose = (StaleObjectStateException) e;
			Serializable identifier = sose.getIdentifier();
			if (identifier != null) {
				Object entity = session.load( sose.getEntityName(), identifier );
				if ( entity instanceof Serializable ) {
					//avoid some user errors regarding boundary crossing
					pe = new OptimisticLockException( null, e, entity );
				}
				else {
					pe = new OptimisticLockException( e );
				}
			}
			else {
				pe = new OptimisticLockException( e );
			}
		}
		else {
			pe = new OptimisticLockException( e );
		}
		return pe;
	}

	@SuppressWarnings( { "ThrowableInstanceNeverThrown" } )
	public Object getSingleResult() {
		try {
			List result = query.list();
			if ( result.size() == 0 ) {
				throwPersistenceException( new NoResultException( "No entity found for query" ) );
			}
			else if ( result.size() > 1 ) {
				Set uniqueResult = new HashSet(result);
				if ( uniqueResult.size() > 1 ) {
					throwPersistenceException( new NonUniqueResultException( "result returns " + uniqueResult.size() + " elements") );
				}
				else {
					return uniqueResult.iterator().next();
				}

			}
			else {
				return result.get(0);
			}
			return null; //should never happen
		}
		catch (QueryExecutionRequestException he) {
			throw new IllegalStateException(he);
		}
		catch( TypeMismatchException e ) {
			throw new IllegalArgumentException(e);
		}
		catch (HibernateException he) {
			throwPersistenceException( he );
			return null;
		}
	}

	public Query setMaxResults(int maxResult) {
		if ( maxResult < 0 ) {
			throw new IllegalArgumentException(
					"Negative ("
							+ maxResult
							+ ") parameter passed in to setMaxResults"
			);
		}
		query.setMaxResults( maxResult );
		return this;
	}

	public int getMaxResults() {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Query setFirstResult(int firstResult) {
		if ( firstResult < 0 ) {
			throw new IllegalArgumentException(
					"Negative ("
							+ firstResult
							+ ") parameter passed in to setFirstResult"
			);
		}
		query.setFirstResult( firstResult );
		return this;
	}

	public int getFirstResult() {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Explanation explain(int documentId) {
		return query.explain( documentId );
	}

	public int executeUpdate() {
		throw new IllegalStateException( "Update not allowed in FullTextQueries" );
	}

	public Query setHint(String hintName, Object value) {
		return this;
	}

	public Map<String, Object> getHints() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Set<String> getSupportedHints() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Query setParameter(String name, Object value) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries");
	}

	public Query setParameter(String name, Date value, TemporalType temporalType) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries");
	}

	public Query setParameter(String name, Calendar value, TemporalType temporalType) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries");
	}

	public Query setParameter(int position, Object value) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries");
	}

	public Query setParameter(int position, Date value, TemporalType temporalType) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries");
	}

	public Query setParameter(int position, Calendar value, TemporalType temporalType) {
		throw new UnsupportedOperationException( "parameters not supported in fullText queries");
	}

	//FIXME remove old JPA 2 contract
	public Map<String, Object> getNamedParameters() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	//FIXME remove old JPA 2 contract
	public List getPositionalParameters() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	//FIXME
	public Set<Parameter<?>> getParameters() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	//FIXME
	public Parameter<?> getParameter(String name) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	//FIXME
	public Parameter<?> getParameter(int position) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	//FIXME
	public <T> Parameter<T> getParameter(String name, Class<T> type) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	//FIXME
	public <T> Parameter<T> getParameter(int position, Class<T> type) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	//FIXME
	public boolean isBound(Parameter<?> param) {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	//FIXME
	public <T> T getParameterValue(Parameter<T> param) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	//FIXME
	public Object getParameterValue(String name) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	//FIXME
	public Object getParameterValue(int position) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Query setFlushMode(FlushModeType flushMode) {
		if ( flushMode == FlushModeType.AUTO ) {
			query.setFlushMode( FlushMode.AUTO );
		}
		else if ( flushMode == FlushModeType.COMMIT ) {
			query.setFlushMode( FlushMode.COMMIT );
		}
		return this;
	}

	public FlushModeType getFlushMode() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Query setLockMode(LockModeType lockModeType) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public LockModeType getLockMode() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public <T> T unwrap(Class<T> tClass) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
