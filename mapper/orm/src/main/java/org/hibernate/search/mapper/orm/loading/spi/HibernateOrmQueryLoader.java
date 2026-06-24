/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.spi;

import java.util.List;

import jakarta.persistence.FindOption;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.SelectionQuery;

public interface HibernateOrmQueryLoader<E, I> {
	SelectionQuery<Long> createCountQuery(SharedSessionContractImplementor session);

	SelectionQuery<I> createIdentifiersQuery(SharedSessionContractImplementor session);

	SelectionQuery<E> createLoadingQuery(SessionImplementor session, String idParameterName);

	List<E> findMultiple(SessionImplementor session, List<?> ids, FindOption... options);

	boolean uniquePropertyIsTheEntityId();
}
