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
import java.util.Locale;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

import org.hibernate.search.engine.cfg.spi.EngineSpiSettings;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Spy on the threads created by Hibernate Search.
 * <p>
 * To use this, configure a ThreadSpy instance as a {@code @Rule} in your test,
 * then set {@link EngineSpiSettings#THREAD_PROVIDER} to the result of {@link ThreadSpy#getThreadProvider()}
 * when starting Hibernate Search.
 */
public class ThreadSpy implements TestRule {
	private List<Thread> createdThreads = Collections.synchronizedList( new ArrayList<>() );

	@Override
	public Statement apply(Statement base, Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				setup();
				try {
					base.evaluate();
				}
				finally {
					tearDown();
				}
			}
		};
	}

	public BeanReference<ThreadProvider> getThreadProvider() {
		return beanResolver -> {
			BeanHolder<? extends ThreadProvider> delegateHolder =
					beanResolver.resolve( EngineSpiSettings.Defaults.THREAD_PROVIDER );
			SpyingThreadProvider spyingFactory = new SpyingThreadProvider( delegateHolder.get() );
			return BeanHolder.<ThreadProvider>of( spyingFactory )
					.withDependencyAutoClosing( delegateHolder );
		};
	}

	public List<Thread> getCreatedThreads(String stringInName) {
		return createdThreads.stream()
				.filter( t -> t.getName().toLowerCase( Locale.ROOT ).contains( stringInName.toLowerCase( Locale.ROOT ) ) )
				.collect( Collectors.toList() );
	}

	private void setup() {
	}

	private void tearDown() {
		createdThreads.clear();
	}

	private class SpyingThreadProvider implements ThreadProvider {
		private final ThreadProvider delegate;

		private SpyingThreadProvider(ThreadProvider delegate) {
			this.delegate = delegate;
		}

		@Override
		public String createThreadName(String prefix, int threadNumber) {
			return delegate.createThreadName( prefix, threadNumber );
		}

		@Override
		public ThreadFactory createThreadFactory(String prefix) {
			return new SpyingThreadFactory( delegate.createThreadFactory( prefix ) );
		}
	}

	private class SpyingThreadFactory implements ThreadFactory {
		private final ThreadFactory delegate;

		SpyingThreadFactory(ThreadFactory delegate) {
			this.delegate = delegate;
		}

		@Override
		public Thread newThread(Runnable r) {
			Thread thread = delegate.newThread( r );
			createdThreads.add( thread );
			return thread;
		}
	}
}
