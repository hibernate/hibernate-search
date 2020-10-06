/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.engine.backend.spi.BackendFactory;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.reporting.spi.FailureCollector;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.log4j.Level;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

// We have to use raw types to mock methods returning generic types with wildcards
@SuppressWarnings({ "unchecked", "rawtypes" })
public class IndexManagerBuildingStateHolderTest {

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	@Rule
	public final ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Mock
	private RootBuildContext rootBuildContextMock;

	@Mock(answer = Answers.CALLS_REAL_METHODS)
	private ConfigurationPropertySource configurationSourceMock;

	@Mock(lenient = true, answer = Answers.CALLS_REAL_METHODS)
	private BeanResolver beanResolverMock;

	@Mock
	private FailureCollector rootFailureCollectorMock;

	@Mock
	private ContextualFailureCollector backendFailureCollectorMock;

	@Mock
	private BackendFactory backendFactoryMock;

	@Mock
	private BackendImplementor backendMock;

	@Mock
	private IndexManagerBuilder indexManagerBuilderMock;

	@Mock
	private IndexSchemaRootNodeBuilder indexSchemaRootNodeBuilderMock;

	private final List<Object> verifiedMocks = new ArrayList<>();

	@Before
	public void setup() {
		Collections.addAll( verifiedMocks,
				rootFailureCollectorMock, backendFailureCollectorMock,
				backendFactoryMock, backendMock, indexManagerBuilderMock, indexSchemaRootNodeBuilderMock );
	}

	@Test
	public void defaultBackend_noNameSet() {
		ArgumentCaptor<ConfigurationPropertySource> backendPropertySourceCapture =
				ArgumentCaptor.forClass( ConfigurationPropertySource.class );
		ArgumentCaptor<ConfigurationPropertySource> indexPropertySourceCapture =
				ArgumentCaptor.forClass( ConfigurationPropertySource.class );

		when( configurationSourceMock.get( "default_backend" ) )
				.thenReturn( (Optional) Optional.empty() );
		IndexManagerBuildingStateHolder holder =
				new IndexManagerBuildingStateHolder( beanResolverMock, configurationSourceMock, rootBuildContextMock );
		verifyNoOtherBackendInteractionsAndReset();

		when( configurationSourceMock.get( "backend.type" ) )
				.thenReturn( (Optional) Optional.of( "someBackendType" ) );
		when( beanResolverMock.resolve( BackendFactory.class, "someBackendType" ) )
				.thenReturn( BeanHolder.of( backendFactoryMock ) );
		when( backendFactoryMock.create(
				eq( EventContexts.defaultBackend() ),
				any(),
				backendPropertySourceCapture.capture()
		) )
				.thenReturn( backendMock );
		holder.createBackends( CollectionHelper.asSet( Optional.empty() ) );
		verifyNoOtherBackendInteractionsAndReset();

		when( backendMock.createIndexManagerBuilder(
				eq( "myIndex" ),
				eq( "myType" ),
				eq( false ),
				any(),
				indexPropertySourceCapture.capture()
		) )
				.thenReturn( indexManagerBuilderMock );
		when( indexManagerBuilderMock.schemaRootNodeBuilder() )
				.thenReturn( indexSchemaRootNodeBuilderMock );
		holder.getIndexManagerBuildingState(
				Optional.empty(), "myIndex", "myType", false
		);
		verifyNoOtherBackendInteractionsAndReset();

		// Check that configuration property sources behave as expected
		Optional result;

		// Backend configuration
		when( configurationSourceMock.get( "backend.foo" ) )
				.thenReturn( (Optional) Optional.of( "bar" ) );
		result = backendPropertySourceCapture.getValue().get( "foo" );
		verifyNoOtherBackendInteractionsAndReset();
		assertThat( result ).contains( "bar" );

		// Index configuration
		when( configurationSourceMock.get( "backend.indexes.myIndex.foo" ) )
				.thenReturn( (Optional) Optional.of( "bar" ) );
		result = indexPropertySourceCapture.getValue().get( "foo" );
		verifyNoOtherBackendInteractionsAndReset();
		assertThat( result ).contains( "bar" );

		// Index configuration defaults
		when( configurationSourceMock.get( "backend.indexes.myIndex.foo" ) )
				.thenReturn( Optional.empty() );
		when( configurationSourceMock.get( "backend.foo" ) )
				.thenReturn( (Optional) Optional.of( "bar" ) );
		result = indexPropertySourceCapture.getValue().get( "foo" );
		verifyNoOtherBackendInteractionsAndReset();
		assertThat( result ).contains( "bar" );

		// Legacy "index_defaults"
		logged.expectEvent( Level.WARN, "Using configuration property 'hibernate.search.backend.index_defaults.foo'."
				+ " The prefix 'index_defaults' is deprecated and its support will ultimately be removed."
				+ " Instead, you should just set defaults for index properties at the backend level."
				+ " For example, set 'hibernate.search.backend.indexing.queue_size'"
				+ " instead of 'hibernate.search.backend.index_defaults.indexing.queue_size'." );
		when( configurationSourceMock.get( "backend.indexes.myIndex.foo" ) )
				.thenReturn( Optional.empty() );
		when( configurationSourceMock.get( "backend.foo" ) )
				.thenReturn( (Optional) Optional.empty() );
		when( configurationSourceMock.get( "backend.index_defaults.foo" ) )
				.thenReturn( (Optional) Optional.of( "bar" ) );
		when( configurationSourceMock.resolve( "backend.index_defaults.foo" ) )
				.thenReturn( Optional.of( "hibernate.search.backend.index_defaults.foo" ) );
		result = indexPropertySourceCapture.getValue().get( "foo" );
		verifyNoOtherBackendInteractionsAndReset();
		assertThat( result ).contains( "bar" );
	}

