/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.intercepting.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.hibernate.search.mapper.pojo.loading.LoadingInterceptor;
import org.hibernate.search.mapper.pojo.intercepting.LoadingInvocationContext;
import org.hibernate.search.mapper.pojo.intercepting.LoadingInvocationInterceptor;

public class PojoInterceptingHandler {

	private final List<? extends LoadingInterceptor> interceptors;
	private final PojoInterceptingInvoker loadingProcess;

	public PojoInterceptingHandler(List<? extends LoadingInterceptor> interceptors,
			PojoInterceptingInvoker loadingProcess) {
		this.interceptors = interceptors;
		this.loadingProcess = loadingProcess;
	}

	public void invoke() throws Exception {
		Iterator<? extends LoadingInterceptor> iterator = interceptors.iterator();
		List<LoadingInvocationInterceptor> handlers = new ArrayList<>();

		LoadingInvocationContext ictx = new PojoInvocationContext( (nctx, next) -> {
			if ( next != null ) {
				handlers.add( next );
			}
			if ( iterator.hasNext() ) {
				iterator.next().intercept( nctx );
			}
			else {
				loadingProcess.invoke( nctx, consumer -> runWithHandledInvocations( handlers, consumer ) );
			}
		} );

		ictx.proceed();
	}

	private void runWithHandledInvocations(List<LoadingInvocationInterceptor> handlers,
			PojoInterceptingNextConsumer finalConsumer) throws Exception {

		Iterator<LoadingInvocationInterceptor> iterator = handlers.iterator();
		invokeNextInvocations( iterator, finalConsumer );

	}

	private void invokeNextInvocations(Iterator<LoadingInvocationInterceptor> iterator,
			PojoInterceptingNextConsumer finalConsumer) throws Exception {
		if ( iterator.hasNext() ) {
			iterator.next().intercept( () -> invokeNextInvocations( iterator, finalConsumer ) );
		}
		else {
			finalConsumer.invoke();
		}
	}

	private static class PojoInvocationContext implements LoadingInvocationContext {
		Map<Class<?>, Object> contextData = new HashMap<>();
		InvokationContextConsumer consumer;

		public PojoInvocationContext(InvokationContextConsumer consumer) {
			this.consumer = consumer;
		}

		@Override
		public <T> T context(Class<T> contextType) {
			return (T) contextData.get( contextType );
		}

		@Override
		public <T> void context(Class<T> contextType, T context) {
			contextData.put( contextType, context );
		}

		@Override
		public void proceed() throws Exception {
			proceed( null );
		}

		@Override
		public void proceed(LoadingInvocationInterceptor next) throws Exception {
			consumer.invoke( this, next );
		}
	}

	interface InvokationContextConsumer {
		void invoke(LoadingInvocationContext nctx, LoadingInvocationInterceptor next) throws Exception;
	}

}
