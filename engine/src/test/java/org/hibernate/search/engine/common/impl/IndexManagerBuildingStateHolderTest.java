/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexRootBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.engine.backend.mapping.spi.BackendMapperContext;
import org.hibernate.search.engine.backend.reporting.spi.BackendMappingHints;
import org.hibernate.search.engine.backend.spi.BackendFactory;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.engine.mapper.mapping.building.spi.BackendsInfo;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.reporting.spi.FailureCollector;
import org.hibernate.search.engine.tenancy.spi.TenancyMode;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.test.extension.ExpectedLog4jLog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

// We have to use raw types to mock methods returning generic types with wildcards
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@SuppressWarnings({ "unchecked", "rawtypes" })
class IndexManagerBuildingStateHolderTest {

	@RegisterExtension
	public final ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Mock
	private RootBuildContext rootBuildContextMock;

	@Mock(answer = Answers.CALLS_REAL_METHODS)
	private ConfigurationPropertySource configurationSourceMock;

	@Mock(strictness = Mock.Strictness.LENIENT, answer = Answers.CALLS_REAL_METHODS)
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
	private IndexRootBuilder indexRootBuilderMock;

	private final List<Object> verifiedMocks = new ArrayList<>();

	@BeforeEach
	void setup() {
		Collections.addAll( verifiedMocks,
				rootFailureCollectorMock, backendFailureCollectorMock,
				backendFactoryMock, backendMock, indexManagerBuilderMock, indexRootBuilderMock
		);
	}

