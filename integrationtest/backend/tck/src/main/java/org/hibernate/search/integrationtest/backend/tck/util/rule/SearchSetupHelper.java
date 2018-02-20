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
import org.hibernate.search.engine.common.SearchMappingRepository;
import org.hibernate.search.engine.common.SearchMappingRepositoryBuilder;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.integrationtest.backend.tck.util.TckConfiguration;
import org.hibernate.search.integrationtest.util.common.stub.mapper.StubMapping;
import org.hibernate.search.integrationtest.util.common.stub.mapper.StubMetadataContributor;
import org.hibernate.search.util.spi.Closer;
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
		testId = description.getTestClass().getSimpleName() + "-" + description.getMethodName() + "-";
	}

	public SetupContext withDefaultConfiguration() {
		TckConfiguration tckConfiguration = TckConfiguration.get();
		ConfigurationPropertySource propertySource = tckConfiguration.getBackendProperties( testId ).withPrefix( "backend.testedBackend" );
		return new SetupContext( propertySource )
				.withProperty( "index.default.backend", "testedBackend" );
	}

	public class SetupContext {

		private final ConfigurationPropertySource propertySource;
		private final Map<String, String> overriddenProperties = new LinkedHashMap<>();
		private final List<IndexDefinition> indexDefinitions = new ArrayList<>();

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

		public void setup() {
			SearchMappingRepositoryBuilder mappingRepositoryBuilder = SearchMappingRepository.builder( propertySource );

			for ( Map.Entry<String, String> entry : overriddenProperties.entrySet() ) {
				mappingRepositoryBuilder = mappingRepositoryBuilder.setProperty( entry.getKey(), entry.getValue() );
			}

			StubMetadataContributor contributor = new StubMetadataContributor( mappingRepositoryBuilder );
			indexDefinitions.forEach( d -> d.beforeBuild( contributor ) );

			SearchMappingRepository mappingRepository = mappingRepositoryBuilder.build();
			mappingRepositories.add( mappingRepository );

			StubMapping mapping = contributor.getResult();
			indexDefinitions.forEach( d -> d.afterBuild( mapping ) );
		}

	}

	public interface IndexSetupListener {

		void onSetup(IndexManager<?> indexManager, String normalizedIndexName);

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

		void beforeBuild(StubMetadataContributor stubMetadataContributor) {
			stubMetadataContributor.add( typeName, rawIndexName, mappingContributor );
		}

		void afterBuild(StubMapping mapping) {
			listener.onSetup(
					mapping.getIndexManagerByTypeIdentifier( typeName ),
					mapping.getNormalizedIndexNameByTypeIdentifier( typeName )
			);
		}

	}
}
