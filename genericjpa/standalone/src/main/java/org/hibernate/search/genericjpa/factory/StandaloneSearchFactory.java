/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.factory;

import java.io.Serializable;
import java.util.Collections;

import org.apache.lucene.search.Query;

import org.hibernate.search.genericjpa.query.HSearchQuery;
import org.hibernate.search.genericjpa.transaction.TransactionContext;

public interface StandaloneSearchFactory extends org.hibernate.search.SearchFactory {

	void close();

	void flushToIndexes(TransactionContext tc);

	void purgeAll(Class<?> entityClass, TransactionContext tc);

	default void purgeAll(Class<?> entityClass) {
		Transaction tc = new Transaction();
		try {
			this.purgeAll( entityClass, tc );
			tc.commit();
		}
		catch (Exception e) {
			tc.rollback();
			throw e;
		}
	}

	HSearchQuery createQuery(Query query, Class<?>... targetedEntities);

	void purge(Class<?> entityClass, Serializable id, TransactionContext tc);

	default void purge(Class<?> entityClass, Serializable id) {
		Transaction tc = new Transaction();
		try {
			this.purge( entityClass, id, tc );
			tc.commit();
		}
		catch (Exception e) {
			tc.rollback();
			throw e;
		}
	}

	void purge(Iterable<?> entities, TransactionContext tc);

	default void purge(Object entity, TransactionContext tc) {
		this.purge( Collections.singletonList( entity ), tc );
	}

	default void purge(Iterable<?> entities) {
		Transaction tc = new Transaction();
		try {
			this.purge( entities, tc );
			tc.commit();
		}
		catch (Exception e) {
			tc.rollback();
			throw e;
		}
	}

	default void purge(Object entity) {
		this.purge( Collections.singletonList( entity ) );
	}

	/**
	 * this first queries for all matching documents and then deletes them by their id
	 */
	@Deprecated
	default void purge(Class<?> entityClass, Query query) {
		Transaction tc = new Transaction();
		try {
			this.purge( entityClass, query, tc );
			tc.commit();
		}
		catch (Exception e) {
			tc.rollback();
			throw e;
		}
	}

	/**
	 * this first queries for all matching documents and then deletes them by their id
	 */
	@Deprecated
	void purge(Class<?> entityClass, Query query, TransactionContext tc);

	void purgeByTerm(Class<?> entityClass, String field, Integer val, TransactionContext tc);

	default void purgeByTerm(Class<?> entityClass, String field, Integer val) {
		Transaction tc = new Transaction();
		try {
			this.purgeByTerm( entityClass, field, val, tc );
			tc.commit();
		}
		catch (Exception e) {
			tc.rollback();
			throw e;
		}
	}

	void purgeByTerm(Class<?> entityClass, String field, Long val, TransactionContext tc);

	default void purgeByTerm(Class<?> entityClass, String field, Long val) {
		Transaction tc = new Transaction();
		try {
			this.purgeByTerm( entityClass, field, val, tc );
			tc.commit();
		}
		catch (Exception e) {
			tc.rollback();
			throw e;
		}
	}

	void purgeByTerm(Class<?> entityClass, String field, Float val, TransactionContext tc);

	default void purgeByTerm(Class<?> entityClass, String field, Float val) {
		Transaction tc = new Transaction();
		try {
			this.purgeByTerm( entityClass, field, val, tc );
			tc.commit();
		}
		catch (Exception e) {
			tc.rollback();
			throw e;
		}
	}

	void purgeByTerm(Class<?> entityClass, String field, Double val, TransactionContext tc);

	default void purgeByTerm(Class<?> entityClass, String field, Double val) {
		Transaction tc = new Transaction();
		try {
			this.purgeByTerm( entityClass, field, val, tc );
			tc.commit();
		}
		catch (Exception e) {
			tc.rollback();
			throw e;
		}
	}

	void purgeByTerm(Class<?> entityClass, String field, String val, TransactionContext tc);

	default void purgeByTerm(Class<?> entityClass, String field, String val) {
		Transaction tc = new Transaction();
		try {
			this.purgeByTerm( entityClass, field, val, tc );
			tc.commit();
		}
		catch (Exception e) {
			tc.rollback();
			throw e;
		}
	}

	// same names

	void index(Iterable<?> entities, TransactionContext tc);

	default void index(Object entity, TransactionContext tc) {
		this.index( Collections.singletonList( entity ), tc );
	}

	default void index(Iterable<?> entities) {
		Transaction tc = new Transaction();
		try {
			this.index( entities, tc );
			tc.commit();
		}
		catch (Exception e) {
			tc.rollback();
			throw e;
		}
	}

	default void index(Object entity) {
		this.index( Collections.singletonList( entity ) );
	}

	void update(Iterable<?> entities, TransactionContext tc);

	default void update(Object entity, TransactionContext tc) {
		this.update( Collections.singletonList( entity ), tc );
	}

	default void update(Iterable<?> entities) {
		Transaction tc = new Transaction();
		try {
			this.update( entities, tc );
			tc.commit();
		}
		catch (Exception e) {
			tc.rollback();
			throw e;
		}
	}

	default void update(Object entity) {
		this.update( Collections.singletonList( entity ) );
	}

	void delete(Iterable<?> entities, TransactionContext tc);

	default void delete(Object entity, TransactionContext tc) {
		this.delete( Collections.singletonList( entity ), tc );
	}

	default void delete(Iterable<?> entities) {
		Transaction tc = new Transaction();
		try {
			this.delete( entities, tc );
			tc.commit();
		}
		catch (Exception e) {
			tc.rollback();
			throw e;
		}
	}

	default void delete(Object entity) {
		this.delete( Collections.singletonList( entity ) );
	}

}
