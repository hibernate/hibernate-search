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
import java.util.function.Consumer;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.util.impl.common.Closer;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackendFactory;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public abstract class MappingSetupHelper<C extends MappingSetupHelper<C, B, R>.SetupContext, B, R> implements TestRule {

	private final List<R> toClose = new ArrayList<>();

	public C withBackendMock(BackendMock backendMock) {
		return createSetupContext( ConfigurationPropertySource.empty() )
				.withProperty( "backend.stubBackend.type", StubBackendFactory.class.getName() )
				.withProperty( "index.default.backend", backendMock.getBackendName() );
	}

	@Override
	public Statement apply(Statement base, Description description) {
		return statement( base );
	}

	protected abstract C createSetupContext(ConfigurationPropertySource propertySource);

	protected abstract void close(R toClose) throws Exception;

	private Statement statement(Statement base) {
		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				try ( Closer<Exception> closer = new Closer<>() ) {
					try {
						base.evaluate();
					}
					finally {
						closer.pushAll( MappingSetupHelper.this::close, toClose );
						toClose.clear();
					}
				}
			}
		};
	}

	public abstract class SetupContext {

		// Use a LinkedHashMap for deterministic iteration
		private final Map<String, String> overriddenProperties = new LinkedHashMap<>();
		private final List<Configuration<B, R>> configurations = new ArrayList<>();

		protected SetupContext() {
		}

		public final C withProperty(String key, String value) {
			overriddenProperties.put( key, value );
			return thisAsC();
		}

		/**
		 * Add configuration to be applied to the builder during setup.
		 * @param beforeBuild A consumer called before Hibernate Search is bootstrapped.
		 * @param afterBuild A consumer called after Hibernate Search is bootstrapped. Gets passed the result of the builder.
		 * @return The setup context, for method chaining.
		 */
		public final C withConfiguration(Consumer<B> beforeBuild, Consumer<R> afterBuild) {
			configurations.add( new Configuration<>( beforeBuild, afterBuild ) );
			return thisAsC();
		}

		/**
		 * Add configuration to be applied to the builder during setup.
		 * @param beforeBuild A consumer called before Hibernate Search is bootstrapped.
		 * @return The setup context, for method chaining.
		 */
		public final C withConfiguration(Consumer<B> beforeBuild) {
			return withConfiguration( beforeBuild, ignored -> { } );
		}

		/**
		 * Setup Hibernate Search, returning the result.
		 * @return The result of setting up Hibernate Search.
		 */
		public final R setup() {
			B builder = createBuilder();

			for ( Map.Entry<String, String> entry : overriddenProperties.entrySet() ) {
				setProperty( builder, entry.getKey(), entry.getValue() );
			}

			configurations.forEach( c -> c.beforeBuild( builder ) );

			R result = build( builder );
			toClose.add( result );

			configurations.forEach( c -> c.afterBuild( result ) );

			return result;
		}

		protected abstract B createBuilder();

		protected abstract void setProperty(B builder, String key, String value);

		protected abstract R build(B builder);

		protected abstract C thisAsC();
	}

	private static class Configuration<B, R> {
		private final Consumer<B> beforeBuild;
		private final Consumer<R> afterBuild;

		private Configuration(Consumer<B> beforeBuild, Consumer<R> afterBuild) {
			this.beforeBuild = beforeBuild;
			this.afterBuild = afterBuild;
		}

		void beforeBuild(B builder) {
			beforeBuild.accept( builder );
		}

		void afterBuild(R result) {
			afterBuild.accept( result );
		}

	}
}
