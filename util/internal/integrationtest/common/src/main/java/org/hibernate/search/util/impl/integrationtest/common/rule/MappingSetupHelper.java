/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.search.util.impl.common.Closer;
import org.hibernate.search.util.impl.integrationtest.common.TestHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackendFactory;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public abstract class MappingSetupHelper<C extends MappingSetupHelper<C, B, R>.AbstractSetupContext, B, R> implements TestRule {

	private final List<R> toClose = new ArrayList<>();

	private TestHelper testHelper;

	public C withBackendMock(BackendMock backendMock) {
		String backendName = backendMock.getBackendName();
		return createSetupContext()
				.withBackendProperty( backendName, "type", StubBackendFactory.class.getName() )
				.withPropertyRadical( "indexes.default.backend", backendName );
	}

	public C withBackend(String configurationId, String backendName) {
		String propertiesPath = getPropertiesPath( configurationId );
		return createSetupContext()
				.withPropertyRadical( "indexes.default.backend", backendName )
				.withProperties( testHelper.getPropertiesFromFile( propertiesPath ) );
	}

	@Override
	public Statement apply(Statement base, Description description) {
		return statement( base, description );
	}

	protected abstract C createSetupContext();

	protected abstract String getPropertiesPath(String configurationId);

	protected abstract void close(R toClose) throws Exception;

	private Statement statement(Statement base, Description description) {
		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				try ( Closer<Exception> closer = new Closer<>() ) {
					testHelper = TestHelper.create( description );
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

	public abstract class AbstractSetupContext {

		private final List<Configuration<B, R>> configurations = new ArrayList<>();

		protected AbstractSetupContext() {
		}

		protected abstract C withPropertyRadical(String keyRadical, Object value);

		public abstract C withProperty(String keyRadical, Object value);

		public final C withBackendProperty(String backendName, String keyRadical, Object value) {
			return withPropertyRadical( "backends." + backendName + "." + keyRadical, value );
		}

		protected C withProperties(Map<String, Object> properties) {
			properties.forEach( this::withProperty );
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

			configurations.forEach( c -> c.beforeBuild( builder ) );

			R result = build( builder );
			toClose.add( result );

			configurations.forEach( c -> c.afterBuild( result ) );

			return result;
		}

		protected abstract B createBuilder();

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
