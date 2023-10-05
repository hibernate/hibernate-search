/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm.multitenancy.impl;

import static org.junit.Assume.assumeTrue;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.SimpleSessionFactoryBuilder;

import org.hibernate.testing.orm.junit.DialectContext;

/**
 * Utility to help setting up a test SessionFactory which uses multi-tenancy based
 * on multiple databases.
 *
 * @author Sanne Grinovero
 * @since 5.4
 */
public class MultitenancyTestHelper {

	public static void enable(SimpleSessionFactoryBuilder builder, String... tenantIds) {
		MultitenancyTestHelper helper = new MultitenancyTestHelper( tenantIds );
		helper.attachTo( builder );
	}

	private final String[] tenantIds;

	private MultitenancyTestHelper(String[] tenantIds) {
		this.tenantIds = tenantIds;
	}

	private void attachTo(SimpleSessionFactoryBuilder builder) {
		assumeTrue( "This test relies on multi-tenancy, which can currently only be set up with H2",
				DialectContext.getDialect() instanceof H2Dialect );

		// Force our own schema management tool which creates the schema for all tenants.
		builder.onServiceRegistryBuilder( srb -> srb.addInitiator(
				new MultitenancyTestHelperSchemaManagementTool.Initiator( tenantIds ) ) );

		builder.setProperty( AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER,
				new H2LazyMultiTenantConnectionProvider( tenantIds ) );
		// any required backend-multi-tenancy property (e.g.:*.backend.multi_tenancy.strategy = discriminator)
		// should be set by the client test
	}

}