	@Test
	void defaultBackend_noNameSet() {
		ArgumentCaptor<ConfigurationPropertySource> backendPropertySourceCapture =
				ArgumentCaptor.forClass( ConfigurationPropertySource.class );
		ArgumentCaptor<ConfigurationPropertySource> indexPropertySourceCapture =
				ArgumentCaptor.forClass( ConfigurationPropertySource.class );

		IndexManagerBuildingStateHolder holder =
				new IndexManagerBuildingStateHolder( beanResolverMock, configurationSourceMock, rootBuildContextMock );
		verifyNoOtherBackendInteractionsAndReset();

		when( configurationSourceMock.get( "backend.type" ) )
				.thenReturn( (Optional) Optional.of( "someBackendType" ) );
		when( beanResolverMock.resolve( BackendFactory.class, "someBackendType", BeanRetrieval.ANY ) )
				.thenReturn( BeanHolder.of( backendFactoryMock ) );
		when( backendFactoryMock.create(
				eq( EventContexts.defaultBackend() ),
				any(),
				backendPropertySourceCapture.capture()
		) )
				.thenReturn( backendMock );
		holder.createBackends( defaultSingleTenancy() );
		verifyNoOtherBackendInteractionsAndReset();

		when( backendMock.createIndexManagerBuilder(
				eq( "myIndex" ), eq( "myType" ), any(), any(),
				indexPropertySourceCapture.capture()
		) )
				.thenReturn( indexManagerBuilderMock );
		when( indexManagerBuilderMock.schemaRootNodeBuilder() )
				.thenReturn( indexRootBuilderMock );
		holder.getIndexManagerBuildingState(
				backendMapperContext(), Optional.empty(), "myIndex", "myType"
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
	}

	@Test
	void explicitBackend() {
		ArgumentCaptor<ConfigurationPropertySource> backendPropertySourceCapture =
				ArgumentCaptor.forClass( ConfigurationPropertySource.class );
		ArgumentCaptor<ConfigurationPropertySource> indexPropertySourceCapture =
				ArgumentCaptor.forClass( ConfigurationPropertySource.class );

		IndexManagerBuildingStateHolder holder =
				new IndexManagerBuildingStateHolder( beanResolverMock, configurationSourceMock, rootBuildContextMock );
		verifyNoOtherBackendInteractionsAndReset();

		when( configurationSourceMock.get( "backends.myBackend.type" ) )
				.thenReturn( (Optional) Optional.of( "someBackendType" ) );
		when( beanResolverMock.resolve( BackendFactory.class, "someBackendType", BeanRetrieval.ANY ) )
				.thenReturn( BeanHolder.of( backendFactoryMock ) );
		when( backendFactoryMock.create(
				eq( EventContexts.fromBackendName( "myBackend" ) ),
				any(),
				backendPropertySourceCapture.capture()
		) )
				.thenReturn( backendMock );
		holder.createBackends( namedSingleTenancy( "myBackend" ) );
		verifyNoOtherBackendInteractionsAndReset();

		when( backendMock.createIndexManagerBuilder(
				eq( "myIndex" ), eq( "myType" ), any(), any(),
				indexPropertySourceCapture.capture()
		) )
				.thenReturn( indexManagerBuilderMock );
		when( indexManagerBuilderMock.schemaRootNodeBuilder() )
				.thenReturn( indexRootBuilderMock );
		holder.getIndexManagerBuildingState(
				backendMapperContext(), Optional.of( "myBackend" ), "myIndex", "myType"
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
	}

	@Test
	void error_missingBackendType_nullType() {
		String keyPrefix = "somePrefix.";

		ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass( Throwable.class );

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
		holder.createBackends( defaultSingleTenancy() );
		verify( backendFailureCollectorMock ).add( throwableCaptor.capture() );
		verifyNoOtherBackendInteractionsAndReset();

		assertThat( throwableCaptor.getValue() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Unable to resolve backend type",
						"configuration property 'somePrefix.backend.type' is not set,"
								+ " and there isn't any backend in the classpath",
						"Check that you added the desired backend to your project's dependencies"
				);
	}

	@Test
	void error_missingBackendType_emptyType() {
		String keyPrefix = "somePrefix.";

		ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass( Throwable.class );

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
		holder.createBackends( defaultSingleTenancy() );
		verify( backendFailureCollectorMock ).add( throwableCaptor.capture() );
		verifyNoOtherBackendInteractionsAndReset();

		assertThat( throwableCaptor.getValue() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Unable to resolve backend type",
						"configuration property 'somePrefix.backend.type' is not set,"
								+ " and there isn't any backend in the classpath",
						"Check that you added the desired backend to your project's dependencies"
				);
	}

	@Test
	void differentTenancyModeNamedBackend() {
		assertThatThrownBy( () -> {
			BackendsInfo result = new BackendsInfo();
			result.collect( Optional.of( "backend-name" ), TenancyMode.MULTI_TENANCY );
			result.collect( Optional.of( "backend-name" ), TenancyMode.SINGLE_TENANCY );
		} ).hasMessageContaining(
				"Different mappings trying to define two backends with the same name 'backend-name' " +
						"but having different expectations on multi-tenancy." );
	}

	@Test
	void differentTenancyModeDefaultBackend() {
		assertThatThrownBy( () -> {
			BackendsInfo result = new BackendsInfo();
			result.collect( Optional.empty(), TenancyMode.SINGLE_TENANCY );
			result.collect( Optional.empty(), TenancyMode.MULTI_TENANCY );
		} ).hasMessageContaining(
				"Different mappings trying to define default backends " +
						"having different expectations on multi-tenancy." );
	}

	private void verifyNoOtherBackendInteractionsAndReset() {
		verifyNoMoreInteractions( verifiedMocks.toArray() );
		reset( verifiedMocks.toArray() );
	}

	public static BackendsInfo defaultSingleTenancy() {
		BackendsInfo result = new BackendsInfo();
		result.collect( Optional.empty(), TenancyMode.SINGLE_TENANCY );
		return result;
	}

	public static BackendsInfo namedSingleTenancy(String name) {
		BackendsInfo result = new BackendsInfo();
		result.collect( Optional.of( name ), TenancyMode.SINGLE_TENANCY );
		return result;
	}

	static BackendMapperContext backendMapperContext() {
		return new BackendMapperContext() {
			@Override
			public BackendMappingHints hints() {
				return BackendMappingHints.NONE;
			}
		};
	}
}
