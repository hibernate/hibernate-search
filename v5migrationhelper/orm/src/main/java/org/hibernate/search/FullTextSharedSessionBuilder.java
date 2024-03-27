/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search;

import java.sql.Connection;

import jakarta.persistence.EntityManager;

import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.search.mapper.orm.session.SearchSession;

/**
 * @author Emmanuel Bernard
 * @deprecated Will be removed without replacement, as Hibernate Search sessions ({@link SearchSession})
 * no longer extend JPA's {@link EntityManager} interface or Hibernate ORM's {@link Session}.
 * To get access to a Hibernate Search 6 session, use {@link org.hibernate.search.mapper.orm.Search#session(Session)}.
 */
@Deprecated
public interface FullTextSharedSessionBuilder extends SharedSessionBuilder {
	@Override
	FullTextSharedSessionBuilder interceptor();

	@Override
	FullTextSharedSessionBuilder connection();

	@Deprecated
	@Override
	FullTextSharedSessionBuilder connectionReleaseMode();

	@Override
	FullTextSharedSessionBuilder autoJoinTransactions();

	@Override
	FullTextSharedSessionBuilder autoClose();

	@Override
	FullTextSharedSessionBuilder interceptor(Interceptor interceptor);

	@Override
	FullTextSharedSessionBuilder noInterceptor();

	@Override
	FullTextSharedSessionBuilder connection(Connection connection);

	@Override
	FullTextSharedSessionBuilder autoJoinTransactions(boolean autoJoinTransactions);

	@Deprecated
	@Override
	FullTextSharedSessionBuilder autoClose(boolean autoClose);

	@Override
	FullTextSession openSession();

	@Deprecated(forRemoval = true)
	@Override
	FullTextSharedSessionBuilder tenantIdentifier(String tenantIdentifier);

	@Override
	FullTextSharedSessionBuilder tenantIdentifier(Object tenantIdentifier);
}
