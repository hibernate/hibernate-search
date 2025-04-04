/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm.multitenancy.impl;

import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.internal.SchemaTruncatorImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.DelayedDropAction;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.ExtractionTool;
import org.hibernate.tool.schema.spi.GenerationTarget;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaMigrator;
import org.hibernate.tool.schema.spi.SchemaPopulator;
import org.hibernate.tool.schema.spi.SchemaTruncator;
import org.hibernate.tool.schema.spi.SchemaValidator;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.hibernate.testing.boot.JdbcConnectionAccessImpl;

class MultitenancyTestHelperSchemaManagementTool
		implements SchemaManagementTool, Service, ServiceRegistryAwareService {

	static class Initiator
			implements StandardServiceInitiator<SchemaManagementTool> {
		private final Object[] tenantIds;

		public Initiator(Object[] tenantIds) {
			this.tenantIds = tenantIds;
		}

		@Override
		public Class<SchemaManagementTool> getServiceInitiated() {
			return SchemaManagementTool.class;
		}

		@Override
		public SchemaManagementTool initiateService(Map<String, Object> configurationValues,
				ServiceRegistryImplementor registry) {
			return new MultitenancyTestHelperSchemaManagementTool( tenantIds );
		}
	}

	private final HibernateSchemaManagementTool toolDelegate = new HibernateSchemaManagementTool();
	private final Object[] tenantIds;

	private GenerationTargetToDatabase[] generationTargets;

	private MultitenancyTestHelperSchemaManagementTool(Object[] tenantIds) {
		this.tenantIds = tenantIds;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		toolDelegate.injectServices( serviceRegistry );
		this.generationTargets = createSchemaTargets( serviceRegistry );
	}

	private GenerationTargetToDatabase[] createSchemaTargets(ServiceRegistryImplementor serviceRegistry) {
		H2LazyMultiTenantConnectionProvider multiTenantConnectionProvider =
				(H2LazyMultiTenantConnectionProvider) serviceRegistry.getService( MultiTenantConnectionProvider.class );
		GenerationTargetToDatabase[] targets = new GenerationTargetToDatabase[tenantIds.length];
		int index = 0;
		for ( Object tenantId : tenantIds ) {
			ConnectionProvider connectionProvider = multiTenantConnectionProvider.selectConnectionProvider( tenantId );
			targets[index] = new GenerationTargetToDatabase(
					new DdlTransactionIsolatorTestingImpl( serviceRegistry,
							new JdbcConnectionAccessImpl( connectionProvider ) ) );
			index++;
		}
		return targets;
	}

	@Override
	public SchemaCreator getSchemaCreator(Map<String, Object> options) {
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
	public SchemaDropper getSchemaDropper(Map<String, Object> options) {
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
	public SchemaMigrator getSchemaMigrator(Map<String, Object> options) {
		throw notSupported();
	}

	@Override
	public SchemaValidator getSchemaValidator(Map<String, Object> options) {
		throw notSupported();
	}

	@Override
	public SchemaPopulator getSchemaPopulator(Map<String, Object> options) {
		return toolDelegate.getSchemaPopulator( options );
	}

	@Override
	public SchemaTruncator getSchemaTruncator(Map<String, Object> options) {
		return new SchemaTruncator() {
			final SchemaTruncatorImpl delegate = (SchemaTruncatorImpl) toolDelegate.getSchemaTruncator( options );

			@Override
			public void doTruncate(Metadata metadata, ExecutionOptions options,
					ContributableMatcher contributableInclusionFilter, TargetDescriptor targetDescriptor) {
				final StandardServiceRegistry serviceRegistry =
						( (MetadataImplementor) metadata ).getMetadataBuildingOptions().getServiceRegistry();

				delegate.doTruncate( metadata,
						new ExecutionOptions() {
							@Override
							public boolean shouldManageNamespaces() {
								return true;
							}

							@Override
							public Map<String, Object> getConfigurationValues() {
								return serviceRegistry.requireService( ConfigurationService.class ).getSettings();
							}

							@Override
							public ExceptionHandler getExceptionHandler() {
								return ExceptionHandlerLoggedImpl.INSTANCE;
							}
						},
						(contributed) -> true,
						serviceRegistry.requireService( JdbcEnvironment.class ).getDialect(),
						generationTargets );
			}
		};
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
