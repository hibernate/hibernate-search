/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.entity.impl;

import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;
import java.util.List;
import java.util.Map;

import org.hibernate.search.genericjpa.jpa.util.impl.JPATransactionWrapper;

public class TransactionWrappedEntityManagerEntityProvider extends BasicEntityProvider {

	private final TransactionManager transactionManager;

	public TransactionWrappedEntityManagerEntityProvider(
			EntityManager em,
			Map<Class<?>, String> idProperties,
			TransactionManager transactionManager) {
		super( em, idProperties );
		this.transactionManager = transactionManager;
	}

	@Override
	public Object get(Class<?> entityClass, Object id, Map<String, Object> hints) {
		JPATransactionWrapper tx = JPATransactionWrapper.get( this.getEm(), this.transactionManager );
		tx.begin();
		try {
			Object ret = super.get( entityClass, id, hints );
			tx.commit();
			return ret;
		}
		catch (Exception e) {
			tx.rollback();
			throw e;
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List getBatch(Class<?> entityClass, List<Object> ids, Map<String, Object> hints) {
		JPATransactionWrapper tx = JPATransactionWrapper.get( this.getEm(), this.transactionManager );
		tx.begin();
		try {
			List ret = super.getBatch( entityClass, ids, hints );
			tx.commit();
			return ret;
		}
		catch (Exception e) {
			tx.rollback();
			throw e;
		}
	}

}
