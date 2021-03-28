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

public class PojoInterceptingHandler<C> {

	private final C loadingContext;
	private final String tenantId;
	private final List<LoadingInterceptor<C>> interceptors;
	private final PojoInterceptingInvoker<C> loadingProcess;

	public PojoInterceptingHandler(C loadingContext, String tenantId,
			List<LoadingInterceptor<C>> interceptors,
			PojoInterceptingInvoker<C> loadingProcess) {
		this.loadingContext = loadingContext;
		this.tenantId = tenantId;
		this.interceptors = interceptors;
		this.loadingProcess = loadingProcess;
	}

	public void invoke() throws Exception {
		Iterator<LoadingInterceptor<C>> iterator = interceptors.iterator();
		List<LoadingInvocationInterceptor> handlers = new ArrayList<>();

		LoadingInvocationContext<C> ictx = new PojoInvocationContext( (nctx, next) -> {
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

	private class PojoInvocationContext implements LoadingInvocationContext<C> {
		Map<Class<?>, Object> contextData = new HashMap<>();
		InvokationContextConsumer<C> consumer;

		public PojoInvocationContext(InvokationContextConsumer<C> consumer) {
			this.consumer = consumer;
		}

		@Override
		public C options() {
			return loadingContext;
		}

		@Override
		public String tenantId() {
			return tenantId;
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

	interface InvokationContextConsumer<C> {
		void invoke(LoadingInvocationContext<C> nctx, LoadingInvocationInterceptor next) throws Exception;
	}

}
