/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm.multitenancy.impl;

import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.DelayedDropAction;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.ExtractionTool;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaMigrator;
import org.hibernate.tool.schema.spi.SchemaValidator;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.hibernate.testing.boot.JdbcConnectionAccessImpl;

class MultitenancyTestHelperSchemaManagementTool
		implements SchemaManagementTool, Service, ServiceRegistryAwareService {

	static class Initiator
			implements StandardServiceInitiator<SchemaManagementTool> {
		private final String[] tenantIds;

		public Initiator(String[] tenantIds) {
			this.tenantIds = tenantIds;
		}

		@Override
		public Class<SchemaManagementTool> getServiceInitiated() {
			return SchemaManagementTool.class;
		}

		@Override
		@SuppressWarnings("rawtypes") // Can't do better: Map is raw in the superclass
		public SchemaManagementTool initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
			return new MultitenancyTestHelperSchemaManagementTool( tenantIds );
		}
	}

	private final HibernateSchemaManagementTool toolDelegate = new HibernateSchemaManagementTool();
	private final String[] tenantIds;

	private GenerationTargetToDatabase[] generationTargets;

	private MultitenancyTestHelperSchemaManagementTool(String[] tenantIds) {
		this.tenantIds = tenantIds;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		toolDelegate.injectServices( serviceRegistry );
		this.generationTargets = createSchemaTargets( serviceRegistry );
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

	@Override
	@SuppressWarnings("rawtypes") // Can't do better: Map is raw in the superclass
	public SchemaCreator getSchemaCreator(Map options) {
		return new SchemaCreator() {
			final SchemaCreatorImpl delegate = (SchemaCreatorImpl) toolDelegate.getSchemaCreator( options );
			@Override
			public void doCreation(Metadata metadata, ExecutionOptions executionOptions,
					ContributableMatcher contributableMatcher, SourceDescriptor sourceDescriptor,
					TargetDescriptor targetDescriptor) {
				delegate.doCreation( metadata, true, generationTargets );
			}
		};
	}

	@Override
	@SuppressWarnings("rawtypes") // Can't do better: Map is raw in the superclass
	public SchemaDropper getSchemaDropper(Map options) {
		return new SchemaDropper() {
			final SchemaDropperImpl delegate = (SchemaDropperImpl) toolDelegate.getSchemaDropper( options );
			@Override
			public void doDrop(Metadata metadata, ExecutionOptions executionOptions,
					ContributableMatcher contributableMatcher, SourceDescriptor sourceDescriptor,
					TargetDescriptor targetDescriptor) {
				delegate.doDrop( metadata, true, generationTargets );
			}

			@Override
			public DelayedDropAction buildDelayedAction(Metadata metadata, ExecutionOptions executionOptions,
					ContributableMatcher contributableMatcher, SourceDescriptor sourceDescriptor) {
				return new DelayedDropAction() {
					@Override
					public void perform(ServiceRegistry serviceRegistry) {
						delegate.doDrop( metadata, true, generationTargets );
					}
				};
			}
		};
	}

	@Override
	@SuppressWarnings("rawtypes") // Can't do better: Map is raw in the superclass
	public SchemaMigrator getSchemaMigrator(Map options) {
		throw notSupported();
	}

	@Override
	@SuppressWarnings("rawtypes") // Can't do better: Map is raw in the superclass
	public SchemaValidator getSchemaValidator(Map options) {
		throw notSupported();
	}

	@Override
	public void setCustomDatabaseGenerationTarget(GenerationTarget generationTarget) {
		throw notSupported();
	}

	@Override
	public ExtractionTool getExtractionTool() {
		throw notSupported();
	}

	private UnsupportedOperationException notSupported() {
		return new UnsupportedOperationException(
				"This feature is not supported when simulating multi-tenancy with test helpers" );
	}
}
