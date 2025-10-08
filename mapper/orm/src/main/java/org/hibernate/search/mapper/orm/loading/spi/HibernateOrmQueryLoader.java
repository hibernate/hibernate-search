/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.spi;

import java.util.List;

import jakarta.persistence.FindOption;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.Query;

public interface HibernateOrmQueryLoader<E, I> {
	Query<Long> createCountQuery(SharedSessionContractImplementor session);

	Query<I> createIdentifiersQuery(SharedSessionContractImplementor session);

	Query<E> createLoadingQuery(SessionImplementor session, String idParameterName);

	/**
	 * @deprecated Use {@link #findMultiple(SessionImplementor, List, FindOption...)} instead.
	 */
	@Deprecated(forRemoval = true, since = "8.2")
	@SuppressWarnings("removal")
	org.hibernate.MultiIdentifierLoadAccess<E> createMultiIdentifierLoadAccess(SessionImplementor session);

	List<E> findMultiple(SessionImplementor session, List<?> ids, FindOption... options);

	boolean uniquePropertyIsTheEntityId();
}
