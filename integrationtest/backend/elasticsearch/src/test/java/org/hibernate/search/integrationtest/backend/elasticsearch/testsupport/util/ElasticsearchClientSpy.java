/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClientFactoryImpl;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientFactory;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientImplementor;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;
import org.hibernate.search.util.impl.integrationtest.common.rule.CallQueue;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class ElasticsearchClientSpy implements TestRule {
	private final AtomicInteger createdClientCount = new AtomicInteger();
	private final AtomicInteger requestCount = new AtomicInteger();
	private final CallQueue<ElasticsearchClientSubmitCall> expectations = new CallQueue<>( new CallQueue.Settings() {
		@Override
		public boolean allowDuplicates() {
			return false;
		}
	} );

	@Override
	public Statement apply(Statement base, Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				setup();
				try {
					base.evaluate();
					verifyExpectationsMet();
				}
				finally {
					resetExpectations();
					tearDown();
				}
			}
		};
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
		return beanResolver -> BeanHolder.of( new SpyingElasticsearchClientFactory( new ElasticsearchClientFactoryImpl() ) );
	}

	public void expectNext(ElasticsearchRequest request, ElasticsearchRequestAssertionMode assertionMode) {
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
		public ElasticsearchClientImplementor create(BeanResolver beanResolver,
				ConfigurationPropertySource propertySource,
				ThreadProvider threadProvider, String threadNamePrefix, ScheduledExecutorService timeoutExecutorService,
				GsonProvider gsonProvider) {
			createdClientCount.incrementAndGet();
			return new SpyingElasticsearchClient( delegate.create(
					beanResolver, propertySource, threadProvider, threadNamePrefix,
					timeoutExecutorService, gsonProvider
			) );
		}
	}

	private class SpyingElasticsearchClient implements ElasticsearchClientImplementor {
		private final ElasticsearchClientImplementor delegate;

		private SpyingElasticsearchClient(ElasticsearchClientImplementor delegate) {
			this.delegate = delegate;
		}

		@Override
		public void close() {
			delegate.close();
		}

		@Override
		public CompletableFuture<ElasticsearchResponse> submit(ElasticsearchRequest request) {
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
