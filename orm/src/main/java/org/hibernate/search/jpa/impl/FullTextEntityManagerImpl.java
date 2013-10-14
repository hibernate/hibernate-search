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
import java.util.List;
import java.util.Map;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.EntityTransaction;
import javax.persistence.EntityManagerFactory;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TypedQuery;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaUpdate;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.SearchException;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.Session;

/**
 * @author Emmanuel Bernard
 */
public class FullTextEntityManagerImpl implements FullTextEntityManager, Serializable {
	private static final Log log = LoggerFactory.make();

	private final EntityManager em;
	private FullTextSession ftSession;

	public FullTextEntityManagerImpl(EntityManager em) {
		if ( em == null ) {
			throw log.getNullSessionPassedToFullEntityManagerCreationException();
		}
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
				ftSession = Search.getFullTextSession( (Session) delegate );
			}
			else if ( EntityManager.class.isAssignableFrom( delegate.getClass() ) ) {
				//Some app servers wrap the EM twice
				delegate = ( (EntityManager) delegate ).getDelegate();
				if ( delegate == null ) {
					throw new SearchException(
							"Trying to use Hibernate Search without an Hibernate EntityManager (no delegate)"
					);
				}
				else if ( Session.class.isAssignableFrom( delegate.getClass() ) ) {
					ftSession = Search.getFullTextSession( (Session) delegate );
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

	@Override
	public FullTextQuery createFullTextQuery(org.apache.lucene.search.Query luceneQuery, Class<?>... entities) {
		FullTextSession ftSession = getFullTextSession();
		return new FullTextQueryImpl( ftSession.createFullTextQuery( luceneQuery, entities ), ftSession );
	}

	@Override
	public <T> void index(T entity) {
		getFullTextSession().index( entity );
	}

	@Override
	public SearchFactory getSearchFactory() {
		return getFullTextSession().getSearchFactory();
	}

	@Override
	public <T> void purge(Class<T> entityType, Serializable id) {
		getFullTextSession().purge( entityType, id );
	}

	@Override
	public <T> void purgeAll(Class<T> entityType) {
		getFullTextSession().purgeAll( entityType );
	}

	@Override
	public void flushToIndexes() {
		getFullTextSession().flushToIndexes();
	}

	@Override
	public void persist(Object entity) {
		em.persist( entity );
	}

	@Override
	public <T> T merge(T entity) {
		return em.merge( entity );
	}

	@Override
	public void remove(Object entity) {
		em.remove( entity );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey) {
		return em.find( entityClass, primaryKey );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> hints) {
		return em.find( entityClass, primaryKey, hints );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockModeType) {
		return em.find( entityClass, primaryKey, lockModeType );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockModeType, Map<String, Object> hints) {
		return em.find( entityClass, primaryKey, lockModeType, hints );
	}

	@Override
	public <T> T getReference(Class<T> entityClass, Object primaryKey) {
		return em.getReference( entityClass, primaryKey );
	}

	@Override
	public void flush() {
		em.flush();
	}

	@Override
	public void setFlushMode(FlushModeType flushMode) {
		em.setFlushMode( flushMode );
	}

	@Override
	public FlushModeType getFlushMode() {
		return em.getFlushMode();
	}

	@Override
	public void lock(Object entity, LockModeType lockMode) {
		em.lock( entity, lockMode );
	}

	@Override
	public void lock(Object entity, LockModeType lockModeType, Map<String, Object> hints) {
		em.lock( entity, lockModeType, hints );
	}

	@Override
	public void refresh(Object entity) {
		em.refresh( entity );
	}

	@Override
	public void refresh(Object entity, Map<String, Object> hints) {
		em.refresh( entity, hints );
	}

	@Override
	public void refresh(Object entity, LockModeType lockModeType) {
		em.refresh( entity, lockModeType );
	}

	@Override
	public void refresh(Object entity, LockModeType lockModeType, Map<String, Object> hints) {
		em.refresh( entity, lockModeType, hints );
	}

	@Override
	public void clear() {
		em.clear();
	}

	@Override
	public void detach(Object entity) {
		em.detach( entity );
	}

	@Override
	public boolean contains(Object entity) {
		return em.contains( entity );
	}

	@Override
	public LockModeType getLockMode(Object entity) {
		return em.getLockMode( entity );
	}

	@Override
	public void setProperty(String key, Object value) {
		em.setProperty( key, value );
	}

	@Override
	public Map<String, Object> getProperties() {
		return em.getProperties();
	}

	@Override
	public Query createQuery(String ejbqlString) {
		return em.createQuery( ejbqlString );
	}

	@Override
	public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
		return em.createQuery( criteriaQuery );
	}

	@Override
	public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
		return em.createQuery( qlString, resultClass );
	}

	@Override
	public Query createNamedQuery(String name) {
		return em.createNamedQuery( name );
	}

	@Override
	public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
		return em.createNamedQuery( name, resultClass );
	}

	@Override
	public Query createNativeQuery(String sqlString) {
		return em.createNativeQuery( sqlString );
	}

	@Override
	public Query createNativeQuery(String sqlString, Class resultClass) {
		return em.createNativeQuery( sqlString, resultClass );
	}

	@Override
	public Query createNativeQuery(String sqlString, String resultSetMapping) {
		return em.createNativeQuery( sqlString, resultSetMapping );
	}

	@Override
	public void joinTransaction() {
		em.joinTransaction();
	}

	@Override
	public <T> T unwrap(Class<T> type) {
		if ( type.equals( FullTextSession.class ) ) {
			@SuppressWarnings("unchecked")
			final T ftSession = (T) Search.getFullTextSession( em.unwrap( Session.class ) );
			return ftSession;
		}
		else {
			return em.unwrap( type );
		}
	}

	@Override
	public Object getDelegate() {
		return em.getDelegate();
	}

	@Override
	public void close() {
		em.close();
	}

	@Override
	public boolean isOpen() {
		return em.isOpen();
	}

	@Override
	public EntityTransaction getTransaction() {
		return em.getTransaction();
	}

	@Override
	public EntityManagerFactory getEntityManagerFactory() {
		return em.getEntityManagerFactory();
	}

	@Override
	public CriteriaBuilder getCriteriaBuilder() {
		return em.getCriteriaBuilder();
	}

	@Override
	public Metamodel getMetamodel() {
		return em.getMetamodel();
	}

	@Override
	public MassIndexer createIndexer(Class<?>... types) {
		return getFullTextSession().createIndexer( types );
	}

	@Override
	public Query createQuery(CriteriaUpdate updateQuery) {
		return em.createQuery( updateQuery );
	}

	@Override
	public Query createQuery(CriteriaDelete deleteQuery) {
		return em.createQuery( deleteQuery );
	}

	@Override
	public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
		return em.createNamedStoredProcedureQuery( name );
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
		return em.createStoredProcedureQuery( procedureName );
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses) {
		return em.createStoredProcedureQuery( procedureName, resultClasses );
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
		return em.createStoredProcedureQuery( procedureName, resultSetMappings );
	}

	@Override
	public boolean isJoinedToTransaction() {
		return em.isJoinedToTransaction();
	}

	@Override
	public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
		return em.createEntityGraph( rootType );
	}

	@Override
	public EntityGraph<?> createEntityGraph(String graphName) {
		return em.createEntityGraph( graphName );
	}

	@Override
	public EntityGraph<?> getEntityGraph(String graphName) {
		return em.getEntityGraph( graphName );
	}

	@Override
	public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
		return em.getEntityGraphs( entityClass );
	}

}
