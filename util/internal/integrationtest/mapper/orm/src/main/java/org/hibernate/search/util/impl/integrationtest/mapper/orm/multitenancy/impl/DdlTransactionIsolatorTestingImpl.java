/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm.multitenancy.impl;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.resource.transaction.backend.jdbc.internal.DdlTransactionIsolatorNonJtaImpl;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.internal.exec.JdbcContext;

/**
 * Copied from Hibernate ORM: being originally part of the `org.hibernate.test.util` package,
 * this is not published in a jar which could be consumed by Hibernate Search.
 * This copy is not meant to stay here for long.
 * Original class name: org.hibernate.test.util.DdlTransactionIsolatorTestingImpl
 *
 * @author Steve Ebersole
 */
final class DdlTransactionIsolatorTestingImpl extends DdlTransactionIsolatorNonJtaImpl {

	public DdlTransactionIsolatorTestingImpl(ServiceRegistry serviceRegistry,
			JdbcConnectionAccess jdbcConnectionAccess) {
		super( createJdbcContext( jdbcConnectionAccess, serviceRegistry ) );
	}

	public static JdbcContext createJdbcContext(
			JdbcConnectionAccess jdbcConnectionAccess,
			ServiceRegistry serviceRegistry) {
		return new JdbcContext() {
			final JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );

			@Override
			public JdbcConnectionAccess getJdbcConnectionAccess() {
				return jdbcConnectionAccess;
			}

			@Override
			public Dialect getDialect() {
				return jdbcServices.getJdbcEnvironment().getDialect();
			}

			@Override
			public SqlStatementLogger getSqlStatementLogger() {
				return jdbcServices.getSqlStatementLogger();
			}

			@Override
			public SqlExceptionHelper getSqlExceptionHelper() {
				return jdbcServices.getSqlExceptionHelper();
			}

			@Override
			public ServiceRegistry getServiceRegistry() {
				return serviceRegistry;
			}
		};
	}
}
