/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm.multitenancy.impl;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.DatabaseContainer;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.SimpleSessionFactoryBuilder;

/**
 * Utility to help setting up a test SessionFactory which uses multi-tenancy based
 * on multiple databases.
 *
 * @author Sanne Grinovero
 * @since 5.4
 */
public class MultitenancyTestHelper {

	public static void enable(SimpleSessionFactoryBuilder builder, Object... tenantIds) {
		MultitenancyTestHelper helper = new MultitenancyTestHelper( tenantIds );
		helper.attachTo( builder );
	}

	private final Object[] tenantIds;

	private MultitenancyTestHelper(Object[] tenantIds) {
		this.tenantIds = tenantIds;
	}

	private void attachTo(SimpleSessionFactoryBuilder builder) {
		assumeTrue(
				// Until we adapt the dialect context ... (that is if we need to adapt it)
				// DialectContext.getDialect() instanceof H2Dialect,
				org.hibernate.dialect.H2Dialect.class.getName().equals( DatabaseContainer.configuration().dialect() ),
				"This test relies on multi-tenancy, which can currently only be set up with H2"
		);

		// Force our own schema management tool which creates the schema for all tenants.
		builder.onServiceRegistryBuilder( srb -> srb.addInitiator(
				new MultitenancyTestHelperSchemaManagementTool.Initiator( tenantIds ) ) );

		builder.setProperty( AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER,
				new H2LazyMultiTenantConnectionProvider( tenantIds ) );
		// any required backend-multi-tenancy property (e.g.:*.backend.multi_tenancy.strategy = discriminator)
		// should be set by the client test
	}

}