	@Test
	public void defaultBackend_nameSet() {
		ArgumentCaptor<ConfigurationPropertySource> backendPropertySourceCapture =
				ArgumentCaptor.forClass( ConfigurationPropertySource.class );
		ArgumentCaptor<ConfigurationPropertySource> indexPropertySourceCapture =
				ArgumentCaptor.forClass( ConfigurationPropertySource.class );

		logged.expectMessage(
				"Using configuration property 'hibernate.search.default_backend' to set the name of the default backend to 'myBackend'.",
				"This configuration property is deprecated and shouldn't be used anymore" );
		when( configurationSourceMock.get( "default_backend" ) )
				.thenReturn( (Optional) Optional.of( "myBackend" ) );
		when( configurationSourceMock.resolve( "default_backend" ) )
				.thenReturn( Optional.of( "hibernate.search.default_backend" ) );
		IndexManagerBuildingStateHolder holder =
				new IndexManagerBuildingStateHolder( beanResolverMock, configurationSourceMock, rootBuildContextMock );
		verifyNoOtherBackendInteractionsAndReset();

		when( configurationSourceMock.get( "backend.type" ) )
				.thenReturn( (Optional) Optional.empty() );
		when( configurationSourceMock.get( "backends.myBackend.type" ) )
				.thenReturn( (Optional) Optional.of( "someBackendType" ) );
		when( beanResolverMock.resolve( BackendFactory.class, "someBackendType" ) )
				.thenReturn( BeanHolder.of( backendFactoryMock ) );
		when( backendFactoryMock.create(
				eq( EventContexts.fromBackendName( "myBackend" ) ),
				any(),
				backendPropertySourceCapture.capture()
		) )
				.thenReturn( backendMock );
		holder.createBackends( CollectionHelper.asSet( Optional.empty() ) );
		verifyNoOtherBackendInteractionsAndReset();

		when( backendMock.createIndexManagerBuilder(
				eq( "myIndex" ),
				eq( "myType" ),
				eq( false ),
				any(),
				indexPropertySourceCapture.capture()
		) )
				.thenReturn( indexManagerBuilderMock );
		when( indexManagerBuilderMock.schemaRootNodeBuilder() )
				.thenReturn( indexSchemaRootNodeBuilderMock );
		holder.getIndexManagerBuildingState(
				Optional.empty(), "myIndex", "myType", false
		);
		verifyNoOtherBackendInteractionsAndReset();

		// Check that configuration property sources behave as expected
		Optional result;

		// Backend configuration - syntax "hibernate.search.backend.foo"
		when( configurationSourceMock.get( "backend.foo" ) )
				.thenReturn( (Optional) Optional.of( "bar" ) );
		result = backendPropertySourceCapture.getValue().get( "foo" );
		verifyNoOtherBackendInteractionsAndReset();
		assertThat( result ).contains( "bar" );

		// Backend configuration - syntax "hibernate.search.backends.myBackend.foo"
		when( configurationSourceMock.get( "backend.foo" ) )
				.thenReturn( (Optional) Optional.empty() );
		when( configurationSourceMock.get( "backends.myBackend.foo" ) )
				.thenReturn( (Optional) Optional.of( "bar" ) );
		result = backendPropertySourceCapture.getValue().get( "foo" );
		verifyNoOtherBackendInteractionsAndReset();
		assertThat( result ).contains( "bar" );

		// Index configuration
		when( configurationSourceMock.get( "backend.indexes.myIndex.foo" ) )
				.thenReturn( (Optional) Optional.empty() );
		when( configurationSourceMock.get( "backends.myBackend.indexes.myIndex.foo" ) )
				.thenReturn( (Optional) Optional.of( "bar" ) );
		result = indexPropertySourceCapture.getValue().get( "foo" );
		verifyNoOtherBackendInteractionsAndReset();
		assertThat( result ).contains( "bar" );

		// Index configuration defaults
		when( configurationSourceMock.get( "backend.indexes.myIndex.foo" ) )
				.thenReturn( (Optional) Optional.empty() );
		when( configurationSourceMock.get( "backends.myBackend.indexes.myIndex.foo" ) )
				.thenReturn( Optional.empty() );
		when( configurationSourceMock.get( "backend.foo" ) )
				.thenReturn( (Optional) Optional.empty() );
		when( configurationSourceMock.get( "backends.myBackend.foo" ) )
				.thenReturn( (Optional) Optional.of( "bar" ) );
		result = indexPropertySourceCapture.getValue().get( "foo" );
		verifyNoOtherBackendInteractionsAndReset();
		assertThat( result ).contains( "bar" );

		// Legacy "index_defaults"
		logged.expectEvent( Level.WARN, "Using configuration property 'hibernate.search.backends.myBackend.index_defaults.foo'."
						+ " The prefix 'index_defaults' is deprecated and its support will ultimately be removed."
						+ " Instead, you should just set defaults for index properties at the backend level."
						+ " For example, set 'hibernate.search.backend.indexing.queue_size'"
						+ " instead of 'hibernate.search.backend.index_defaults.indexing.queue_size'." );
		when( configurationSourceMock.get( "backend.indexes.myIndex.foo" ) )
				.thenReturn( (Optional) Optional.empty() );
		when( configurationSourceMock.get( "backends.myBackend.indexes.myIndex.foo" ) )
				.thenReturn( Optional.empty() );
		when( configurationSourceMock.get( "backend.foo" ) )
				.thenReturn( (Optional) Optional.empty() );
		when( configurationSourceMock.get( "backends.myBackend.foo" ) )
				.thenReturn( (Optional) Optional.empty() );
		when( configurationSourceMock.get( "backend.index_defaults.foo" ) )
				.thenReturn( (Optional) Optional.empty() );
		when( configurationSourceMock.get( "backends.myBackend.index_defaults.foo" ) )
				.thenReturn( (Optional) Optional.of( "bar" ) );
		when( configurationSourceMock.resolve( "backends.myBackend.index_defaults.foo" ) )
				.thenReturn( Optional.of( "hibernate.search.backends.myBackend.index_defaults.foo" ) );
		result = indexPropertySourceCapture.getValue().get( "foo" );
		verifyNoOtherBackendInteractionsAndReset();
		assertThat( result ).contains( "bar" );
	}

