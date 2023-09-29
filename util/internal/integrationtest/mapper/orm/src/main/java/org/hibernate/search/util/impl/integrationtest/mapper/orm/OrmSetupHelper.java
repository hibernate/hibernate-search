/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import static org.junit.Assume.assumeTrue;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendSetupStrategy;
import org.hibernate.search.util.impl.integrationtest.common.extension.MappingSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.BackendMappingHandle;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.multitenancy.impl.MultitenancyTestHelper;

public final class OrmSetupHelper
		extends
		MappingSetupHelper<OrmSetupHelper.SetupContext,
				SimpleSessionFactoryBuilder,
				SimpleSessionFactoryBuilder,
				SessionFactory,
				OrmSetupHelper.SetupVariant> {

	private static final BiConsumer<Boolean, String> JUPITER_ASSUMPTION_CHECK = org.junit.jupiter.api.Assumptions::assumeTrue;
	private static final BiConsumer<Boolean, String> VINTAGE_ASSUMPTION_CHECK = (b, s) -> assumeTrue( s, b );
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
		return withBackendMock( backendMock, JUPITER_ASSUMPTION_CHECK );
	}

	public static OrmSetupHelper withBackendMocks(BackendMock defaultBackendMock,
			Map<String, BackendMock> namedBackendMocks) {
		return withBackendMocks( defaultBackendMock, namedBackendMocks, JUPITER_ASSUMPTION_CHECK );
	}

	public static OrmSetupHelper withSingleBackend(BackendConfiguration backendConfiguration) {
		return withSingleBackend( backendConfiguration, JUPITER_ASSUMPTION_CHECK );
	}

	public static OrmSetupHelper withMultipleBackends(BackendConfiguration defaultBackendConfiguration,
			Map<String, BackendConfiguration> namedBackendConfigurations) {
		return withMultipleBackends(
				defaultBackendConfiguration, namedBackendConfigurations,
				JUPITER_ASSUMPTION_CHECK );
	}

	public static OrmSetupHelper withBackendMockLegacy(BackendMock backendMock) {
		return withBackendMock( backendMock, VINTAGE_ASSUMPTION_CHECK );
	}

	public static OrmSetupHelper withBackendMocksLegacy(BackendMock defaultBackendMock,
			Map<String, BackendMock> namedBackendMocks) {
		return withBackendMocks( defaultBackendMock, namedBackendMocks, VINTAGE_ASSUMPTION_CHECK );
	}

	public static OrmSetupHelper withSingleBackendLegacy(BackendConfiguration backendConfiguration) {
		return withSingleBackend( backendConfiguration, VINTAGE_ASSUMPTION_CHECK );
	}

	public static OrmSetupHelper withMultipleBackendsGlobal(BackendConfiguration defaultBackendConfiguration,
			Map<String, BackendConfiguration> namedBackendConfigurations) {
		return withMultipleBackends(
				defaultBackendConfiguration, namedBackendConfigurations,
				VINTAGE_ASSUMPTION_CHECK );
	}

	private static OrmSetupHelper withBackendMock(BackendMock backendMock,
			BiConsumer<Boolean, String> assumptionTrueCheck) {
		return new OrmSetupHelper(
				BackendSetupStrategy.withSingleBackendMock( backendMock ),
				Collections.singleton( backendMock ),
				// Mock backend => avoid schema management unless we want to test it
				SchemaManagementStrategyName.NONE,
				assumptionTrueCheck
		);
	}

	private static OrmSetupHelper withBackendMocks(BackendMock defaultBackendMock,
			Map<String, BackendMock> namedBackendMocks, BiConsumer<Boolean, String> assumptionTrueCheck) {
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
				assumptionTrueCheck
		);
	}

	private static OrmSetupHelper withSingleBackend(BackendConfiguration backendConfiguration,
			BiConsumer<Boolean, String> assumptionTrueCheck) {
		return new OrmSetupHelper(
				BackendSetupStrategy.withSingleBackend( backendConfiguration ),
				Collections.emptyList(),
				// Real backend => ensure we clean up everything before and after the tests
				SchemaManagementStrategyName.DROP_AND_CREATE_AND_DROP,
				assumptionTrueCheck
		);
	}

	private static OrmSetupHelper withMultipleBackends(BackendConfiguration defaultBackendConfiguration,
			Map<String, BackendConfiguration> namedBackendConfigurations, BiConsumer<Boolean, String> assumptionTrueCheck) {
		return new OrmSetupHelper(
				BackendSetupStrategy.withMultipleBackends( defaultBackendConfiguration, namedBackendConfigurations ),
				Collections.emptyList(),
				// Real backend => ensure to clean up everything
				SchemaManagementStrategyName.DROP_AND_CREATE_AND_DROP,
				assumptionTrueCheck
		);
	}

	private final Collection<BackendMock> backendMocks;
	private final SchemaManagementStrategyName schemaManagementStrategyName;
	private final OrmAssertionHelper assertionHelper;
	private final BiConsumer<Boolean, String> assumptionTrueCheck;
	private CoordinationStrategyExpectations coordinationStrategyExpectations =
			DEFAULT_COORDINATION_STRATEGY_EXPECTATIONS;

	private OrmSetupHelper(BackendSetupStrategy backendSetupStrategy, Collection<BackendMock> backendMocks,
			SchemaManagementStrategyName schemaManagementStrategyName,
			BiConsumer<Boolean, String> assumptionTrueCheck) {
		super( backendSetupStrategy );
		this.backendMocks = backendMocks;
		this.schemaManagementStrategyName = schemaManagementStrategyName;
		this.assertionHelper = new OrmAssertionHelper( backendSetupStrategy );
		this.assumptionTrueCheck = assumptionTrueCheck;
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
				withConfiguration( b -> MultitenancyTestHelper.enable( b, assumptionTrueCheck, tenants ) );
			}
			if ( coordinationStrategyExpectations.requiresTenantIds ) {
				withProperty( HibernateOrmMapperSettings.MULTI_TENANCY_TENANT_IDS, String.join( ",", tenants ) );
			}
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
			return builder.build();
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
