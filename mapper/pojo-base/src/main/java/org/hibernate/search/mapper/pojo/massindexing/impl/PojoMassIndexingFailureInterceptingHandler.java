/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import org.hibernate.search.mapper.pojo.loading.LoadingInterceptor;
import org.hibernate.search.mapper.pojo.logging.impl.Log;

import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.mapper.pojo.intercepting.spi.PojoInterceptingHandler;
import org.hibernate.search.mapper.pojo.intercepting.spi.PojoInterceptingInvoker;

/**
 * Wraps the execution of a {@code Runnable} in a list of {@link LoadingInterceptor}.
 */
public class PojoMassIndexingFailureInterceptingHandler<O> extends PojoMassIndexingFailureHandledRunnable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoInterceptingInvoker<O> consumer;
	private final O options;
	private final List<? extends LoadingInterceptor<? super O>> interceptors;

	PojoMassIndexingFailureInterceptingHandler(O options,
			List<? extends LoadingInterceptor<? super O>> interceptors,
			PojoMassIndexingNotifier notifier,
			PojoInterceptingInvoker<O> consumer) {
		super( notifier );
		this.options = options;
		this.interceptors = interceptors;
		this.consumer = consumer;
	}

	@Override
	public void runWithFailureHandler() {
		try {
			PojoInterceptingHandler<?> handler = new PojoInterceptingHandler<>(
					options, interceptors, consumer );
			handler.invoke();
		}
		catch (Exception e) {
			throw log.massIndexingInterceptionHandlingException( e.getMessage(), e );
		}
	}

	@Override
	protected void cleanUpOnInterruption() throws InterruptedException {
		// Do nothing
	}

	@Override
	protected void cleanUpOnFailure() throws InterruptedException {
		// Do nothing
	}
}
