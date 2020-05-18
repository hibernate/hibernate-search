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
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertyChecker;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.common.spi.SearchIntegrationBuilder;
import org.hibernate.search.engine.common.spi.SearchIntegrationFinalizer;
import org.hibernate.search.engine.common.spi.SearchIntegrationPartialBuildState;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendAccessor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendSetupStrategy;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapping;
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
	private final TckBackendSetupStrategy setupStrategy;
	private final TestRule delegateRule;

	private final List<SearchIntegrationPartialBuildState> integrationPartialBuildStates = new ArrayList<>();
	private final List<SearchIntegration> integrations = new ArrayList<>();
	private TckBackendAccessor backendAccessor;

	public SearchSetupHelper() {
		this( TckBackendHelper::createDefaultBackendSetupStrategy );
	}

	public SearchSetupHelper(Function<TckBackendHelper, TckBackendSetupStrategy> setupStrategyFunction) {
		this.configurationProvider = new TestConfigurationProvider();
		this.setupStrategy = setupStrategyFunction.apply( TckConfiguration.get().getBackendHelper() );
		Optional<TestRule> setupStrategyTestRule = setupStrategy.getTestRule();
		this.delegateRule = setupStrategyTestRule
				.<TestRule>map( rule -> RuleChain.outerRule( configurationProvider ).around( rule ) )
				.orElse( configurationProvider );
	}

	public SetupContext start() {
		return start( "testedBackend" );
	}

	public SetupContext start(String backendName) {
		ConfigurationPropertySource propertySource = setupStrategy.createBackendConfigurationPropertySource( configurationProvider )
				.withPrefix( EngineSettings.BACKENDS + "." + backendName );

		// Hack to have the resolve() method ignore the various masks and prefixes that we added for TCK purposes only
		propertySource = ConfigurationPropertySource.empty().withOverride( propertySource );

		SetupContext setupContext = new SetupContext( backendName, propertySource )
				.withProperty( EngineSettings.DEFAULT_BACKEND, backendName );

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
		closer.pushAll(
				SearchIntegrationPartialBuildState::closeOnFailure, integrationPartialBuildStates
		);
		integrationPartialBuildStates.clear();
		closer.pushAll( SearchIntegration::close, integrations );
		integrations.clear();
		closer.push( TckBackendAccessor::close, backendAccessor );
		backendAccessor = null;
	}

	public class SetupContext {

		private final String defaultBackendName;
		private final ConfigurationPropertyChecker unusedPropertyChecker;
		private final ConfigurationPropertySource propertySource;
		// Use a LinkedHashMap for deterministic iteration
		private final Map<String, Object> overriddenProperties = new LinkedHashMap<>();
		private final List<StubMappedIndex> mappedIndexes = new ArrayList<>();
		private boolean multiTenancyEnabled = false;
		private StubMappingSchemaManagementStrategy schemaManagementStrategy = StubMappingSchemaManagementStrategy.DROP_AND_CREATE_AND_DROP;

		SetupContext(String defaultBackendName, ConfigurationPropertySource basePropertySource) {
			this.defaultBackendName = defaultBackendName;
			this.unusedPropertyChecker = ConfigurationPropertyChecker.create();
			this.propertySource = basePropertySource.withOverride( ConfigurationPropertySource.fromMap( overriddenProperties ) );
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
			return withBackendProperty( defaultBackendName, keyRadical, value );
		}

		public SetupContext withBackendProperty(String backendName, String keyRadical, Object value) {
			return withProperty( EngineSettings.BACKENDS + "." + backendName + "." + keyRadical, value );
		}

		public SetupContext withIndexDefaultsProperty(String keyRadical, Object value) {
			return withIndexDefaultsProperty( defaultBackendName, keyRadical, value );
		}

		public SetupContext withIndexDefaultsProperty(String backendName, String keyRadical, Object value) {
			return withProperty(
					EngineSettings.BACKENDS + "." + backendName
							+ "." + BackendSettings.INDEX_DEFAULTS + "." + keyRadical,
					value
			);
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
			multiTenancyEnabled = true;
			return this;
		}

		public SetupContext withSchemaManagement(StubMappingSchemaManagementStrategy schemaManagementStrategy) {
			this.schemaManagementStrategy = schemaManagementStrategy;
			return this;
		}

		public PartialSetup setupFirstPhaseOnly() {
			SearchIntegrationBuilder integrationBuilder =
					SearchIntegration.builder( propertySource, unusedPropertyChecker );

			StubMappingInitiator initiator = new StubMappingInitiator( multiTenancyEnabled );
			mappedIndexes.forEach( initiator::add );
			StubMappingKey mappingKey = new StubMappingKey();
			integrationBuilder.addMappingInitiator( mappingKey, initiator );

			SearchIntegrationPartialBuildState integrationPartialBuildState = integrationBuilder.prepareBuild();
			integrationPartialBuildStates.add( integrationPartialBuildState );

			return () -> {
				SearchIntegrationFinalizer finalizer =
						integrationPartialBuildState.finalizer( propertySource, unusedPropertyChecker );
				StubMapping mapping = finalizer.finalizeMapping(
						mappingKey,
						(context, partialMapping) -> partialMapping.finalizeMapping( schemaManagementStrategy )
				);

				SearchIntegration integration = finalizer.finalizeIntegration();
				integrations.add( integration );
				integrationPartialBuildStates.remove( integrationPartialBuildState );

				return integration;
			};
		}

		public SearchIntegration setup() {
			return setupFirstPhaseOnly().doSecondPhase();
		}

	}

	public interface PartialSetup {

		SearchIntegration doSecondPhase();

	}
}