	@Test
	public void explicitBackend() {
		ArgumentCaptor<ConfigurationPropertySource> backendPropertySourceCapture =
				ArgumentCaptor.forClass( ConfigurationPropertySource.class );
		ArgumentCaptor<ConfigurationPropertySource> indexPropertySourceCapture =
				ArgumentCaptor.forClass( ConfigurationPropertySource.class );

		when( configurationSourceMock.get( "default_backend" ) )
				.thenReturn( (Optional) Optional.empty() );
		IndexManagerBuildingStateHolder holder =
				new IndexManagerBuildingStateHolder( beanResolverMock, configurationSourceMock, rootBuildContextMock );
		verifyNoOtherBackendInteractionsAndReset();

		when( configurationSourceMock.get( "backends.myBackend.type" ) )
				.thenReturn( (Optional) Optional.of( "someBackendType" ) );
		when( beanResolverMock.resolve( BackendFactory.class, "someBackendType" ) )
				.thenReturn( BeanHolder.of( backendFactoryMock ) );
		when( backendFactoryMock.create(
				eq( EventContexts.fromBackendName( "myBackend" ) ),
				any(),
				backendPropertySourceCapture.capture()
		) )
				.thenReturn( backendMock );
		holder.createBackends( CollectionHelper.asSet( Optional.of( "myBackend" ) ) );
		verifyNoOtherBackendInteractionsAndReset();

		when( backendMock.createIndexManagerBuilder(
				eq( "myIndex" ),
				eq( "myType" ),
				eq( false ),
				any(),
				indexPropertySourceCapture.capture()
		) )
				.thenReturn( indexManagerBuilderMock );
		when( indexManagerBuilderMock.schemaRootNodeBuilder() )
				.thenReturn( indexSchemaRootNodeBuilderMock );
		holder.getIndexManagerBuildingState(
				Optional.of( "myBackend" ), "myIndex", "myType", false
		);
		verifyNoOtherBackendInteractionsAndReset();

		// Check that configuration property sources behave as expected
		Optional result;

		// Backend configuration - empty
		when( configurationSourceMock.get( "backends.myBackend.foo" ) )
				.thenReturn( (Optional) Optional.empty() );
		result = backendPropertySourceCapture.getValue().get( "foo" );
		verifyNoOtherBackendInteractionsAndReset();
		assertThat( result ).isEmpty();

		// Backend configuration - non-empty
		when( configurationSourceMock.get( "backends.myBackend.foo" ) )
				.thenReturn( (Optional) Optional.of( "bar" ) );
		result = backendPropertySourceCapture.getValue().get( "foo" );
		verifyNoOtherBackendInteractionsAndReset();
		assertThat( result ).contains( "bar" );

		// Index configuration
		when( configurationSourceMock.get( "backends.myBackend.indexes.myIndex.foo" ) )
				.thenReturn( (Optional) Optional.of( "bar" ) );
		result = indexPropertySourceCapture.getValue().get( "foo" );
		verifyNoOtherBackendInteractionsAndReset();
		assertThat( result ).contains( "bar" );

		// Index configuration defaults
		when( configurationSourceMock.get( "backends.myBackend.indexes.myIndex.foo" ) )
				.thenReturn( Optional.empty() );
		when( configurationSourceMock.get( "backends.myBackend.foo" ) )
				.thenReturn( (Optional) Optional.of( "bar" ) );
		result = indexPropertySourceCapture.getValue().get( "foo" );
		verifyNoOtherBackendInteractionsAndReset();
		assertThat( result ).contains( "bar" );

		// Legacy "index_defaults"
		logged.expectEvent( Level.WARN, "Using configuration property 'hibernate.search.backends.myBackend.index_defaults.foo'."
				+ " The prefix 'index_defaults' is deprecated and its support will ultimately be removed."
				+ " Instead, you should just set defaults for index properties at the backend level."
				+ " For example, set 'hibernate.search.backend.indexing.queue_size'"
				+ " instead of 'hibernate.search.backend.index_defaults.indexing.queue_size'." );
		when( configurationSourceMock.get( "backends.myBackend.indexes.myIndex.foo" ) )
				.thenReturn( Optional.empty() );
		when( configurationSourceMock.get( "backends.myBackend.foo" ) )
				.thenReturn( Optional.empty() );
		when( configurationSourceMock.get( "backends.myBackend.index_defaults.foo" ) )
				.thenReturn( (Optional) Optional.of( "bar" ) );
		when( configurationSourceMock.resolve( "backends.myBackend.index_defaults.foo" ) )
				.thenReturn( Optional.of( "hibernate.search.backends.myBackend.index_defaults.foo" ) );
		result = indexPropertySourceCapture.getValue().get( "foo" );
		verifyNoOtherBackendInteractionsAndReset();
		assertThat( result ).contains( "bar" );
	}

