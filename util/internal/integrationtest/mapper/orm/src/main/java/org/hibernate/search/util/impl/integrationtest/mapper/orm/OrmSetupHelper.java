/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class OrmSetupHelper
		extends
		MappingSetupHelper<OrmSetupHelper.SetupContext,
				SimpleSessionFactoryBuilder,
				SimpleSessionFactoryBuilder,
				SessionFactory,
				OrmSetupHelper.SetupVariant>
		implements AfterTestExecutionCallback {

	private static final CoordinationStrategyExpectations DEFAULT_COORDINATION_STRATEGY_EXPECTATIONS;
	private static final Map<String, Object> DEFAULT_PROPERTIES;

	static {
		// we don't need a ServiceLoader using a general-purpose aggregated class loader,
		// since we expect the service impl in the direct dependent test module.
		ServiceLoader<OrmSetupHelperConfig> serviceLoader = ServiceLoader.load( OrmSetupHelperConfig.class );
		Iterator<OrmSetupHelperConfig> iterator = serviceLoader.iterator();
		if ( iterator.hasNext() ) {
			OrmSetupHelperConfig next = iterator.next();
			DEFAULT_COORDINATION_STRATEGY_EXPECTATIONS = next.coordinationStrategyExpectations();
			Map<String, Object> defaults = new LinkedHashMap<>();
			next.overrideHibernateSearchDefaults( defaults::put );
			DEFAULT_PROPERTIES = Collections.unmodifiableMap( defaults );
		}
		else {
			DEFAULT_COORDINATION_STRATEGY_EXPECTATIONS = CoordinationStrategyExpectations.defaults();
			DEFAULT_PROPERTIES = Collections.emptyMap();
		}
	}

	public static OrmSetupHelper withBackendMock(BackendMock backendMock) {
		return new OrmSetupHelper(
				BackendSetupStrategy.withSingleBackendMock( backendMock ),
				Collections.singleton( backendMock ),
				// Mock backend => avoid schema management unless we want to test it
				SchemaManagementStrategyName.NONE
		);
	}

	public static OrmSetupHelper withBackendMocks(BackendMock defaultBackendMock, Map<String, BackendMock> namedBackendMocks) {
		List<BackendMock> backendMocks = new ArrayList<>();
		if ( defaultBackendMock != null ) {
			backendMocks.add( defaultBackendMock );
		}
		backendMocks.addAll( namedBackendMocks.values() );
		return new OrmSetupHelper(
				BackendSetupStrategy.withMultipleBackendMocks( defaultBackendMock, namedBackendMocks ),
				backendMocks,
				// Mock backend => avoid schema management unless we want to test it
				SchemaManagementStrategyName.NONE
		);
	}

	public static OrmSetupHelper withSingleBackend(BackendConfiguration backendConfiguration) {
		return new OrmSetupHelper(
				BackendSetupStrategy.withSingleBackend( backendConfiguration ),
				Collections.emptyList(),
				// Real backend => ensure we clean up everything before and after the tests
				SchemaManagementStrategyName.DROP_AND_CREATE_AND_DROP
		);
	}

	public static OrmSetupHelper withMultipleBackends(BackendConfiguration defaultBackendConfiguration,
			Map<String, BackendConfiguration> namedBackendConfigurations) {
		return new OrmSetupHelper(
				BackendSetupStrategy.withMultipleBackends( defaultBackendConfiguration, namedBackendConfigurations ),
				Collections.emptyList(),
				// Real backend => ensure to clean up everything
				SchemaManagementStrategyName.DROP_AND_CREATE_AND_DROP
		);
	}

	private final Collection<BackendMock> backendMocks;
	private final SchemaManagementStrategyName schemaManagementStrategyName;
	private final OrmAssertionHelper assertionHelper;
	private OrmSetupHelperCleaner ormSetupHelperCleaner;
	private CoordinationStrategyExpectations coordinationStrategyExpectations = DEFAULT_COORDINATION_STRATEGY_EXPECTATIONS;
	private SessionFactoryImplementor sessionFactory;

	protected OrmSetupHelper(BackendSetupStrategy backendSetupStrategy, Collection<BackendMock> backendMocks,
			SchemaManagementStrategyName schemaManagementStrategyName) {
		super( backendSetupStrategy );
		this.backendMocks = backendMocks;
		this.schemaManagementStrategyName = schemaManagementStrategyName;
		this.assertionHelper = new OrmAssertionHelper( backendSetupStrategy );
	}

	public OrmSetupHelper coordinationStrategy(CoordinationStrategyExpectations coordinationStrategyExpectations) {
		this.coordinationStrategyExpectations = coordinationStrategyExpectations;
		return this;
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
	protected void init() {
		for ( BackendMock backendMock : backendMocks ) {
			backendMock.indexingWorkExpectations( coordinationStrategyExpectations.indexingWorkExpectations );
		}
	}

	@Override
	protected void close(SessionFactory toClose) {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( SessionFactory::close, toClose );
		}
	}

	@Override
	public void afterTestExecution(ExtensionContext context) {
		// if test was aborted then we don't want to clean the data since the test wasn't executed.
		if ( !context.getExecutionException().map( Object::getClass )
				.map( org.opentest4j.TestAbortedException.class::equals ).orElse( Boolean.FALSE ) ) {
			this.ormSetupHelperCleaner.cleanupData( sessionFactory );
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

		SetupContext(SchemaManagementStrategyName schemaManagementStrategyName) {
			ormSetupHelperCleaner = OrmSetupHelperCleaner.create( callOncePerClass, backendSetupStrategy.isMockBackend() );
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

		public SetupContext tenants(String... tenants) {
			return tenants( true, tenants );
		}

		public SetupContext tenants(boolean enableMultitenancyHelper, String... tenants) {
			if ( enableMultitenancyHelper ) {
				withConfiguration( b -> MultitenancyTestHelper.enable( b, tenants ) );
			}
			if ( coordinationStrategyExpectations.requiresTenantIds ) {
				withProperty( HibernateOrmMapperSettings.MULTI_TENANCY_TENANT_IDS, String.join( ",", tenants ) );
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
				ormSetupHelperCleaner = OrmSetupHelperCleaner.create( callOncePerClass,
						backendSetupStrategy.isMockBackend()
				);
			}
			ormSetupHelperCleaner.appendConfiguration( configurer );
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
			sessionFactory = builder.build().unwrap( SessionFactoryImplementor.class );
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

}
