/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.common.resources.impl.EngineThreads;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.engine.environment.thread.impl.ThreadPoolProviderImpl;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingKey;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class SearchIntegrationImplTest {

	@Mock
	private BeanProvider beanProviderMock;

	@Mock
	private BeanHolder<? extends FailureHandler> failureHandlerHolderMock;

	@Mock
	private ThreadPoolProviderImpl threadPoolProviderMock;

	@Mock
	private MappingImplementor<?> mapping1Mock;

	@Mock
	private MappingImplementor<?> mapping2Mock;

	@Mock
	private BackendImplementor backend1Mock;

	@Mock
	private BackendImplementor backend2Mock;

	@Mock
	private IndexManagerImplementor indexManager1Mock;

	@Mock
	private IndexManagerImplementor indexManager2Mock;

	@Mock
	private EngineThreads engineThreadsMock;

	@Mock
	private TimingSource timingSourceMock;

	private SearchIntegrationImpl searchIntegration;

	@BeforeEach
	void setup() {
		Map<MappingKey<?, ?>, MappingImplementor<?>> mappings = new LinkedHashMap<>();
		mappings.put( mappingKey( "mapping1" ), mapping1Mock );
		mappings.put( mappingKey( "mapping2" ), mapping2Mock );

		Map<String, BackendImplementor> backends = new LinkedHashMap<>();
		backends.put( "backend1", backend1Mock );
		backends.put( "backend2", backend2Mock );

		Map<String, IndexManagerImplementor> indexManagers = new LinkedHashMap<>();
		indexManagers.put( "index1", indexManager1Mock );
		indexManagers.put( "index2", indexManager2Mock );

		searchIntegration = new SearchIntegrationImpl( beanProviderMock, failureHandlerHolderMock,
				threadPoolProviderMock, mappings, backends, indexManagers, engineThreadsMock, timingSourceMock );
	}

	@Test
	void close_success() {
		when( mapping1Mock.preStop( any() ) ).thenReturn( CompletableFuture.completedFuture( null ) );
		when( mapping2Mock.preStop( any() ) ).thenReturn( CompletableFuture.completedFuture( null ) );
		when( indexManager1Mock.preStop() ).thenReturn( CompletableFuture.completedFuture( null ) );
		when( indexManager2Mock.preStop() ).thenReturn( CompletableFuture.completedFuture( null ) );
		when( backend1Mock.preStop() ).thenReturn( CompletableFuture.completedFuture( null ) );
		when( backend2Mock.preStop() ).thenReturn( CompletableFuture.completedFuture( null ) );

		searchIntegration.close();

		Object[] mocks = {
				mapping1Mock, mapping2Mock, indexManager1Mock, indexManager2Mock,
				backend1Mock, backend2Mock, threadPoolProviderMock, failureHandlerHolderMock, beanProviderMock,
				engineThreadsMock, timingSourceMock
		};

		InOrder inOrder = Mockito.inOrder( mocks );

		// Mappings must be closed first
		inOrder.verify( mapping1Mock ).preStop( any() );
		inOrder.verify( mapping2Mock ).preStop( any() );
		inOrder.verify( mapping1Mock ).stop();
		inOrder.verify( mapping2Mock ).stop();

		// Then index managers
		inOrder.verify( indexManager1Mock ).preStop();
		inOrder.verify( indexManager2Mock ).preStop();
		inOrder.verify( indexManager1Mock ).stop();
		inOrder.verify( indexManager2Mock ).stop();

		// Then backends
		inOrder.verify( backend1Mock ).preStop();
		inOrder.verify( backend2Mock ).preStop();
		inOrder.verify( backend1Mock ).stop();
		inOrder.verify( backend2Mock ).stop();

		// Then engine resources
		inOrder.verify( threadPoolProviderMock ).close();
		inOrder.verify( failureHandlerHolderMock ).close();
		inOrder.verify( beanProviderMock ).close();
		inOrder.verify( engineThreadsMock ).onStop();
		inOrder.verify( timingSourceMock ).stop();

		verifyNoMoreInteractions( mocks );
	}

	@Test
	void close_failure() {
		when( mapping1Mock.preStop( any() ) ).thenReturn( failedFuture( "mapping1 preStop failure" ) );
		when( mapping2Mock.preStop( any() ) ).thenThrow( exception( "mapping2 preStop failure" ) );
		doThrow( exception( "mapping1 stop failure" ) ).when( mapping1Mock ).stop();
		doThrow( exception( "mapping2 stop failure" ) ).when( mapping2Mock ).stop();

		when( indexManager1Mock.preStop() ).thenReturn( failedFuture( "indexManager1 preStop failure" ) );
		when( indexManager2Mock.preStop() ).thenThrow( exception( "indexManager2 preStop failure" ) );
		doThrow( exception( "indexManager1 stop failure" ) ).when( indexManager1Mock ).stop();
		doThrow( exception( "indexManager2 stop failure" ) ).when( indexManager2Mock ).stop();

		when( backend1Mock.preStop() ).thenReturn( failedFuture( "backend1 preStop failure" ) );
		when( backend2Mock.preStop() ).thenThrow( exception( "backend2 preStop failure" ) );
		doThrow( exception( "backend1 stop failure" ) ).when( backend1Mock ).stop();
		doThrow( exception( "backend2 stop failure" ) ).when( backend2Mock ).stop();

		assertThatThrownBy( searchIntegration::close )
				.isInstanceOf( SearchException.class )
				.hasMessageFindingMatch( "mapping 'mapping1':.*\n.*failures:.*\n.*mapping1 preStop failure\n.*mapping1 stop failure" )
				.hasMessageFindingMatch( "mapping 'mapping2':.*\n.*failures:.*\n.*mapping2 preStop failure\n.*mapping2 stop failure" )
				.hasMessageFindingMatch( "index 'index1':.*\n.*failures:.*\n.*indexManager1 preStop failure\n.*indexManager1 stop failure" )
				.hasMessageFindingMatch( "index 'index2':.*\n.*failures:.*\n.*indexManager2 preStop failure\n.*indexManager2 stop failure" )
				.hasMessageFindingMatch( "backend 'backend1':.*\n.*failures:.*\n.*backend1 preStop failure\n.*backend1 stop failure" )
				.hasMessageFindingMatch( "backend 'backend2':.*\n.*failures:.*\n.*backend2 preStop failure\n.*backend2 stop failure" );
	}

	private static MappingKey<?, ?> mappingKey(String name) {
		return (MappingKey<Object, Object>) () -> "mapping '" + name + "'";
	}

	private static <T> CompletableFuture<T> failedFuture(String message) {
		CompletableFuture<T> future = new CompletableFuture<>();
		future.completeExceptionally( exception( message ) );
		return future;
	}

	private static RuntimeException exception(String message) {
		return new RuntimeException( message );
	}

}
