/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertyChecker;
import org.hibernate.search.engine.common.spi.SearchIntegrationBuilder;
import org.hibernate.search.engine.common.spi.SearchIntegrationFinalizer;
import org.hibernate.search.engine.common.spi.SearchIntegrationPartialBuildState;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendAccessor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendSetupStrategy;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapping;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingInitiator;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingKey;

import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class SearchSetupHelper implements TestRule {

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

		SetupContext setupContext = new SetupContext( propertySource )
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
				try {
					base.evaluate();
				}
				finally {
					cleanUp();
				}
			}
		};
		return delegateRule.apply( wrapped, description );
	}

	public void cleanUp() throws IOException {
		try ( Closer<IOException> closer = new Closer<>() ) {
			closer.pushAll(
					SearchIntegrationPartialBuildState::closeOnFailure, integrationPartialBuildStates
			);
			integrationPartialBuildStates.clear();
			closer.pushAll( SearchIntegration::close, integrations );
			integrations.clear();
			closer.push( TckBackendAccessor::close, backendAccessor );
			backendAccessor = null;
		}
	}

	public class SetupContext {

		private final ConfigurationPropertyChecker unusedPropertyChecker;
		private final ConfigurationPropertySource propertySource;
		// Use a LinkedHashMap for deterministic iteration
		private final Map<String, Object> overriddenProperties = new LinkedHashMap<>();
		private final List<IndexDefinition> indexDefinitions = new ArrayList<>();
		private boolean multiTenancyEnabled = false;

		SetupContext(ConfigurationPropertySource basePropertySource) {
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

		public SetupContext withBackendProperty(String backendName, String keyRadical, Object value) {
			return withProperty( EngineSettings.BACKENDS + "." + backendName + "." + keyRadical, value );
		}

		public SetupContext withIndexDefaultsProperty(String backendName, String keyRadical, Object value) {
			return withProperty(
					EngineSettings.BACKENDS + "." + backendName
							+ "." + BackendSettings.INDEX_DEFAULTS + "." + keyRadical,
					value
			);
		}

		public SetupContext withIndex(String rawIndexName,
				Consumer<? super IndexedEntityBindingContext> mappingContributor, IndexSetupListener listener) {
			return withIndex( rawIndexName, ignored -> { }, mappingContributor, listener );
		}

		public SetupContext withIndex(String rawIndexName, Consumer<? super IndexedEntityBindingContext> mappingContributor) {
			return withIndex( rawIndexName, mappingContributor, ignored -> { } );
		}

		public SetupContext withIndex(String rawIndexName,
				Consumer<? super StubIndexMappingContext> optionsContributor,
				Consumer<? super IndexedEntityBindingContext> mappingContributor, IndexSetupListener listener) {
			IndexDefinition indexDefinition = new IndexDefinition( rawIndexName, mappingContributor, listener );
			optionsContributor.accept( new StubIndexMappingContext( indexDefinition ) );
			indexDefinitions.add( indexDefinition );
			return this;
		}

		public SetupContext withMultiTenancy() {
			multiTenancyEnabled = true;
			return this;
		}

		public PartialSetup setupFirstPhaseOnly() {
			SearchIntegrationBuilder integrationBuilder =
					SearchIntegration.builder( propertySource, unusedPropertyChecker );

			StubMappingInitiator initiator = new StubMappingInitiator( multiTenancyEnabled );
			StubMappingKey mappingKey = new StubMappingKey();
			integrationBuilder.addMappingInitiator( mappingKey, initiator );
			indexDefinitions.forEach( d -> d.beforeBuild( initiator ) );

			SearchIntegrationPartialBuildState integrationPartialBuildState = integrationBuilder.prepareBuild();
			integrationPartialBuildStates.add( integrationPartialBuildState );

			return () -> {
				SearchIntegrationFinalizer finalizer =
						integrationPartialBuildState.finalizer( propertySource, unusedPropertyChecker );
				StubMapping mapping = finalizer.finalizeMapping(
						mappingKey, (context, partialMapping) -> partialMapping.finalizeMapping()
				);

				SearchIntegration integration = finalizer.finalizeIntegration();
				integrations.add( integration );
				integrationPartialBuildStates.remove( integrationPartialBuildState );

				indexDefinitions.forEach( d -> d.afterBuild( mapping ) );

				return integration;
			};
		}

		public SearchIntegration setup() {
			return setupFirstPhaseOnly().doSecondPhase();
		}

	}

	public static class StubIndexMappingContext {

		private final IndexDefinition indexDefinition;

		public StubIndexMappingContext(IndexDefinition indexDefinition) {
			this.indexDefinition = indexDefinition;
		}

		public StubIndexMappingContext mappedType(String typeName) {
			indexDefinition.typeName = typeName;
			return this;
		}

		public StubIndexMappingContext backend(String backendName) {
			indexDefinition.backendName = backendName;
			return this;
		}

	}

	public interface IndexSetupListener {

		void onSetup(StubMappingIndexManager indexMapping);

	}

	public interface PartialSetup {

		SearchIntegration doSecondPhase();

	}

	private static class IndexDefinition {
		private final String rawIndexName;
		private final Consumer<? super IndexedEntityBindingContext> mappingContributor;
		private final IndexSetupListener listener;

		private String typeName;
		private String backendName;

		private IndexDefinition(String rawIndexName,
				Consumer<? super IndexedEntityBindingContext> mappingContributor, IndexSetupListener listener) {
			this.rawIndexName = rawIndexName;
			this.mappingContributor = mappingContributor;
			this.listener = listener;
			this.typeName = rawIndexName + "_Type";
			this.backendName = null;
		}

		void beforeBuild(StubMappingInitiator stubMetadataContributor) {
			stubMetadataContributor.add( typeName, backendName, rawIndexName, mappingContributor );
		}

		void afterBuild(StubMapping mapping) {
			listener.onSetup(
					mapping.getIndexMappingByTypeIdentifier( typeName )
			);
		}

	}
}
