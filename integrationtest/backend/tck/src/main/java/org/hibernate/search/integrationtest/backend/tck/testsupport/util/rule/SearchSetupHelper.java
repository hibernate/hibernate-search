/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.common.spi.SearchIntegrationBuilder;
import org.hibernate.search.engine.common.spi.SearchIntegrationPartialBuildState;
import org.hibernate.search.engine.mapper.mapping.building.spi.RootIndexModelBindingContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.TestHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapping;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingInitiator;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingKey;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingPartialBuildState;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class SearchSetupHelper implements TestRule {

	private final List<SearchIntegrationPartialBuildState> integrationPartialBuildStates = new ArrayList<>();
	private final List<SearchIntegration> integrations = new ArrayList<>();

	private TestHelper testHelper;

	public SetupContext withDefaultConfiguration() {
		return withConfiguration( null );
	}

	public SetupContext withDefaultConfiguration(String backendName) {
		return withConfiguration( null, backendName );
	}

	public SetupContext withConfiguration(String configurationId) {
		return withConfiguration( configurationId, "testedBackend" );
	}

	public SetupContext withConfiguration(String configurationId, String backendName) {
		TckConfiguration tckConfiguration = TckConfiguration.get();
		ConfigurationPropertySource propertySource = tckConfiguration.getBackendProperties( testHelper, configurationId )
				.withPrefix( "backends." + backendName );

		// Hack to have the resolve() method ignore the various masks and prefixes that we added for TCK purposes only
		propertySource = ConfigurationPropertySource.empty().withOverride( propertySource );

		return new SetupContext( propertySource )
				.withProperty( EngineSettings.DEFAULT_BACKEND, backendName );
	}

	@Override
	public Statement apply(Statement base, Description description) {
		return statement( base, description );
	}

	private Statement statement(final Statement base, final Description description) {
		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				testHelper = TestHelper.create( description );
				try ( Closer<RuntimeException> closer = new Closer<>() ) {
					try {
						base.evaluate();
					}
					finally {
						closer.pushAll( SearchIntegrationPartialBuildState::closeOnFailure, integrationPartialBuildStates );
						integrationPartialBuildStates.clear();
						closer.pushAll( SearchIntegration::close, integrations );
						integrations.clear();
					}
				}
			}
		};
	}

	public class SetupContext {

		private final ConfigurationPropertySource propertySource;
		// Use a LinkedHashMap for deterministic iteration
		private final Map<String, Object> overriddenProperties = new LinkedHashMap<>();
		private final List<IndexDefinition> indexDefinitions = new ArrayList<>();
		private boolean multiTenancyEnabled = false;

		SetupContext(ConfigurationPropertySource basePropertySource) {
			this.propertySource = basePropertySource.withOverride( ConfigurationPropertySource.fromMap( overriddenProperties ) );
		}

		public SetupContext withProperty(String key, Object value) {
			overriddenProperties.put( key, value );
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
				Consumer<? super RootIndexModelBindingContext> mappingContributor, IndexSetupListener listener) {
			return withIndex( rawIndexName + "_Type", rawIndexName, mappingContributor, listener );
		}

		public SetupContext withIndex(String typeName, String rawIndexName,
				Consumer<? super RootIndexModelBindingContext> mappingContributor, IndexSetupListener listener) {
			indexDefinitions.add( new IndexDefinition( typeName, rawIndexName, mappingContributor, listener ) );
			return this;
		}

		public SetupContext withMultiTenancy() {
			multiTenancyEnabled = true;
			return this;
		}

		public PartialSetup setupFirstPhaseOnly() {
			SearchIntegrationBuilder integrationBuilder = SearchIntegration.builder( propertySource );

			StubMappingInitiator initiator = new StubMappingInitiator( multiTenancyEnabled );
			StubMappingKey mappingKey = new StubMappingKey();
			integrationBuilder.addMappingInitiator( mappingKey, initiator );
			indexDefinitions.forEach( d -> d.beforeBuild( initiator ) );

			SearchIntegrationPartialBuildState integrationPartialBuildState = integrationBuilder.prepareBuild();
			integrationPartialBuildStates.add( integrationPartialBuildState );

			return () -> {
				StubMapping mapping = integrationPartialBuildState.finalizeMapping(
						mappingKey, StubMappingPartialBuildState::finalizeMapping
				);

				SearchIntegration integration = integrationPartialBuildState.finalizeIntegration( propertySource );
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

	public interface IndexSetupListener {

		void onSetup(StubMappingIndexManager indexMapping);

	}

	public interface PartialSetup {

		SearchIntegration doSecondPhase();

	}

	private static class IndexDefinition {
		private final String typeName;
		private final String rawIndexName;
		private final Consumer<? super RootIndexModelBindingContext> mappingContributor;
		private final IndexSetupListener listener;

		private IndexDefinition(String typeName, String rawIndexName,
				Consumer<? super RootIndexModelBindingContext> mappingContributor, IndexSetupListener listener) {
			this.typeName = typeName;
			this.rawIndexName = rawIndexName;
			this.mappingContributor = mappingContributor;
			this.listener = listener;
		}

		void beforeBuild(StubMappingInitiator stubMetadataContributor) {
			stubMetadataContributor.add( typeName, rawIndexName, mappingContributor );
		}

		void afterBuild(StubMapping mapping) {
			listener.onSetup(
					mapping.getIndexMappingByTypeIdentifier( typeName )
			);
		}

	}
}
