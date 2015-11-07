/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.impl;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.Metamodel;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.genericjpa.batchindexing.MassIndexer;
import org.hibernate.search.genericjpa.factory.Transaction;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;

/**
 * @author Emmanuel Bernard
 * @author Martin Braun
 */
public final class FullTextEntityManagerImpl implements FullTextEntityManager, Serializable {

	private static final long serialVersionUID = 5348732906148044722L;

	private final EntityManager em;
	private final JPASearchFactoryAdapter searchFactory;
	private Transaction standaloneTransaction;

	public FullTextEntityManagerImpl(EntityManager em, JPASearchFactoryAdapter searchFactory) {
		this.em = em;
		this.searchFactory = searchFactory;
	}

	//FIXME: register a eventlistener for the current EM in here so we can listen
	//for session events. this is at least possible for Hibernate and EclipseLink

	//the search factory has to know about the currently used entitymanagers then,
	//since the entitylisteners are built ontop of the EMF level. then we have
	//to map back to the current EntityManager, use that for event handling
	//(retrieve the changed data) and also wait for its transaction to be commited.
	//only after the original TX is commited, we are allowed to automatically
	//flush to the index (standaloneTransaction.commit())

	@Override
	public boolean isSearchTransactionInProgress() {
		return this.standaloneTransaction != null;
	}

	@Override
	public void beginSearchTransaction() {
		if ( this.isSearchTransactionInProgress() ) {
			throw new IllegalStateException( "a search transaction is already in progress!" );
		}
		this.standaloneTransaction = new Transaction();
	}

	@Override
	public void commitSearchTransaction() {
		if ( !this.isSearchTransactionInProgress() ) {
			throw new IllegalStateException( "no search transaction is in progress!" );
		}
		this.standaloneTransaction.commit();
		this.standaloneTransaction = null;
	}

	@Override
	public void rollbackSearchTransaction() {
		if ( !this.isSearchTransactionInProgress() ) {
			throw new IllegalStateException( "no search transaction is in progress!" );
		}
		this.standaloneTransaction.rollback();
		this.standaloneTransaction = null;
	}

	@Override
	public FullTextQuery createFullTextQuery(org.apache.lucene.search.Query luceneQuery, Class<?>... entities) {
		return new FullTextQueryImpl(
				this.searchFactory.createQuery( luceneQuery, entities ),
				this.searchFactory.entityProvider( this.em )
		);
	}

	@Override
	public <T> void index(T entity) {
		if ( !this.isSearchTransactionInProgress() ) {
			throw new IllegalStateException( "no search transaction is in progress!" );
		}
		this.searchFactory.index( entity, this.standaloneTransaction );
	}

	@Override
	public <T> void purge(Class<T> entityType, Serializable id) {
		if ( !this.isSearchTransactionInProgress() ) {
			throw new IllegalStateException( "no search transaction is in progress!" );
		}
		this.searchFactory.purge( entityType, id, this.standaloneTransaction );
	}

	@Override
	public <T> void purgeAll(Class<T> entityType) {
		if ( !this.isSearchTransactionInProgress() ) {
			throw new IllegalStateException( "no search transaction is in progress!" );
		}
		this.searchFactory.purgeAll( entityType, this.standaloneTransaction );
	}

	@Override
	public <T> void purgeByTerm(Class<T> entityType, String field, Integer val) {
		if ( !this.isSearchTransactionInProgress() ) {
			throw new IllegalStateException( "no search transaction is in progress!" );
		}
		this.searchFactory.purgeByTerm( entityType, field, val );
	}

	@Override
	public <T> void purgeByTerm(Class<T> entityType, String field, Long val) {
		if ( !this.isSearchTransactionInProgress() ) {
			throw new IllegalStateException( "no search transaction is in progress!" );
		}
		this.searchFactory.purgeByTerm( entityType, field, val );
	}

	@Override
	public <T> void purgeByTerm(Class<T> entityType, String field, Float val) {
		if ( !this.isSearchTransactionInProgress() ) {
			throw new IllegalStateException( "no search transaction is in progress!" );
		}
		this.searchFactory.purgeByTerm( entityType, field, val );
	}

	@Override
	public <T> void purgeByTerm(Class<T> entityType, String field, Double val) {
		if ( !this.isSearchTransactionInProgress() ) {
			throw new IllegalStateException( "no search transaction is in progress!" );
		}
		this.searchFactory.purgeByTerm( entityType, field, val );
	}

	@Override
	public <T> void purgeByTerm(Class<T> entityType, String field, String val) {
		if ( !this.isSearchTransactionInProgress() ) {
			throw new IllegalStateException( "no search transaction is in progress!" );
		}
		this.searchFactory.purgeByTerm( entityType, field, val );
	}

	@Override
	public void flushToIndexes() {
		if ( !this.isSearchTransactionInProgress() ) {
			throw new IllegalStateException( "no search transaction is in progress!" );
		}
		this.searchFactory.flushToIndexes( this.standaloneTransaction );
	}

	public void close() {
		this.em.close();
		if ( this.isSearchTransactionInProgress() ) {
			throw new IllegalStateException(
					"search transaction is in progress, underlying entity-manager was still closed to not generate a resource-leak!"
			);
		}
	}

	@Override
	public SearchFactory getSearchFactory() {
		return this.searchFactory;
	}

