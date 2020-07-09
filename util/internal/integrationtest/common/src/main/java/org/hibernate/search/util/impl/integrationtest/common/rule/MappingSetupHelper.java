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
import java.util.Optional;
import java.util.function.Consumer;

import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public abstract class MappingSetupHelper<C extends MappingSetupHelper<C, B, R>.AbstractSetupContext, B, R> implements TestRule {

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

	public C start() {
		return backendSetupStrategy.start( createSetupContext(), configurationProvider );
	}

	@Override
	public Statement apply(Statement base, Description description) {
		return statement( base, description );
	}

	protected abstract C createSetupContext();

	protected abstract void close(R toClose) throws Exception;

	private Statement statement(Statement base, Description description) {
		Statement wrapped = new Statement() {
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
		return delegateRule.apply( wrapped, description );
	}

	public abstract class AbstractSetupContext {

		private final List<Configuration<B, R>> configurations = new ArrayList<>();

		protected AbstractSetupContext() {
		}

		public final C withPropertyRadical(String keyRadical, Object value) {
			return withProperty( EngineSettings.PREFIX + keyRadical, value );
		}

		public abstract C withProperty(String keyRadical, Object value);

		public final C withProperties(Map<String, Object> properties) {
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

		public final C withBackendProperties(Map<String, Object> relativeProperties) {
			return withBackendProperties( null, relativeProperties );
		}

		public final C withBackendProperties(String backendName, Map<String, Object> relativeProperties) {
			relativeProperties.forEach( (k, v) -> withBackendProperty( backendName, k, v ) );
			return thisAsC();
		}

		public C withIndexDefaultsProperty(String keyRadical, Object value) {
			return withIndexDefaultsProperty( null, keyRadical, value );
		}

		public C withIndexDefaultsProperty(String backendName, String keyRadical, Object value) {
			return withBackendProperty( backendName, BackendSettings.INDEX_DEFAULTS + "." + keyRadical, value );
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

		MappingSetupHelper<C, B, R> getHelper() {
			return MappingSetupHelper.this;
		}
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
