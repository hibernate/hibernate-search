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
import java.util.List;
import java.util.Map;

/**
 * Created by Martin on 23.06.2015.
 */
final class DummyEntityManager implements EntityManager {

	private boolean closed = false;

	@Override
	public void close() {
		this.closed = true;
	}

	@Override
	public boolean isOpen() {
		return !this.closed;
	}

	@Override
	public void persist(Object entity) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public <T> T merge(T entity) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public void remove(Object entity) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public <T> T find(
			Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public <T> T getReference(Class<T> entityClass, Object primaryKey) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public void flush() {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public void setFlushMode(FlushModeType flushMode) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public FlushModeType getFlushMode() {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public void lock(Object entity, LockModeType lockMode) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public void refresh(Object entity) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public void refresh(Object entity, Map<String, Object> properties) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public void refresh(Object entity, LockModeType lockMode) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public void detach(Object entity) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public boolean contains(Object entity) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public LockModeType getLockMode(Object entity) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public void setProperty(String propertyName, Object value) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public Map<String, Object> getProperties() {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public Query createQuery(String qlString) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public Query createQuery(CriteriaUpdate updateQuery) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public Query createQuery(CriteriaDelete deleteQuery) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public Query createNamedQuery(String name) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public Query createNativeQuery(String sqlString) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public Query createNativeQuery(String sqlString, Class resultClass) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public Query createNativeQuery(String sqlString, String resultSetMapping) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public void joinTransaction() {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public boolean isJoinedToTransaction() {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public <T> T unwrap(Class<T> cls) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public Object getDelegate() {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public EntityTransaction getTransaction() {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public EntityManagerFactory getEntityManagerFactory() {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public CriteriaBuilder getCriteriaBuilder() {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public Metamodel getMetamodel() {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public EntityGraph<?> createEntityGraph(String graphName) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public EntityGraph<?> getEntityGraph(String graphName) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}

	@Override
	public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
		throw new UnsupportedOperationException(
				"this is just a DummyEntityManager used when the original EntityManager was null!"
		);
	}
}
