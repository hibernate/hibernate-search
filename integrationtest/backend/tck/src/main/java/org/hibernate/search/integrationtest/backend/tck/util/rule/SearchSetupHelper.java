/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.util.rule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.index.spi.IndexManager;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.SearchMappingRepository;
import org.hibernate.search.engine.common.spi.SearchMappingRepositoryBuilder;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.integrationtest.backend.tck.util.TckConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapping;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingInitiator;
import org.hibernate.search.util.impl.common.Closer;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingKey;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class SearchSetupHelper implements TestRule {

	private final List<SearchMappingRepository> mappingRepositories = new ArrayList<>();

	private String testId;

	@Override
	public Statement apply(Statement base, Description description) {
		return statement( base, description );
	}

	private Statement statement(final Statement base, final Description description) {
		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				before( description );
				try ( Closer<RuntimeException> closer = new Closer<>() ) {
					try {
						base.evaluate();
					}
					finally {
						closer.pushAll( SearchMappingRepository::close, mappingRepositories );
						mappingRepositories.clear();
					}
				}
			}
		};
	}

	protected void before(Description description) {
		testId = description.getTestClass().getSimpleName() + "-" + description.getMethodName();
	}

	public SetupContext withDefaultConfiguration() {
		return withDefaultConfiguration( "testedBackend" );
	}

	public SetupContext withDefaultConfiguration(String backendName) {
		TckConfiguration tckConfiguration = TckConfiguration.get();
		ConfigurationPropertySource propertySource = tckConfiguration.getBackendProperties( testId ).withPrefix( "backend." + backendName );
		return new SetupContext( propertySource )
				.withProperty( "index.default.backend", backendName );
	}

	public SetupContext withMultiTenancyConfiguration() {
		TckConfiguration tckConfiguration = TckConfiguration.get();
		ConfigurationPropertySource propertySource = tckConfiguration.getMultiTenancyBackendProperties( testId ).withPrefix( "backend.testedBackend" );
		return new SetupContext( propertySource )
				.withProperty( "index.default.backend", "testedBackend" );
	}

	public class SetupContext {

		private final ConfigurationPropertySource propertySource;
		// Use a LinkedHashMap for deterministic iteration
		private final Map<String, String> overriddenProperties = new LinkedHashMap<>();
		private final List<IndexDefinition> indexDefinitions = new ArrayList<>();
		private boolean multiTenancyEnabled = false;

		SetupContext(ConfigurationPropertySource propertySource) {
			this.propertySource = propertySource;
		}

		public SetupContext withProperty(String key, String value) {
			overriddenProperties.put( key, value );
			return this;
		}

		public SetupContext withIndex(String typeName, String rawIndexName,
				Consumer<IndexModelBindingContext> mappingContributor, IndexSetupListener listener) {
			indexDefinitions.add( new IndexDefinition( typeName, rawIndexName, mappingContributor, listener ) );
			return this;
		}

		public SetupContext withMultiTenancy() {
			multiTenancyEnabled = true;
			return this;
		}

		public SearchMappingRepository setup() {
			SearchMappingRepositoryBuilder mappingRepositoryBuilder = SearchMappingRepository.builder( propertySource );

			for ( Map.Entry<String, String> entry : overriddenProperties.entrySet() ) {
				mappingRepositoryBuilder = mappingRepositoryBuilder.setProperty( entry.getKey(), entry.getValue() );
			}

			StubMappingInitiator initiator = new StubMappingInitiator( multiTenancyEnabled );
			StubMappingKey mappingKey = new StubMappingKey();
			mappingRepositoryBuilder.addMappingInitiator( mappingKey, initiator );
			indexDefinitions.forEach( d -> d.beforeBuild( initiator ) );

			SearchMappingRepository mappingRepository = mappingRepositoryBuilder.build();
			mappingRepositories.add( mappingRepository );

			StubMapping mapping = mappingRepository.getMapping( mappingKey );
			indexDefinitions.forEach( d -> d.afterBuild( mapping ) );

			return mappingRepository;
		}

	}

	public interface IndexSetupListener {

		void onSetup(IndexManager<?> indexManager);

	}

	private static class IndexDefinition {
		private final String typeName;
		private final String rawIndexName;
		private final Consumer<IndexModelBindingContext> mappingContributor;
		private final IndexSetupListener listener;

		private IndexDefinition(String typeName, String rawIndexName,
				Consumer<IndexModelBindingContext> mappingContributor, IndexSetupListener listener) {
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
					mapping.getIndexManagerByTypeIdentifier( typeName )
			);
		}

	}
}
