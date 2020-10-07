/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm.multitenancy.impl;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.SimpleSessionFactoryBuilder;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase;

import org.hibernate.testing.boot.JdbcConnectionAccessImpl;

/**
 * Utility to help setting up a test SessionFactory which uses multi-tenancy based
 * on multiple databases.
 *
 * @author Sanne Grinovero
 * @since 5.4
 */
public class MultitenancyTestHelper {

	private final String[] tenantIds;

	public MultitenancyTestHelper(String[] tenantIds) {
		this.tenantIds = tenantIds;
	}

	public void enable(SimpleSessionFactoryBuilder builder) {
		builder.setProperty( org.hibernate.cfg.Environment.HBM2DDL_AUTO, org.hibernate.tool.schema.Action.NONE );
		builder.setProperty( AvailableSettings.MULTI_TENANT, MultiTenancyStrategy.DATABASE.name() );
		builder.setProperty( AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER,
				new H2LazyMultiTenantConnectionProvider( tenantIds ) );
		// any required backend-multi-tenancy property (e.g.:*.backend.multi_tenancy.strategy = discriminator)
		// should be set by the client test

		builder.onMetadata( this::exportSchema );
	}

	private void exportSchema(MetadataImplementor metadata) {
		ServiceRegistryImplementor serviceRegistry = (ServiceRegistryImplementor) metadata.getMetadataBuildingOptions()
				.getServiceRegistry();
		HibernateSchemaManagementTool tool = new HibernateSchemaManagementTool();
		tool.injectServices( serviceRegistry );
		final GenerationTargetToDatabase[] databaseTargets = createSchemaTargets( serviceRegistry );
		new SchemaDropperImpl( serviceRegistry ).doDrop(
				metadata,
				true,
				databaseTargets
		);
		new SchemaCreatorImpl( serviceRegistry ).doCreation(
				metadata,
				true,
				databaseTargets
		);
	}

	private GenerationTargetToDatabase[] createSchemaTargets(ServiceRegistryImplementor serviceRegistry) {
		H2LazyMultiTenantConnectionProvider multiTenantConnectionProvider = (H2LazyMultiTenantConnectionProvider)
				serviceRegistry.getService( MultiTenantConnectionProvider.class );
		GenerationTargetToDatabase[] targets = new GenerationTargetToDatabase[tenantIds.length];
		int index = 0;
		for ( String tenantId : tenantIds ) {
			ConnectionProvider connectionProvider = multiTenantConnectionProvider.selectConnectionProvider( tenantId );
			targets[index] = new GenerationTargetToDatabase(
						new DdlTransactionIsolatorTestingImpl( serviceRegistry,
								new JdbcConnectionAccessImpl( connectionProvider ) ) );
			index++;
		}
		return targets;
	}

}
