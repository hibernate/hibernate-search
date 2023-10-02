/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertyChecker;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.common.spi.SearchIntegrationEnvironment;
import org.hibernate.search.engine.common.spi.SearchIntegrationFinalizer;
import org.hibernate.search.engine.common.spi.SearchIntegrationPartialBuildState;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.engine.tenancy.spi.TenancyMode;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendAccessor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendSetupStrategy;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.common.bean.ForbiddenBeanProvider;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapping;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingImpl;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingInitiator;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingKey;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.extension.AbstractScopeTrackingExtension;
import org.hibernate.search.util.impl.test.extension.ExtensionScope;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

public class SearchSetupHelper extends AbstractScopeTrackingExtension implements TestExecutionExceptionHandler {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final TestConfigurationProvider configurationProvider = new TestConfigurationProvider();
	private TckBackendSetupStrategy<?> setupStrategy;
	private TckBackendAccessor backendAccessor;
	private final Map<ExtensionScope, Context> scopeContexts = new EnumMap<>( ExtensionScope.class );

	public static SearchSetupHelper create() {
		return new SearchSetupHelper();
	}

	private SearchSetupHelper() {
	}

	public SetupContext start() {
		return start( null, TckBackendHelper::createDefaultBackendSetupStrategy );
	}

	public SetupContext start(Function<TckBackendHelper, TckBackendSetupStrategy<?>> setupStrategyFunction) {
		return start( null, setupStrategyFunction );
	}

	public SetupContext start(String backendName) {
		return start( backendName, TckBackendHelper::createDefaultBackendSetupStrategy );
	}

	public SetupContext start(String backendName,
			Function<TckBackendHelper, TckBackendSetupStrategy<?>> setupStrategyFunction) {
		this.setupStrategy = setupStrategyFunction.apply( TckConfiguration.get().getBackendHelper() );

		Map<String, ?> backendRelativeProperties = setupStrategy.createBackendConfigurationProperties( configurationProvider );
		String backendPrefix = backendName == null
				? EngineSettings.BACKEND + "."
				: EngineSettings.BACKENDS + "." + backendName + ".";
		Map<String, Object> properties = new LinkedHashMap<>();
		for ( Map.Entry<String, ?> entry : backendRelativeProperties.entrySet() ) {
			properties.put( backendPrefix + entry.getKey(), entry.getValue() );
		}

		AllAwareConfigurationPropertySource propertySource = AllAwareConfigurationPropertySource.fromMap( properties );

		SetupContext setupContext = new SetupContext( propertySource );

		return setupStrategy.startSetup( setupContext );
	}

	public TckBackendAccessor getBackendAccessor() {
		if ( backendAccessor == null ) {
			if ( setupStrategy == null ) {
				setupStrategy = TckConfiguration.get().getBackendHelper().createDefaultBackendSetupStrategy();
			}
			backendAccessor = setupStrategy.createBackendAccessor( configurationProvider );
		}
		return backendAccessor;
	}

	private Context currentContext() {
		return scopeContexts.get( currentScope() );
	}

	@Override
	protected void actualAfterAll(ExtensionContext context) throws Exception {
		configurationProvider.afterAll( context );
		if ( !runningInNestedContext( context ) ) {
			cleanUp();
		}
	}

	@Override
	protected void actualAfterEach(ExtensionContext context) throws Exception {
		configurationProvider.afterEach( context );
		cleanUp();
	}

	@Override
	protected void actualBeforeAll(ExtensionContext context) {
		if ( !runningInNestedContext( context ) ) {
			configurationProvider.beforeAll( context );
			scopeContexts.put( ExtensionScope.CLASS, new Context() );
		}
	}

