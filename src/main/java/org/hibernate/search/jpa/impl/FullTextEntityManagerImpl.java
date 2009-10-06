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
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.EntityTransaction;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.QueryBuilder;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.SearchException;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.Session;

/**
 * @author Emmanuel Bernard
 */
public class FullTextEntityManagerImpl implements FullTextEntityManager, Serializable {
	private final EntityManager em;
	private FullTextSession ftSession;

	public FullTextEntityManagerImpl(EntityManager em) {
		this.em = em;
	}

	private FullTextSession getFullTextSession() {
		if ( ftSession == null ) {
			Object delegate = em.getDelegate();
			if ( delegate == null ) {
				throw new SearchException(
						"Trying to use Hibernate Search without an Hibernate EntityManager (no delegate)"
				);
			}
			else if ( Session.class.isAssignableFrom( delegate.getClass() ) ) {
				ftSession = Search.getFullTextSession( ( Session ) delegate );
			}
			else if ( EntityManager.class.isAssignableFrom( delegate.getClass() ) ) {
				//Some app servers wrap the EM twice
				delegate = ( ( EntityManager ) delegate ).getDelegate();
				if ( delegate == null ) {
					throw new SearchException(
							"Trying to use Hibernate Search without an Hibernate EntityManager (no delegate)"
					);
				}
				else if ( Session.class.isAssignableFrom( delegate.getClass() ) ) {
					ftSession = Search.getFullTextSession( ( Session ) delegate );
				}
				else {
					throw new SearchException(
							"Trying to use Hibernate Search without an Hibernate EntityManager: " + delegate.getClass()
					);
				}
			}
			else {
				throw new SearchException(
						"Trying to use Hibernate Search without an Hibernate EntityManager: " + delegate.getClass()
				);
			}
		}
		return ftSession;
	}

	public FullTextQuery createFullTextQuery(org.apache.lucene.search.Query luceneQuery, Class<?>... entities) {
		FullTextSession ftSession = getFullTextSession();
		return new FullTextQueryImpl( ftSession.createFullTextQuery( luceneQuery, entities ), ftSession );
	}

	public <T> void index(T entity) {
		getFullTextSession().index( entity );
	}

	public SearchFactory getSearchFactory() {
		return getFullTextSession().getSearchFactory();
	}

	public <T> void purge(Class<T> entityType, Serializable id) {
		getFullTextSession().purge( entityType, id );
	}

	public <T> void purgeAll(Class<T> entityType) {
		getFullTextSession().purgeAll( entityType );
	}

	public void flushToIndexes() {
		getFullTextSession().flushToIndexes();
	}

	public void persist(Object entity) {
		em.persist( entity );
	}

	public <T> T merge(T entity) {
		return em.merge( entity );
	}

	public void remove(Object entity) {
		em.remove( entity );
	}

	public <T> T find(Class<T> entityClass, Object primaryKey) {
		return em.find( entityClass, primaryKey );
	}

	public <T> T find(Class<T> tClass, Object o, Map<String, Object> stringObjectMap) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public <T> T find(Class<T> tClass, Object o, LockModeType lockModeType) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public <T> T find(Class<T> tClass, Object o, LockModeType lockModeType, Map<String, Object> stringObjectMap) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public <T> T getReference(Class<T> entityClass, Object primaryKey) {
		return em.getReference( entityClass, primaryKey );
	}

	public void flush() {
		em.flush();
	}

	public void setFlushMode(FlushModeType flushMode) {
		em.setFlushMode( flushMode );
	}

	public FlushModeType getFlushMode() {
		return em.getFlushMode();
	}

	public void lock(Object entity, LockModeType lockMode) {
		em.lock( entity, lockMode );
	}

	public void lock(Object o, LockModeType lockModeType, Map<String, Object> stringObjectMap) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public void refresh(Object entity) {
		em.refresh( entity );
	}

	public void refresh(Object o, Map<String, Object> stringObjectMap) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public void refresh(Object o, LockModeType lockModeType) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public void refresh(Object o, LockModeType lockModeType, Map<String, Object> stringObjectMap) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public void clear() {
		em.clear();
	}

	public void detach(Object o) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public boolean contains(Object entity) {
		return em.contains( entity );
	}

	public LockModeType getLockMode(Object o) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public void setProperty(String s, Object o) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public Map<String, Object> getProperties() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Set<String> getSupportedProperties() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Query createQuery(String ejbqlString) {
		return em.createQuery( ejbqlString );
	}

	public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
		return em.createQuery( criteriaQuery );
	}

	public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
		return em.createQuery( qlString, resultClass );
	}

	public Query createNamedQuery(String name) {
		return em.createNamedQuery( name );
	}

	public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
		return em.createNamedQuery( name, resultClass );
	}

	public Query createNativeQuery(String sqlString) {
		return em.createNativeQuery( sqlString );
	}

	public Query createNativeQuery(String sqlString, Class resultClass) {
		return em.createNativeQuery( sqlString, resultClass );
	}

	public Query createNativeQuery(String sqlString, String resultSetMapping) {
		return em.createNativeQuery( sqlString, resultSetMapping );
	}

	public void joinTransaction() {
		em.joinTransaction();
	}

	public <T> T unwrap(Class<T> tClass) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Object getDelegate() {
		return em.getDelegate();
	}

	public void close() {
		em.close();
	}

	public boolean isOpen() {
		return em.isOpen();
	}

	public EntityTransaction getTransaction() {
		return em.getTransaction();
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public QueryBuilder getQueryBuilder() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Metamodel getMetamodel() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public MassIndexer createIndexer(Class<?>... types) {
		return getFullTextSession().createIndexer( types );
	}
	
}
