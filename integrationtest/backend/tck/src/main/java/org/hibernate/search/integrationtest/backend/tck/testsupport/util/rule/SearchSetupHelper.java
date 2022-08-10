/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class SearchSetupHelper implements TestRule {

	private final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final TestConfigurationProvider configurationProvider;
	private final TckBackendSetupStrategy<?> setupStrategy;
	private final TestRule delegateRule;

	private final List<SearchIntegrationEnvironment> environments = new ArrayList<>();
	private final List<SearchIntegrationPartialBuildState> integrationPartialBuildStates = new ArrayList<>();
	private final List<StubMappingImpl> mappings = new ArrayList<>();
	private TckBackendAccessor backendAccessor;

	public SearchSetupHelper() {
		this( TckBackendHelper::createDefaultBackendSetupStrategy );
	}

	public SearchSetupHelper(Function<TckBackendHelper, TckBackendSetupStrategy<?>> setupStrategyFunction) {
		this.configurationProvider = new TestConfigurationProvider();
		this.setupStrategy = setupStrategyFunction.apply( TckConfiguration.get().getBackendHelper() );
		Optional<TestRule> setupStrategyTestRule = setupStrategy.testRule();
		this.delegateRule = setupStrategyTestRule
				.<TestRule>map( rule -> RuleChain.outerRule( configurationProvider ).around( rule ) )
				.orElse( configurationProvider );
	}

	public SetupContext start() {
		return start( null );
	}

	public SetupContext start(String backendName) {
		Map<String, ?> backendRelativeProperties =
				setupStrategy.createBackendConfigurationProperties( configurationProvider );
		String backendPrefix = backendName == null ? EngineSettings.BACKEND + "."
				: EngineSettings.BACKENDS + "." + backendName + ".";
		Map<String, Object> properties = new LinkedHashMap<>();
		for ( Map.Entry<String, ?> entry : backendRelativeProperties.entrySet() ) {
			properties.put( backendPrefix + entry.getKey(), entry.getValue() );
		}

		AllAwareConfigurationPropertySource propertySource = AllAwareConfigurationPropertySource.fromMap( properties );

		SetupContext setupContext = new SetupContext( backendName, propertySource );

		return setupStrategy.startSetup( setupContext );
	}

	public TckBackendAccessor getBackendAccessor() {
		if ( backendAccessor == null ) {
			backendAccessor = setupStrategy.createBackendAccessor( configurationProvider );
		}
		return backendAccessor;
	}

	@Override
	public Statement apply(Statement base, Description description) {
		return statement( base, description );
	}

	private Statement statement(final Statement base, final Description description) {
		Statement wrapped = new Statement() {
			@Override
			public void evaluate() throws Throwable {
				// Using the closer like this allows to suppress exceptions thrown by the 'finally' block.
				try ( Closer<IOException> closer = new Closer<>() ) {
					try {
						base.evaluate();
					}
					catch (RuntimeException e) {
						// When used as a @ClassRule, exceptions are not properly reported by JUnit.
						// Log them so that we have something in the logs, at least.
						log.warn( "Exception thrown by test and caught by SearchSetupHelper rule: " + e.getMessage(), e );
						throw e;
					}
					finally {
						cleanUp( closer );
					}
				}
			}
		};
		return delegateRule.apply( wrapped, description );
	}

	public void cleanUp() throws IOException {
		try ( Closer<IOException> closer = new Closer<>() ) {
			cleanUp( closer );
		}
	}

	private void cleanUp(Closer<IOException> closer) {
		closer.pushAll( StubMappingImpl::close, mappings );
		mappings.clear();
		closer.pushAll( SearchIntegrationPartialBuildState::closeOnFailure, integrationPartialBuildStates );
		integrationPartialBuildStates.clear();
		closer.pushAll( SearchIntegrationEnvironment::close, environments );
		environments.clear();
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
		private StubMappingSchemaManagementStrategy schemaManagementStrategy = StubMappingSchemaManagementStrategy.DROP_AND_CREATE_AND_DROP;

		SetupContext(String defaultBackendName, AllAwareConfigurationPropertySource basePropertySource) {
			this.unusedPropertyChecker = ConfigurationPropertyChecker.create();
			this.propertySource = unusedPropertyChecker.wrap( basePropertySource )
					.withOverride( unusedPropertyChecker.wrap( AllAwareConfigurationPropertySource.fromMap( overriddenProperties ) ) );
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
					BackendSettings.INDEXES + "." + indexName + "." + keyRadical, value );
		}

		public SetupContext withIndexes(StubMappedIndex ... mappedIndexes) {
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

		public PartialSetup setupFirstPhaseOnly(Optional<StubMapping> previousMapping) {
			SearchIntegrationEnvironment environment =
					SearchIntegrationEnvironment.builder( propertySource, unusedPropertyChecker )
							.beanProvider( beanProvider )
							.build();
			environments.add( environment );

			SearchIntegration.Builder integrationBuilder = (previousMapping.isPresent()) ?
					previousMapping.get().integration().restartBuilder( environment ) :
					SearchIntegration.builder( environment );

			StubMappingInitiator initiator = new StubMappingInitiator( tenancyMode );
			mappedIndexes.forEach( initiator::add );
			StubMappingKey mappingKey = new StubMappingKey();
			integrationBuilder.addMappingInitiator( mappingKey, initiator );

			SearchIntegrationPartialBuildState integrationPartialBuildState = integrationBuilder.prepareBuild();
			integrationPartialBuildStates.add( integrationPartialBuildState );

			return overrides -> {
				SearchIntegrationFinalizer finalizer =
						integrationPartialBuildState.finalizer( propertySource.withOverride( overrides ), unusedPropertyChecker );
				StubMappingImpl mapping = finalizer.finalizeMapping(
						mappingKey,
						(context, partialMapping) -> partialMapping.finalizeMapping( schemaManagementStrategy )
				);
				mappings.add( mapping );

				finalizer.finalizeIntegration();
				integrationPartialBuildStates.remove( integrationPartialBuildState );

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
}