	@Override
	protected void actualBeforeEach(ExtensionContext context) {
		configurationProvider.beforeEach( context );
		scopeContexts.put( ExtensionScope.TEST, new Context() );
	}

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
		// When used as a "ClassExtension", exceptions are not properly reported by JUnit.
		// Log them so that we have something in the logs, at least.
		log.warn(
				"Exception thrown by test and caught by SearchSetupHelper rule: " + throwable.getMessage(),
				throwable
		);
		throw throwable;
	}

	public void cleanUp() throws IOException {
		try ( Closer<IOException> closer = new Closer<>() ) {
			cleanUp( closer );
		}
	}

	private void cleanUp(Closer<IOException> closer) {
		Context context = currentContext();
		closer.pushAll( StubMappingImpl::close, context.mappings );
		context.mappings.clear();
		closer.pushAll( SearchIntegrationPartialBuildState::closeOnFailure, context.integrationPartialBuildStates );
		context.integrationPartialBuildStates.clear();
		closer.pushAll( SearchIntegrationEnvironment::close, context.environments );
		context.environments.clear();
		closer.push( TckBackendAccessor::close, backendAccessor );
		backendAccessor = null;
	}

	public class SetupContext {

		private final ConfigurationPropertyChecker unusedPropertyChecker;
		private final ConfigurationPropertySource propertySource;
		// Use a LinkedHashMap for deterministic iteration
		private final Map<String, Object> overriddenProperties = new LinkedHashMap<>();

		// Disable the bean provider by default,
		// so that we detect code that relies on beans from the bean provider
		// whereas it should rely on reflection or built-in beans.
		private BeanProvider beanProvider = new ForbiddenBeanProvider();

		private final List<StubMappedIndex> mappedIndexes = new ArrayList<>();
		private TenancyMode tenancyMode = TenancyMode.SINGLE_TENANCY;
		private StubMappingSchemaManagementStrategy schemaManagementStrategy =
				StubMappingSchemaManagementStrategy.DROP_AND_CREATE_AND_DROP;

		SetupContext(AllAwareConfigurationPropertySource basePropertySource) {
			this.unusedPropertyChecker = ConfigurationPropertyChecker.create();
			this.propertySource = unusedPropertyChecker.wrap( basePropertySource )
					.withOverride( unusedPropertyChecker.wrap(
							AllAwareConfigurationPropertySource.fromMap( overriddenProperties ) ) );
		}

		public SetupContext expectCustomBeans() {
			beanProvider = null;
			return this;
		}

		public SetupContext withProperty(String key, Object value) {
			overriddenProperties.put( key, value );
			return this;
		}

		public SetupContext withPropertyRadical(String keyRadical, Object value) {
			overriddenProperties.put( EngineSettings.PREFIX + keyRadical, value );
			return this;
		}

		public SetupContext withBackendProperty(String keyRadical, Object value) {
			return withBackendProperty( null, keyRadical, value );
		}

		public SetupContext withBackendProperty(String backendName, String keyRadical, Object value) {
			if ( backendName == null ) {
				return withProperty( EngineSettings.BACKEND + "." + keyRadical, value );
			}
			return withProperty( EngineSettings.BACKENDS + "." + backendName + "." + keyRadical, value );
		}

		public SetupContext withIndexProperty(String indexName, String keyRadical, Object value) {
			return withIndexProperty( null, indexName, keyRadical, value );
		}

		public SetupContext withIndexProperty(String backendName, String indexName, String keyRadical, Object value) {
			return withBackendProperty( backendName,
					BackendSettings.INDEXES + "." + indexName + "." + keyRadical, value
			);
		}

		public SetupContext withIndexes(StubMappedIndex... mappedIndexes) {
			return withIndexes( Arrays.asList( mappedIndexes ) );
		}

		public SetupContext withIndexes(Collection<? extends StubMappedIndex> mappedIndexes) {
			mappedIndexes.forEach( this::withIndex );
			return this;
		}

		public SetupContext withIndex(StubMappedIndex mappedIndex) {
			mappedIndexes.add( mappedIndex );
			return this;
		}

		public SetupContext withMultiTenancy() {
			tenancyMode = TenancyMode.MULTI_TENANCY;
			return this;
		}

		public SetupContext withSchemaManagement(StubMappingSchemaManagementStrategy schemaManagementStrategy) {
			this.schemaManagementStrategy = schemaManagementStrategy;
			return this;
		}

		public PartialSetup setupFirstPhaseOnly() {
			return setupFirstPhaseOnly( Optional.empty() );
		}

		@SuppressWarnings("resource") // For the eclipse-compiler: complains on StubMappingImpl not bing closed
		public PartialSetup setupFirstPhaseOnly(Optional<StubMapping> previousMapping) {
			Context context = currentContext();
			SearchIntegrationEnvironment environment =
					SearchIntegrationEnvironment.builder( propertySource, unusedPropertyChecker )
							.beanProvider( beanProvider )
							.build();
			context.environments.add( environment );

			SearchIntegration.Builder integrationBuilder = ( previousMapping.isPresent() )
					? previousMapping.get().integration().restartBuilder( environment )
					: SearchIntegration.builder( environment );

			StubMappingInitiator initiator = new StubMappingInitiator( TckConfiguration.get().getBackendFeatures(),
					tenancyMode );
			mappedIndexes.forEach( initiator::add );
			StubMappingKey mappingKey = new StubMappingKey();
			integrationBuilder.addMappingInitiator( mappingKey, initiator );

			SearchIntegrationPartialBuildState integrationPartialBuildState = integrationBuilder.prepareBuild();
			context.integrationPartialBuildStates.add( integrationPartialBuildState );

			return overrides -> {
				SearchIntegrationFinalizer finalizer =
						integrationPartialBuildState.finalizer( propertySource.withOverride( overrides ),
								unusedPropertyChecker );
				StubMappingImpl mapping = finalizer.finalizeMapping(
						mappingKey,
						(ctx, partialMapping) -> partialMapping.finalizeMapping( schemaManagementStrategy )
				);
				context.mappings.add( mapping );

				finalizer.finalizeIntegration();
				context.integrationPartialBuildStates.remove( integrationPartialBuildState );

				return mapping;
			};
		}

		public StubMapping setup() {

			return setupFirstPhaseOnly().doSecondPhase();
		}

		public StubMapping setup(StubMapping previousMapping) {
			return setupFirstPhaseOnly( Optional.of( previousMapping ) ).doSecondPhase();
		}

	}

	public interface PartialSetup {

		default StubMapping doSecondPhase() {
			return doSecondPhase( ConfigurationPropertySource.empty() );
		}

		StubMapping doSecondPhase(ConfigurationPropertySource overrides);

	}

	private static boolean runningInNestedContext(ExtensionContext context) {
		// if we are running @Nested tests all of the Before/After are executed for each nested test
		// as well as for the nested class itself, leading to BeforeAll/AfterAll extensions being called more than needed.
		return context.getRequiredTestClass().isMemberClass();
	}

	private static class Context {
		private final List<SearchIntegrationEnvironment> environments = new ArrayList<>();
		private final List<SearchIntegrationPartialBuildState> integrationPartialBuildStates = new ArrayList<>();
		private final List<StubMappingImpl> mappings = new ArrayList<>();
	}
}
