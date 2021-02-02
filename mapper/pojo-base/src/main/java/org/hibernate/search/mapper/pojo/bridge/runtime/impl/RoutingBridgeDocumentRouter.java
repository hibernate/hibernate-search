/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.route.DocumentRoute;
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;
import org.hibernate.search.mapper.pojo.route.impl.DocumentRouteImpl;
import org.hibernate.search.mapper.pojo.route.DocumentRouteDescriptor;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class RoutingBridgeDocumentRouter<E> implements DocumentRouter<E> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BeanHolder<? extends RoutingBridge<? super E>> routingBridgeHolder;

	public RoutingBridgeDocumentRouter(BeanHolder<? extends RoutingBridge<? super E>> routingBridgeHolder) {
		this.routingBridgeHolder = routingBridgeHolder;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( RoutingBridge::close, routingBridgeHolder, BeanHolder::get );
			closer.push( BeanHolder::close, routingBridgeHolder );
		}
	}

	@Override
	public DocumentRouteDescriptor currentRoute(Object entityIdentifier, Supplier<? extends E> entitySupplier,
			DocumentRoutesDescriptor providedRoutes,
			BridgeSessionContext context) {
		if ( providedRoutes != null ) {
			return NoOpDocumentRouter.INSTANCE.currentRoute( entityIdentifier, entitySupplier, providedRoutes, context );
		}
		return new CurrentDocumentRoutes()
				.currentRoute( entityIdentifier, entitySupplier.get(), context );
	}

	@Override
	public DocumentRoutesDescriptor routes(Object entityIdentifier, Supplier<? extends E> entitySupplier,
			DocumentRoutesDescriptor providedRoutes,
			BridgeSessionContext context) {
		if ( providedRoutes != null ) {
			return NoOpDocumentRouter.INSTANCE.routes( entityIdentifier, entitySupplier, providedRoutes, context );
		}
		E entity = entitySupplier.get();
		DocumentRouteDescriptor currentRoute = new CurrentDocumentRoutes()
				.currentRoute( entityIdentifier, entity, context );
		Collection<DocumentRouteDescriptor> previousRoutes = new PreviousDocumentRoutes()
				.previousDifferentRoutes( currentRoute, entityIdentifier, entity, context );
		return DocumentRoutesDescriptor.of( currentRoute, previousRoutes );
	}

	private final class CurrentDocumentRoutes implements DocumentRoutes {
		private DocumentRouteImpl currentRoute = null;
		private boolean skip = false;

		@Override
		public DocumentRoute addRoute() {
			if ( currentRoute != null ) {
				// TODO HSEARCH-3971 allow routing to multiple indexes *simultaneously*?
				throw log.multipleCurrentRoutes( routingBridgeHolder.get() );
			}
			currentRoute = new DocumentRouteImpl();
			return currentRoute;
		}

		@Override
		public void notIndexed() {
			skip = true;
		}

		DocumentRouteDescriptor currentRoute(Object entityIdentifier, E entity, BridgeSessionContext context) {
			routingBridgeHolder.get()
					.route( this, entityIdentifier, entity, context.routingBridgeRouteContext() );
			if ( skip ) {
				return null;
			}
			if ( currentRoute == null ) {
				throw log.noCurrentRoute( routingBridgeHolder.get() );
			}

			return currentRoute.toDescriptor();
		}
	}

	private final class PreviousDocumentRoutes implements DocumentRoutes {
		private List<DocumentRouteImpl> previousRoutes = null;
		private boolean skip = false;

		@Override
		public DocumentRoute addRoute() {
			if ( previousRoutes == null ) {
				previousRoutes = new ArrayList<>();
			}
			DocumentRouteImpl route = new DocumentRouteImpl();
			previousRoutes.add( route );
			return route;
		}

		@Override
		public void notIndexed() {
			skip = true;
		}

		List<DocumentRouteDescriptor> previousDifferentRoutes(DocumentRouteDescriptor currentRoute,
				Object entityIdentifier, E entity, BridgeSessionContext context) {
			routingBridgeHolder.get()
					.previousRoutes( this, entityIdentifier, entity, context.routingBridgeRouteContext() );
			if ( skip ) {
				return Collections.emptyList();
			}
			if ( previousRoutes == null || previousRoutes.isEmpty() ) {
				throw log.noPreviousRoute( routingBridgeHolder.get() );
			}

			List<DocumentRouteDescriptor> result = new ArrayList<>( previousRoutes.size() );
			for ( DocumentRouteImpl previousRoute : previousRoutes ) {
				DocumentRouteDescriptor descriptor = previousRoute.toDescriptor();
				if ( descriptor.equals( currentRoute ) ) {
					// Exclude previous routes that are the same as the current one
					continue;
				}
				result.add( descriptor );
			}
			return result;
		}
	}
}