	@Override
	public MassIndexer createIndexer(Class<?>... types) {
		if ( types == null || types.length == 0 ) {
			return this.searchFactory.createMassIndexer();
		}
		else {
			return this.searchFactory.createMassIndexer( Arrays.asList( types ) );
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> clazz) {
		if ( FullTextEntityManagerImpl.class.equals( clazz ) || FullTextEntityManager.class.equals( clazz ) ) {
			return (T) this;
		}
		return em.unwrap( clazz );
	}

	public void clear() {
		this.em.clear();
	}

	public boolean contains(Object arg0) {
		return this.em.contains( arg0 );
	}

	public <T> EntityGraph<T> createEntityGraph(Class<T> arg0) {
		return this.em.createEntityGraph( arg0 );
	}

	public EntityGraph<?> createEntityGraph(String arg0) {
		return this.em.createEntityGraph( arg0 );
	}

	public <T> TypedQuery<T> createNamedQuery(String arg0, Class<T> arg1) {
		return this.em.createNamedQuery( arg0, arg1 );
	}

	public Query createNamedQuery(String arg0) {
		return this.em.createNamedQuery( arg0 );
	}

	public StoredProcedureQuery createNamedStoredProcedureQuery(String arg0) {
		return this.em.createNamedStoredProcedureQuery( arg0 );
	}

	public Query createNativeQuery(String arg0, @SuppressWarnings("rawtypes") Class arg1) {
		return this.em.createNativeQuery( arg0, arg1 );
	}

	public Query createNativeQuery(String arg0, String arg1) {
		return this.em.createNativeQuery( arg0, arg1 );
	}

	public Query createNativeQuery(String arg0) {
		return this.em.createNativeQuery( arg0 );
	}

	public Query createQuery(@SuppressWarnings("rawtypes") CriteriaDelete arg0) {
		return this.em.createQuery( arg0 );
	}

	public <T> TypedQuery<T> createQuery(CriteriaQuery<T> arg0) {
		return this.em.createQuery( arg0 );
	}

	public Query createQuery(@SuppressWarnings("rawtypes") CriteriaUpdate arg0) {
		return this.em.createQuery( arg0 );
	}

	public <T> TypedQuery<T> createQuery(String arg0, Class<T> arg1) {
		return this.em.createQuery( arg0, arg1 );
	}

	public Query createQuery(String arg0) {
		return this.em.createQuery( arg0 );
	}

	public StoredProcedureQuery createStoredProcedureQuery(String arg0, @SuppressWarnings("rawtypes") Class... arg1) {
		return this.em.createStoredProcedureQuery( arg0, arg1 );
	}

	public StoredProcedureQuery createStoredProcedureQuery(String arg0, String... arg1) {
		return this.em.createStoredProcedureQuery( arg0, arg1 );
	}

	public StoredProcedureQuery createStoredProcedureQuery(String arg0) {
		return this.em.createStoredProcedureQuery( arg0 );
	}

	public void detach(Object arg0) {
		this.em.detach( arg0 );
	}

	public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2, Map<String, Object> arg3) {
		return this.em.find( arg0, arg1, arg2, arg3 );
	}

	public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2) {
		return this.em.find( arg0, arg1, arg2 );
	}

	public <T> T find(Class<T> arg0, Object arg1, Map<String, Object> arg2) {
		return this.em.find( arg0, arg1, arg2 );
	}

	public <T> T find(Class<T> arg0, Object arg1) {
		return this.em.find( arg0, arg1 );
	}

	public void flush() {
		this.em.flush();
	}

	public CriteriaBuilder getCriteriaBuilder() {
		return this.em.getCriteriaBuilder();
	}

	public Object getDelegate() {
		return this.em.getDelegate();
	}

	public EntityGraph<?> getEntityGraph(String arg0) {
		return this.em.getEntityGraph( arg0 );
	}

	public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> arg0) {
		return this.em.getEntityGraphs( arg0 );
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return this.em.getEntityManagerFactory();
	}

	public FlushModeType getFlushMode() {
		return this.em.getFlushMode();
	}

	public void setFlushMode(FlushModeType arg0) {
		this.em.setFlushMode( arg0 );
	}

	public LockModeType getLockMode(Object arg0) {
		return this.em.getLockMode( arg0 );
	}

	public Metamodel getMetamodel() {
		return this.em.getMetamodel();
	}

	public Map<String, Object> getProperties() {
		return this.em.getProperties();
	}

	public <T> T getReference(Class<T> arg0, Object arg1) {
		return this.em.getReference( arg0, arg1 );
	}

	public EntityTransaction getTransaction() {
		return this.em.getTransaction();
	}

	public boolean isJoinedToTransaction() {
		return this.em.isJoinedToTransaction();
	}

	public boolean isOpen() {
		return this.em.isOpen();
	}

	public void joinTransaction() {
		this.em.joinTransaction();
	}

	public void lock(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
		this.em.lock( arg0, arg1, arg2 );
	}

	public void lock(Object arg0, LockModeType arg1) {
		this.em.lock( arg0, arg1 );
	}

	public <T> T merge(T arg0) {
		return this.em.merge( arg0 );
	}

	public void persist(Object arg0) {
		this.em.persist( arg0 );
	}

	public void refresh(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
		this.em.refresh( arg0, arg1, arg2 );
	}

	public void refresh(Object arg0, LockModeType arg1) {
		this.em.refresh( arg0, arg1 );
	}

	public void refresh(Object arg0, Map<String, Object> arg1) {
		this.em.refresh( arg0, arg1 );
	}

	public void refresh(Object arg0) {
		this.em.refresh( arg0 );
	}

	public void remove(Object arg0) {
		this.em.remove( arg0 );
	}

	public void setProperty(String arg0, Object arg1) {
		this.em.setProperty( arg0, arg1 );
	}

}
