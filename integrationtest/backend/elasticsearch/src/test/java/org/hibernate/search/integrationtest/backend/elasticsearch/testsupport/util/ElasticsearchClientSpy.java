package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient.getDiscoveredClientFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonClientImplementor;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonResponse;
import org.hibernate.search.backend.elasticsearch.client.common.json.internal.spi.JsonProvider;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchClientFactory;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.execution.spi.SimpleScheduledExecutor;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;
import org.hibernate.search.util.impl.integrationtest.common.extension.CallQueue;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ElasticsearchClientSpy implements BeforeEachCallback, AfterEachCallback {
	private final AtomicInteger createdClientCount = new AtomicInteger();
	private final AtomicInteger requestCount = new AtomicInteger();
	private final CallQueue<ElasticsearchClientSubmitCall> expectations = new CallQueue<>( () -> false );

	private ElasticsearchClientSpy() {
	}

	public static ElasticsearchClientSpy create() {
		return new ElasticsearchClientSpy();
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		setup();
	}

	@Override
	public void afterEach(ExtensionContext context) {
		try {
			verifyExpectationsMet();
		}
		finally {
			resetExpectations();
			tearDown();
		}
	}

	private void setup() {
	}

	public void resetExpectations() {
		expectations.reset();
	}

	public void verifyExpectationsMet() {
		expectations.verifyExpectationsMet();
	}

	private void tearDown() {
	}

	public int getCreatedClientCount() {
		return createdClientCount.get();
	}

	public int getRequestCount() {
		return requestCount.get();
	}

	public BeanReference<ElasticsearchClientFactory> factoryReference() {
		return beanResolver -> BeanHolder.of(
				new SpyingElasticsearchClientFactory( getDiscoveredClientFactory( beanResolver ).get() ) );
	}

	public void expectNext(ElasticsearchJsonRequest request, ElasticsearchRequestAssertionMode assertionMode) {
		expectations.expectInOrder( new ElasticsearchClientSubmitCall(
				request,
				assertionMode
		) );
	}

	private class SpyingElasticsearchClientFactory implements ElasticsearchClientFactory {
		private final ElasticsearchClientFactory delegate;

		private SpyingElasticsearchClientFactory(
				ElasticsearchClientFactory delegate) {
			this.delegate = delegate;
		}

		@Override
		public ElasticsearchJsonClientImplementor create(BeanResolver beanResolver,
				ConfigurationPropertySource propertySource,
				ThreadProvider threadProvider, String threadNamePrefix, SimpleScheduledExecutor timeoutExecutorService,
				JsonProvider jsonProvider) {
			createdClientCount.incrementAndGet();
			return new SpyingElasticsearchClient( delegate.create(
					beanResolver, propertySource, threadProvider, threadNamePrefix,
					timeoutExecutorService, jsonProvider
			) );
		}
	}

	private class SpyingElasticsearchClient implements ElasticsearchJsonClientImplementor {
		private final ElasticsearchJsonClientImplementor delegate;

		private SpyingElasticsearchClient(ElasticsearchJsonClientImplementor delegate) {
			this.delegate = delegate;
		}

		@Override
		public void close() {
			delegate.close();
		}

		@SuppressWarnings("unused") // For EJC and lambda arg
		@Override
		public CompletableFuture<ElasticsearchJsonResponse> submit(ElasticsearchJsonRequest request) {
			requestCount.incrementAndGet();
			return expectations.verify(
					new ElasticsearchClientSubmitCall( request ),
					// If there was an expectation, check it is met and forward the request to the actual client
					(expectedCall, actualCall) -> {
						expectedCall.verify( actualCall );
						return () -> delegate.submit( request );
					},
					// If there wasn't any expectation, just forward the request to the actual client
					call -> delegate.submit( request )
			);
		}

		@Override
		public <T> T unwrap(Class<T> clientClass) {
			throw new UnsupportedOperationException();
		}
	}
}
