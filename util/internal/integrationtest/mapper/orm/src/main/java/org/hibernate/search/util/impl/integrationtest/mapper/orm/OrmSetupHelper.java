/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Consumer;

import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendSetupStrategy;
import org.hibernate.search.util.impl.integrationtest.common.extension.MappingSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.BackendMappingHandle;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.multitenancy.impl.MultitenancyTestHelper;

import org.junit.jupiter.api.extension.ExtensionContext;

public class OrmSetupHelper
		extends
		MappingSetupHelper<OrmSetupHelper.SetupContext,
				SimpleSessionFactoryBuilder,
				SimpleSessionFactoryBuilder,
				SessionFactory,
				OrmSetupHelper.SetupVariant> {

	private static final CoordinationStrategyExpectations DEFAULT_COORDINATION_STRATEGY_EXPECTATIONS;
	private static final Map<String, Object> DEFAULT_PROPERTIES;

	static {
		Map<String, Object> defaults = new LinkedHashMap<>();
		DatabaseContainer.configuration().add( defaults );

		// we don't need a ServiceLoader using a general-purpose aggregated class loader,
		// since we expect the service impl in the direct dependent test module.
		ServiceLoader<OrmSetupHelperConfig> serviceLoader = ServiceLoader.load( OrmSetupHelperConfig.class );
		Iterator<OrmSetupHelperConfig> iterator = serviceLoader.iterator();
		if ( iterator.hasNext() ) {
			OrmSetupHelperConfig next = iterator.next();
			DEFAULT_COORDINATION_STRATEGY_EXPECTATIONS = next.coordinationStrategyExpectations();
			next.overrideHibernateSearchDefaults( defaults::put );
		}
		else {
			DEFAULT_COORDINATION_STRATEGY_EXPECTATIONS = CoordinationStrategyExpectations.defaults();
		}
		DEFAULT_PROPERTIES = Collections.unmodifiableMap( defaults );
	}

	public static OrmSetupHelper withBackendMock(BackendMock backendMock) {
		return create( backendMock, DEFAULT_COORDINATION_STRATEGY_EXPECTATIONS );
	}

	public static OrmSetupHelper withBackendMocks(BackendMock defaultBackendMock, Map<String, BackendMock> namedBackendMocks) {
		return create( defaultBackendMock, namedBackendMocks, DEFAULT_COORDINATION_STRATEGY_EXPECTATIONS );
	}

	public static OrmSetupHelper withSingleBackend(BackendConfiguration backendConfiguration) {
		return create( backendConfiguration, DEFAULT_COORDINATION_STRATEGY_EXPECTATIONS );
	}

	public static OrmSetupHelper withMultipleBackends(BackendConfiguration defaultBackendConfiguration,
			Map<String, BackendConfiguration> namedBackendConfigurations) {
		return create( defaultBackendConfiguration, namedBackendConfigurations, DEFAULT_COORDINATION_STRATEGY_EXPECTATIONS );
	}

	private static OrmSetupHelper create(BackendMock backendMock,
			CoordinationStrategyExpectations coordinationStrategyExpectations) {
		return new OrmSetupHelper(
				BackendSetupStrategy.withSingleBackendMock( backendMock ),
				Collections.singleton( backendMock ),
				// Mock backend => avoid schema management unless we want to test it
				SchemaManagementStrategyName.NONE,
				coordinationStrategyExpectations
		);
	}

	private static OrmSetupHelper create(BackendMock defaultBackendMock, Map<String, BackendMock> namedBackendMocks,
			CoordinationStrategyExpectations coordinationStrategyExpectations) {
		List<BackendMock> backendMocks = new ArrayList<>();
		if ( defaultBackendMock != null ) {
			backendMocks.add( defaultBackendMock );
		}
		backendMocks.addAll( namedBackendMocks.values() );
		return new OrmSetupHelper(
				BackendSetupStrategy.withMultipleBackendMocks( defaultBackendMock, namedBackendMocks ),
				backendMocks,
				// Mock backend => avoid schema management unless we want to test it
				SchemaManagementStrategyName.NONE,
				coordinationStrategyExpectations
		);
	}

	private static OrmSetupHelper create(BackendConfiguration backendConfiguration,
			CoordinationStrategyExpectations coordinationStrategyExpectations) {
		return new OrmSetupHelper(
				BackendSetupStrategy.withSingleBackend( backendConfiguration ),
				Collections.emptyList(),
				// Real backend => ensure we clean up everything before and after the tests
				SchemaManagementStrategyName.DROP_AND_CREATE_AND_DROP,
				coordinationStrategyExpectations
		);
	}

	private static OrmSetupHelper create(BackendConfiguration defaultBackendConfiguration,
			Map<String, BackendConfiguration> namedBackendConfigurations,
			CoordinationStrategyExpectations coordinationStrategyExpectations) {
		return new OrmSetupHelper(
				BackendSetupStrategy.withMultipleBackends( defaultBackendConfiguration, namedBackendConfigurations ),
				Collections.emptyList(),
				// Real backend => ensure to clean up everything
				SchemaManagementStrategyName.DROP_AND_CREATE_AND_DROP,
				coordinationStrategyExpectations
		);
	}

	private final SchemaManagementStrategyName schemaManagementStrategyName;
	private final OrmAssertionHelper assertionHelper;
	private final List<OrmSetupHelperCleaner> ormSetupHelperCleaners = new ArrayList<>();
	private final CoordinationStrategyExpectations coordinationStrategyExpectations;

	private OrmSetupHelper(BackendSetupStrategy backendSetupStrategy, Collection<BackendMock> backendMocks,
			SchemaManagementStrategyName schemaManagementStrategyName,
			CoordinationStrategyExpectations coordinationStrategyExpectations) {
		super( backendSetupStrategy );
		this.schemaManagementStrategyName = schemaManagementStrategyName;
		this.coordinationStrategyExpectations = coordinationStrategyExpectations;
		this.assertionHelper = new OrmAssertionHelper( backendSetupStrategy );
		for ( BackendMock backendMock : backendMocks ) {
			backendMock.indexingWorkExpectations( coordinationStrategyExpectations.indexingWorkExpectations );
		}
	}

	public static CoordinationStrategyHolder withCoordinationStrategy(
			CoordinationStrategyExpectations coordinationStrategyExpectations) {
		return new CoordinationStrategyHolder( coordinationStrategyExpectations );
	}

	@Override
	public OrmAssertionHelper assertions() {
		return assertionHelper;
	}

	@Override
	protected SetupVariant defaultSetupVariant() {
		return SetupVariant.variant();
	}

	@Override
	protected SetupContext createSetupContext(SetupVariant setupVariant) {
		return new SetupContext( schemaManagementStrategyName );
	}

	public boolean areEntitiesProcessedInSession() {
		return coordinationStrategyExpectations.sync;
	}

	@Override
	protected void close(SessionFactory toClose) {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( SessionFactory::close, toClose );
			// if we are closing the session factory, we don't want to keep its cleaner around, so we remove it from the list:
			ormSetupHelperCleaners.removeIf( cleaner -> cleaner.usesExactly( toClose ) );
		}
	}

	@Override
	public void actualBeforeEach(ExtensionContext context) {
		super.actualBeforeEach( context );
		// if test was aborted then we don't want to clean the data since the test wasn't executed.
		if ( !context.getExecutionException().map( Object::getClass )
				.map( org.opentest4j.TestAbortedException.class::equals ).orElse( Boolean.FALSE ) ) {

			try ( Closer<RuntimeException> closer = new Closer<>() ) {
				closer.pushAll( OrmSetupHelperCleaner::cleanupData, ormSetupHelperCleaners );
			}
		}
	}

	public static class SetupVariant {
		private static final SetupVariant INSTANCE = new SetupVariant();

		public static SetupVariant variant() {
			return INSTANCE;
		}

		protected SetupVariant() {
		}
	}

	public final class SetupContext
			extends
			MappingSetupHelper<SetupContext,
					SimpleSessionFactoryBuilder,
					SimpleSessionFactoryBuilder,
					SessionFactory,
					SetupVariant>.AbstractSetupContext {

		// Use a LinkedHashMap for deterministic iteration
		private final Map<String, Object> overriddenProperties = new LinkedHashMap<>();
		private final List<Consumer<DataClearConfig>> dataCleanerConfigurers = new ArrayList<>();

		SetupContext(SchemaManagementStrategyName schemaManagementStrategyName) {
			// Set the default properties according to OrmSetupHelperConfig
			withProperties( DEFAULT_PROPERTIES );
			// Override the schema management strategy according to our needs for testing
			withProperty( HibernateOrmMapperSettings.SCHEMA_MANAGEMENT_STRATEGY, schemaManagementStrategyName );
			// Set the coordination strategy according to the expectations
			withProperty( "hibernate.search.coordination.strategy", coordinationStrategyExpectations.strategyName );
			// Ensure we don't build Jandex indexes needlessly:
			// discovery based on Jandex ought to be tested in real projects that don't use this setup helper.
			withProperty( HibernateOrmMapperSettings.MAPPING_BUILD_MISSING_DISCOVERED_JANDEX_INDEXES, false );
			// Ensure overridden properties will be applied
			withConfiguration( builder -> overriddenProperties.forEach( builder::setProperty ) );
		}

		public SetupContext withTcclLookupPrecedenceBefore() {
			withConfiguration( builder -> builder.setTcclLookupPrecedenceBefore() );
			return thisAsC();
		}

		@Override
		public SetupContext withProperty(String key, Object value) {
			overriddenProperties.put( key, value );
			return thisAsC();
		}

		public SetupContext tenantsWithHelperEnabled(Object... tenants) {
			return tenants( true, tenants );
		}

		public SetupContext tenants(boolean enableMultitenancyHelper, Object... tenants) {
			if ( enableMultitenancyHelper ) {
				withConfiguration( b -> MultitenancyTestHelper.enable( b, tenants ) );
			}
			if ( coordinationStrategyExpectations.requiresTenantIds ) {
				withProperty( HibernateOrmMapperSettings.MULTI_TENANCY_TENANT_IDS, Arrays.asList( tenants ) );
			}
			dataClearing( config -> config.tenants( tenants ) );
			return thisAsC();
		}

		public SetupContext skipTestForDialect(Class<? extends Dialect> dialect, String reason) {
			withConfiguration( b -> b.onMetadata( metadataImplementor -> {
				Dialect currentDialect = metadataImplementor.getDatabase().getDialect();
				assumeFalse(
						dialect.isAssignableFrom( currentDialect.getClass() ),
						"Skipping test for dialect " + dialect.getName() + "; reason: " + reason
				);
			} ) );
			return thisAsC();
		}

		public SetupContext withAnnotatedTypes(Class<?>... annotatedTypes) {
			return withConfiguration( builder -> builder.addAnnotatedClasses( Arrays.asList( annotatedTypes ) ) );
		}

		public SetupContext dataClearing(Consumer<DataClearConfig> configurer) {
			return dataClearing( false, configurer );
		}

		public SetupContext dataClearing(boolean reset, Consumer<DataClearConfig> configurer) {
			if ( reset ) {
				dataCleanerConfigurers.clear();
			}
			dataCleanerConfigurers.add( configurer );

			return thisAsC();
		}

		public SetupContext dataClearingIndexOnly() {
			return dataClearing( false, config -> config.clearDatabaseData( false ).clearIndexData( true ) );
		}

		public SessionFactory setup(Class<?>... annotatedTypes) {
			return withAnnotatedTypes( annotatedTypes ).setup();
		}


		@Override
		protected SimpleSessionFactoryBuilder createBuilder() {
			return new SimpleSessionFactoryBuilder();
		}

		@Override
		protected void consumeBeforeBuildConfigurations(SimpleSessionFactoryBuilder builder,
				List<Consumer<SimpleSessionFactoryBuilder>> consumers) {
			consumers.forEach( c -> c.accept( builder ) );
		}

		@Override
		protected SessionFactory build(SimpleSessionFactoryBuilder builder) {
			SessionFactoryImplementor sessionFactory = builder.build().unwrap( SessionFactoryImplementor.class );

			OrmSetupHelperCleaner cleaner = OrmSetupHelperCleaner.create(
					sessionFactory, currentScope(), backendSetupStrategy.isMockBackend() );
			for ( Consumer<DataClearConfig> configurer : dataCleanerConfigurers ) {
				cleaner.appendConfiguration( configurer );
			}
			ormSetupHelperCleaners.add( cleaner );

			return sessionFactory;
		}

		@Override
		protected BackendMappingHandle toBackendMappingHandle(SessionFactory result) {
			return new HibernateOrmMappingHandle( result );
		}

		@Override
		protected SetupContext thisAsC() {
			return this;
		}

	}

	public static final class CoordinationStrategyHolder {
		private CoordinationStrategyHolder(CoordinationStrategyExpectations coordinationStrategyExpectations) {
			this.coordinationStrategyExpectations = coordinationStrategyExpectations;
		}

		private final CoordinationStrategyExpectations coordinationStrategyExpectations;

		public OrmSetupHelper withBackendMock(BackendMock backendMock) {
			return create( backendMock, coordinationStrategyExpectations );
		}

		public OrmSetupHelper withBackendMocks(BackendMock defaultBackendMock, Map<String, BackendMock> namedBackendMocks) {
			return create( defaultBackendMock, namedBackendMocks, coordinationStrategyExpectations );
		}

		public OrmSetupHelper withSingleBackend(BackendConfiguration backendConfiguration) {
			return create( backendConfiguration, coordinationStrategyExpectations );
		}

		public OrmSetupHelper withMultipleBackends(BackendConfiguration defaultBackendConfiguration,
				Map<String, BackendConfiguration> namedBackendConfigurations) {
			return create( defaultBackendConfiguration, namedBackendConfigurations, coordinationStrategyExpectations );
		}
	}

}
