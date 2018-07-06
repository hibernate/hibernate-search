/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.SearchMappingRepository;
import org.hibernate.search.engine.common.SearchMappingRepositoryBuilder;
import org.hibernate.search.util.impl.common.Closer;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackendFactory;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class MappingSetupHelper implements TestRule {

	private final List<SearchMappingRepository> mappingRepositories = new ArrayList<>();

	public SetupContext withBackendMock(BackendMock backendMock) {
		return new SetupContext( ConfigurationPropertySource.empty() )
				.withProperty( "backend.stubBackend.type", StubBackendFactory.class.getName() )
				.withProperty( "index.default.backend", backendMock.getBackendName() );
	}

	@Override
	public Statement apply(Statement base, Description description) {
		return statement( base );
	}

	private Statement statement(Statement base) {
		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
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

	public class SetupContext {

		private final ConfigurationPropertySource propertySource;
		// Use a LinkedHashMap for deterministic iteration
		private final Map<String, String> overriddenProperties = new LinkedHashMap<>();
		private final List<MappingDefinition<?>> mappingDefinitions = new ArrayList<>();

		SetupContext(ConfigurationPropertySource propertySource) {
			this.propertySource = propertySource;
		}

		public SetupContext withProperty(String key, String value) {
			overriddenProperties.put( key, value );
			return this;
		}

		/**
		 * Add a mapping initiator to the setup. Note this method must not be called if you use {@link #setup(Function)}.
		 * @param beforeBuild A function called before Hibernate Search is bootstrapped.
		 * @param afterBuild A consumer called after Hibernate Search is bootstrapped. Gets passed the result of {@code beforeBuild}.
		 * @param <T> The type of the result of {@code beforeBuild}.
		 * @return The setup context, for method chaining.
		 */
		public <T> SetupContext withMapping(Function<SearchMappingRepositoryBuilder, T> beforeBuild,
				BiConsumer<SearchMappingRepository, T> afterBuild) {
			mappingDefinitions.add( new MappingDefinition<>( beforeBuild, afterBuild ) );
			return this;
		}

		/**
		 * Setup Hibernate Search, returning the mapping for the given initiator.
		 * @param beforeBuild A function called before Hibernate Search is bootstrapped.
		 * @param <T> The type of the result of {@code beforeBuild}.
		 * @return The result of {@code beforeBuild}
		 */
		public <T> T setup(Function<SearchMappingRepositoryBuilder, T> beforeBuild) {
			AtomicReference<T> reference = new AtomicReference<>();
			withMapping( beforeBuild, (mappingRepository, beforeBuildResult) -> reference.set( beforeBuildResult ) )
					.setup();
			return reference.get();
		}

		/**
		 * Setup Hibernate Search, returning the {@link SearchMappingRepository}.
		 * @return The created {@link SearchMappingRepository}
		 */
		public SearchMappingRepository setup() {
			SearchMappingRepositoryBuilder mappingRepositoryBuilder = SearchMappingRepository.builder( propertySource );

			for ( Map.Entry<String, String> entry : overriddenProperties.entrySet() ) {
				mappingRepositoryBuilder.setProperty( entry.getKey(), entry.getValue() );
			}

			List<Object> beforeBuildResults = mappingDefinitions.stream()
					.map( d -> d.beforeBuild( mappingRepositoryBuilder ) )
					.collect( Collectors.toList() );

			SearchMappingRepository mappingRepository = mappingRepositoryBuilder.build();
			mappingRepositories.add( mappingRepository );

			for ( int i = 0; i < mappingDefinitions.size(); i++ ) {
				MappingDefinition definition = mappingDefinitions.get( i );
				definition.afterBuild( mappingRepository, beforeBuildResults.get( i ) );
			}

			return mappingRepository;
		}
	}

	private static class MappingDefinition<T> {
		private final Function<SearchMappingRepositoryBuilder, T> beforeBuild;
		private final BiConsumer<SearchMappingRepository, T> afterBuild;

		private MappingDefinition(Function<SearchMappingRepositoryBuilder, T> beforeBuild,
				BiConsumer<SearchMappingRepository, T> afterBuild) {
			this.beforeBuild = beforeBuild;
			this.afterBuild = afterBuild;
		}

		T beforeBuild(SearchMappingRepositoryBuilder mappingRepositoryBuilder) {
			return beforeBuild.apply( mappingRepositoryBuilder );
		}

		void afterBuild(SearchMappingRepository mappingRepository, T beforeBuildResult) {
			afterBuild.accept( mappingRepository, beforeBuildResult );
		}

	}
}
