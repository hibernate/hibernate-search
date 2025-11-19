/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.session.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.Transaction;
import org.hibernate.engine.extension.spi.Extension;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;

public class HibernateOrmSearchSessionExtension implements Serializable, Extension {

	public static HibernateOrmSearchSessionExtension get(SharedSessionContractImplementor session) {
		return session.getExtension( HibernateOrmSearchSessionExtension.class );
	}

	// Everything here should be transient because the holder might get serialized along with a Hibernate ORM session.
	// The Hibernate Search data (indexing plans in particular) will be lost in the process,
	// but that's the best we can do.
	private transient HibernateOrmSearchSession searchSession;
	private transient Map<Transaction, PojoIndexingPlan> planPerTransaction;

	public static HibernateOrmSearchSessionExtension init() {
		return new HibernateOrmSearchSessionExtension();
	}

	public HibernateOrmSearchSession searchSession() {
		return searchSession;
	}

	public void searchSession(HibernateOrmSearchSession searchSession) {
		this.searchSession = searchSession;
	}

	public PojoIndexingPlan pojoIndexingPlan(Transaction transaction) {
		return planPerTransaction == null ? null : planPerTransaction.get( transaction );
	}

	public void pojoIndexingPlan(Transaction transaction, PojoIndexingPlan plan) {
		if ( planPerTransaction == null ) {
			planPerTransaction = new HashMap<>();
		}
		planPerTransaction.put( transaction, plan );
	}

	public void clear(Transaction transactionIdentifier) {
		if ( planPerTransaction == null ) {
			return;
		}
		planPerTransaction.remove( transactionIdentifier );
	}
}
