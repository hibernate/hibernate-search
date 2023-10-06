/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.extension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.common.assertion.MappingAssertionHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.BackendMappingHandle;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public abstract class MappingSetupHelper<C extends MappingSetupHelper<C, B, BC, R, SV>.AbstractSetupContext, B, BC, R, SV>
		implements AfterAllCallback, AfterEachCallback, BeforeAllCallback, BeforeEachCallback {

	private final TestConfigurationProvider configurationProvider;
	private final BackendSetupStrategy backendSetupStrategy;
	protected boolean callOncePerClass = false;

	private final List<R> toClose = new ArrayList<>();

	protected MappingSetupHelper(BackendSetupStrategy backendSetupStrategy) {
		this.configurationProvider = new TestConfigurationProvider();
		this.backendSetupStrategy = backendSetupStrategy;
	}

	@Override
	public String toString() {
		return backendSetupStrategy.toString();
	}

	public abstract MappingAssertionHelper<? super R> assertions();

	protected abstract SV defaultSetupVariant();

	public C start() {
		return start( defaultSetupVariant() );
	}

	public C start(SV setupVariant) {
		C setupContext = createSetupContext( setupVariant );
		return backendSetupStrategy.start( setupContext, configurationProvider, setupContext.backendMappingHandlePromise );
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		configurationProvider.afterAll( context );
		if ( callOncePerClass ) {
			cleanUp();
		}
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		configurationProvider.afterEach( context );
		if ( !callOncePerClass ) {
			cleanUp();
		}
	}

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		configurationProvider.beforeAll( context );
		callOncePerClass = true;
		if ( callOncePerClass ) {
			init();
		}
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		configurationProvider.beforeEach( context );
		if ( !callOncePerClass ) {
			init();
		}
	}

	private void cleanUp() throws Exception {
		try ( Closer<Exception> closer = new Closer<>() ) {
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

	protected abstract C createSetupContext(SV setupVariant);

	protected void init() {
	}

	protected abstract void close(R toClose) throws Exception;

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
			return withConfiguration( beforeBuild, ignored -> {} );
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
