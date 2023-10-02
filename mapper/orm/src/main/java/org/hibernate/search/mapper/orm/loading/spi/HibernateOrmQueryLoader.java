/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.spi;

import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.Query;

public interface HibernateOrmQueryLoader<E, I> {
	Query<Long> createCountQuery(SharedSessionContractImplementor session);

	Query<I> createIdentifiersQuery(SharedSessionContractImplementor session);

	Query<E> createLoadingQuery(SessionImplementor session, String idParameterName);

	MultiIdentifierLoadAccess<E> createMultiIdentifierLoadAccess(SessionImplementor session);

	boolean uniquePropertyIsTheEntityId();
}
