/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.backend.lucene.multitenancy.MultiTenancyStrategyName;
import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase;

import org.hibernate.testing.boot.JdbcConnectionAccessImpl;
import org.hibernate.testing.env.ConnectionProviderBuilder;

/**
 * Utility to help setting up a test SessionFactory which uses multi-tenancy based
 * on multiple databases.
 *
 * @author Sanne Grinovero
 * @since 5.4
 */
public class MultitenancyTestHelper implements Closeable {

	private final Set<String> tenantIds;
	private final boolean multitenancyEnabled;
	private final AbstractMultiTenantConnectionProvider multiTenantConnectionProvider;
	private final Map<String,DriverManagerConnectionProviderImpl> tenantSpecificConnectionProviders = new HashMap<>();

	public MultitenancyTestHelper(Set<String> tenantIds) {
		this.tenantIds = tenantIds;
		this.multitenancyEnabled = tenantIds != null && tenantIds.size() != 0;
		if ( multitenancyEnabled ) {
			multiTenantConnectionProvider = buildMultiTenantConnectionProvider();
		}
		else {
			multiTenantConnectionProvider = null;
		}
	}

	public void enableIfNeeded(StandardServiceRegistryBuilder registryBuilder) {
		if ( !multitenancyEnabled ) {
			return;
		}
		registryBuilder.addService( MultiTenantConnectionProvider.class, multiTenantConnectionProvider );
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

	@Override
	public void close() {
		for ( DriverManagerConnectionProviderImpl connectionProvider : tenantSpecificConnectionProviders.values() ) {
			connectionProvider.stop();
		}
	}

	public void exportSchema(MetadataImplementor metadata) {
		if ( !multitenancyEnabled ) {
			return;
		}
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
		GenerationTargetToDatabase[] targets = new GenerationTargetToDatabase[tenantSpecificConnectionProviders.size()];
		int index = 0;
		for ( Entry<String, DriverManagerConnectionProviderImpl> e : tenantSpecificConnectionProviders.entrySet() ) {
			ConnectionProvider connectionProvider = e.getValue();
			targets[index] = new GenerationTargetToDatabase(
						new DdlTransactionIsolatorTestingImpl( serviceRegistry,
								new JdbcConnectionAccessImpl( connectionProvider ) ) );
			index++;
		}
		return targets;
	}

	public void forceConfigurationSettings(Map<String, Object> settings) {
		if ( !multitenancyEnabled ) {
			return;
		}
		settings.remove( org.hibernate.cfg.Environment.HBM2DDL_AUTO );
		settings.put( AvailableSettings.MULTI_TENANT, MultiTenancyStrategy.DATABASE.name() );
		settings.put( BackendSettings.backendKey( LuceneBackendSettings.MULTI_TENANCY_STRATEGY ),
				MultiTenancyStrategyName.DISCRIMINATOR );
	}

}