	@Test
	public void error_missingBackendType_nullType() {
		String keyPrefix = "somePrefix.";

		ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass( Throwable.class );

		when( configurationSourceMock.get( "default_backend" ) )
				.thenReturn( (Optional) Optional.empty() );
		IndexManagerBuildingStateHolder holder =
				new IndexManagerBuildingStateHolder( beanResolverMock, configurationSourceMock, rootBuildContextMock );
		verifyNoOtherBackendInteractionsAndReset();

		when( configurationSourceMock.get( "backend.type" ) )
				.thenReturn( Optional.empty() );
		when( beanResolverMock.namedConfiguredForRole( BackendFactory.class ) )
				.thenReturn( Collections.emptyMap() );
		when( configurationSourceMock.resolve( "backend.type" ) )
				.thenReturn( Optional.of( keyPrefix + "backend.type" ) );
		when( rootBuildContextMock.getFailureCollector() )
				.thenReturn( rootFailureCollectorMock );
		when( rootFailureCollectorMock.withContext( EventContexts.defaultBackend() ) )
				.thenReturn( backendFailureCollectorMock );
		holder.createBackends( CollectionHelper.asSet( Optional.empty() ) );
		verify( backendFailureCollectorMock ).add( throwableCaptor.capture() );
		verifyNoOtherBackendInteractionsAndReset();

		assertThat( throwableCaptor.getValue() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Configuration property 'somePrefix.backend.type' is not set, and no backend was found in the classpath",
						"Did you forget to add the desired backend to your project's dependencies?" );
	}

