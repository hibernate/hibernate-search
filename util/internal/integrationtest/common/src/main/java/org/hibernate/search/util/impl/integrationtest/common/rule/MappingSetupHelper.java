/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.BackendMappingHandle;

import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public abstract class MappingSetupHelper<C extends MappingSetupHelper<C, B, BC, R>.AbstractSetupContext, B, BC, R> implements TestRule {

	private final TestConfigurationProvider configurationProvider;
	private final BackendSetupStrategy backendSetupStrategy;
	private final TestRule delegateRule;

	private final List<R> toClose = new ArrayList<>();

	protected MappingSetupHelper(BackendSetupStrategy backendSetupStrategy) {
		this.configurationProvider = new TestConfigurationProvider();
		this.backendSetupStrategy = backendSetupStrategy;
		Optional<TestRule> setupStrategyTestRule = backendSetupStrategy.getTestRule();
		this.delegateRule = setupStrategyTestRule
				.<TestRule>map( rule -> RuleChain.outerRule( configurationProvider ).around( rule ) )
				.orElse( configurationProvider );
	}

	@Override
	public String toString() {
		return backendSetupStrategy.toString();
	}

	public C start() {
		C setupContext = createSetupContext();
		return backendSetupStrategy.start( setupContext, configurationProvider, setupContext.backendMappingHandlePromise );
	}

	@Override
	public Statement apply(Statement base, Description description) {
		return statement( base, description );
	}

	protected abstract C createSetupContext();

	protected void init() {
	}

	protected abstract void close(R toClose) throws Exception;

	private Statement statement(Statement base, Description description) {
		Statement wrapped = new Statement() {
			@Override
			public void evaluate() throws Throwable {
				try ( Closer<Exception> closer = new Closer<>() ) {
					try {
						init();
						base.evaluate();
					}
					finally {
						// Make sure to close the last-created resource first,
						// to avoid problems e.g. if starting multiple ORM SessionFactories
						// where only the first one creates/drops the schema:
						// in that case the other SessionFactories must be closed before the first one,
						// to avoid any SQL queries after the schema was dropped.
						Collections.reverse( toClose );
						closer.pushAll( MappingSetupHelper.this::close, toClose );
						toClose.clear();
					}
				}
			}
		};
		return delegateRule.apply( wrapped, description );
	}

	public abstract class AbstractSetupContext {

		private final List<Configuration<BC, R>> configurations = new ArrayList<>();
		final CompletableFuture<BackendMappingHandle> backendMappingHandlePromise = new CompletableFuture<>();

		private boolean setupCalled;

		protected AbstractSetupContext() {
		}

		public final C with(UnaryOperator<C> config) {
			return config.apply( thisAsC() );
		}

		public final C withPropertyRadical(String keyRadical, Object value) {
			return withProperty( EngineSettings.PREFIX + keyRadical, value );
		}

		public abstract C withProperty(String keyRadical, Object value);

		public final C withProperties(Map<String, ?> properties) {
			properties.forEach( this::withProperty );
			return thisAsC();
		}

		public final C withProperties(String propertyFilePath) {
			return withProperties( configurationProvider.getPropertiesFromFile( propertyFilePath ) );
		}

		public final C withBackendProperty(String keyRadical, Object value) {
			return withBackendProperty( null, keyRadical, value );
		}

		public final C withBackendProperty(String backendName, String keyRadical, Object value) {
			if ( backendName == null ) {
				return withPropertyRadical( EngineSettings.Radicals.BACKEND + "." + keyRadical, value );
			}
			else {
				return withPropertyRadical( EngineSettings.Radicals.BACKENDS + "." + backendName + "." + keyRadical, value );
			}
		}

		public final C withBackendProperties(String backendName, Map<String, ?> relativeProperties) {
			relativeProperties.forEach( (k, v) -> withBackendProperty( backendName, k, v ) );
			return thisAsC();
		}

		/**
		 * Add configuration to be applied to the builder during setup.
		 * @param beforeBuild A consumer called before Hibernate Search is bootstrapped.
		 * @param afterBuild A consumer called after Hibernate Search is bootstrapped. Gets passed the result of the builder.
		 * @return The setup context, for method chaining.
		 */
		public final C withConfiguration(Consumer<BC> beforeBuild, Consumer<R> afterBuild) {
			configurations.add( new Configuration<>( beforeBuild, afterBuild ) );
			return thisAsC();
		}

		/**
		 * Add configuration to be applied to the builder during setup.
		 * @param beforeBuild A consumer called before Hibernate Search is bootstrapped.
		 * @return The setup context, for method chaining.
		 */
		public final C withConfiguration(Consumer<BC> beforeBuild) {
			return withConfiguration( beforeBuild, ignored -> { } );
		}

		/**
		 * Setup Hibernate Search, returning the result.
		 * @return The result of setting up Hibernate Search.
		 */
		public final R setup() {
			if ( setupCalled ) {
				throw new IllegalStateException( "SetupContext#setup() was called multiple times on the same context" );
			}
			setupCalled = true;

			B builder = createBuilder();
			consumeBeforeBuildConfigurations(
					builder,
					configurations.stream().map( c -> c.beforeBuild )
							.collect( Collectors.toList() )
			);

			try {
				R result = build( builder );
				toClose.add( result );
				backendMappingHandlePromise.complete( toBackendMappingHandle( result ) );

				configurations.forEach( c -> c.afterBuild( result ) );

				return result;
			}
			catch (Throwable t) {
				// If the future was not complete, backendMock.verifyExpectationsMet()
				// would assume there was some test setup issue; avoid that.
				backendMappingHandlePromise.complete( null );
				throw t;
			}
		}

		protected abstract B createBuilder();

		protected abstract void consumeBeforeBuildConfigurations(B builder, List<Consumer<BC>> consumers);

		protected abstract R build(B builder);

		protected abstract BackendMappingHandle toBackendMappingHandle(R result);

		protected abstract C thisAsC();
	}

	private static class Configuration<BC, R> {
		private final Consumer<BC> beforeBuild;
		private final Consumer<R> afterBuild;

		private Configuration(Consumer<BC> beforeBuild, Consumer<R> afterBuild) {
			this.beforeBuild = beforeBuild;
			this.afterBuild = afterBuild;
		}

		void beforeBuild(BC builder) {
			beforeBuild.accept( builder );
		}

		void afterBuild(R result) {
			afterBuild.accept( result );
		}

	}
}
