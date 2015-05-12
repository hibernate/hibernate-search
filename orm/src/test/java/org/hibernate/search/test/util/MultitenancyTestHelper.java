/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.hibernate.tool.schema.internal.TargetDatabaseImpl;
import org.hibernate.tool.schema.spi.SchemaManagementTool;

/**
 * Utility to help setting up a test SessionFactory which uses multi-tenancy based
 * on multiple databases.
 *
 * @author Sanne Grinovero
 * @since 5.4
 */
public class MultitenancyTestHelper {

	private final Set<String> tenantIds;
	private final boolean multitenancyEnabled;

	private AbstractMultiTenantConnectionProvider multiTenantConnectionProvider;
	private Map<String,DriverManagerConnectionProviderImpl> tenantSpecificConnectionProviders = new HashMap<>();

	public MultitenancyTestHelper(Set<String> tenantIds) {
		this.tenantIds = tenantIds;
		this.multitenancyEnabled = tenantIds != null && tenantIds.size() != 0;
	}

	public void enableIfNeeded(StandardServiceRegistryBuilder registryBuilder) {
		registryBuilder.addService( MultiTenantConnectionProvider.class, multiTenantConnectionProvider );
	}

	public void start() {
		if ( multitenancyEnabled ) {
			multiTenantConnectionProvider = buildMultiTenantConnectionProvider();
		}
	}

	private AbstractMultiTenantConnectionProvider buildMultiTenantConnectionProvider() {
		for ( String tenantId : tenantIds ) {
			DriverManagerConnectionProviderImpl connectionProvider = ConnectionProviderBuilder.buildConnectionProvider( tenantId );
			tenantSpecificConnectionProviders.put( tenantId, connectionProvider );
		}
		return new AbstractMultiTenantConnectionProvider() {
			@Override
			protected ConnectionProvider getAnyConnectionProvider() {
				//blatantly assuming there's at least one entry:
				return tenantSpecificConnectionProviders.entrySet().iterator().next().getValue();
			}

			@Override
			protected ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
				DriverManagerConnectionProviderImpl connectionProviderImpl = tenantSpecificConnectionProviders.get( tenantIdentifier );
				if ( connectionProviderImpl == null ) {
					throw new HibernateException( "Unknown tenant identifier" );
				}
				return connectionProviderImpl;
			}
		};
	}

	public void close() {
		for ( DriverManagerConnectionProviderImpl connectionProvider : tenantSpecificConnectionProviders.values() ) {
			connectionProvider.stop();
		}
	}

	public void exportSchema(StandardServiceRegistry serviceRegistry, Metadata metadata, Map<String, Object> settings) {
		final TargetDatabaseImpl[] targets = createDatabaseTargets();

		serviceRegistry.getService( SchemaManagementTool.class ).getSchemaDropper( settings ).doDrop(
				metadata,
				true,
				targets
		);

		serviceRegistry.getService( SchemaManagementTool.class ).getSchemaCreator( settings ).doCreation(
				metadata,
				true,
				targets
		);
	}

	private TargetDatabaseImpl[] createDatabaseTargets() {
		int tenantsNumber = tenantIds.size();
		TargetDatabaseImpl[] targets = new TargetDatabaseImpl[tenantsNumber];
		Iterator<String> iterator = tenantIds.iterator();
		for ( int i = 0; i < tenantsNumber; i++ ) {
			targets[i] = createDatabaseTarget( iterator.next() );
		}
		return targets;
	}

	private TargetDatabaseImpl createDatabaseTarget(String tenantId) {
		DriverManagerConnectionProviderImpl connectionProviderImpl = tenantSpecificConnectionProviders.get( tenantId );
		return new TargetDatabaseImpl( new JdbcConnectionAccessImpl( connectionProviderImpl ) );
	}

	public void forceConfigurationSettings(Map<String, Object> settings) {
		if ( multitenancyEnabled ) {
			settings.remove( org.hibernate.cfg.Environment.HBM2DDL_AUTO );
			settings.put( AvailableSettings.MULTI_TENANT, MultiTenancyStrategy.DATABASE.name() );
		}
	}

}