	@Test
	public void error_missingBackendType_emptyType() {
		String keyPrefix = "somePrefix.";

		ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass( Throwable.class );

		when( configurationSourceMock.get( "default_backend" ) )
				.thenReturn( (Optional) Optional.empty() );
		IndexManagerBuildingStateHolder holder =
				new IndexManagerBuildingStateHolder( beanResolverMock, configurationSourceMock, rootBuildContextMock );
		verifyNoOtherBackendInteractionsAndReset();

		when( configurationSourceMock.get( "backend.type" ) )
				.thenReturn( (Optional) Optional.of( "" ) );
		when( beanResolverMock.namedConfiguredForRole( BackendFactory.class ) )
				.thenReturn( Collections.emptyMap() );
		when( configurationSourceMock.resolve( "backend.type" ) )
				.thenReturn( Optional.of( keyPrefix + "backend.type" ) );
		when( rootBuildContextMock.getFailureCollector() )
				.thenReturn( rootFailureCollectorMock );
		when( rootFailureCollectorMock.withContext( EventContexts.defaultBackend() ) )
				.thenReturn( backendFailureCollectorMock );
		holder.createBackends( CollectionHelper.asSet( Optional.empty() ) );
		verify( backendFailureCollectorMock ).add( throwableCaptor.capture() );
		verifyNoOtherBackendInteractionsAndReset();

		assertThat( throwableCaptor.getValue() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Configuration property 'somePrefix.backend.type' is not set, and no backend was found in the classpath",
						"Did you forget to add the desired backend to your project's dependencies?" );
	}

	private void verifyNoOtherBackendInteractionsAndReset() {
		verifyNoMoreInteractions( verifiedMocks.toArray() );
		reset( verifiedMocks.toArray() );
	}

}
