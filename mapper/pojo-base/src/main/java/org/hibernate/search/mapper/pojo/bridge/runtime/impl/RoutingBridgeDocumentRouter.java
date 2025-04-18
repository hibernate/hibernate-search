/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;
import org.hibernate.search.mapper.pojo.logging.impl.MappingLog;
import org.hibernate.search.mapper.pojo.route.DocumentRoute;
import org.hibernate.search.mapper.pojo.route.DocumentRouteDescriptor;
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.route.impl.DocumentRouteImpl;
import org.hibernate.search.util.common.impl.Closer;

public final class RoutingBridgeDocumentRouter<E> implements DocumentRouter<E> {

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
		// Provided routes are ignored: they will only be used to determine previous routes.
		return new CurrentDocumentRoutes()
				.currentRoute( entityIdentifier, entitySupplier.get(), context );
	}

	@Override
	public DocumentRoutesDescriptor routes(Object entityIdentifier, Supplier<? extends E> entitySupplier,
			DocumentRoutesDescriptor providedRoutes,
			BridgeSessionContext context) {
		E entity = entitySupplier.get();
		DocumentRouteDescriptor currentRoute = new CurrentDocumentRoutes()
				.currentRoute( entityIdentifier, entity, context );
		Collection<DocumentRouteDescriptor> previousRoutes = new PreviousDocumentRoutes()
				.previousDifferentRoutes( currentRoute, entityIdentifier, entity, providedRoutes, context );
		return DocumentRoutesDescriptor.of( currentRoute, previousRoutes );
	}

	private final class CurrentDocumentRoutes implements DocumentRoutes {
		private DocumentRouteImpl currentRoute = null;
		private boolean skip = false;

		@Override
		@SuppressWarnings("resource") // For the eclipse-compiler: complains on bridge not bing closed
		public DocumentRoute addRoute() {
			if ( currentRoute != null ) {
				// TODO HSEARCH-3971 allow routing to multiple indexes *simultaneously*?
				throw MappingLog.INSTANCE.multipleCurrentRoutes( routingBridgeHolder.get() );
			}
			currentRoute = new DocumentRouteImpl();
			return currentRoute;
		}

		@Override
		public void notIndexed() {
			skip = true;
		}

		@SuppressWarnings("resource") // For the eclipse-compiler: complains on bridge not bing closed
		DocumentRouteDescriptor currentRoute(Object entityIdentifier, E entity, BridgeSessionContext context) {
			routingBridgeHolder.get()
					.route( this, entityIdentifier, entity, context.routingBridgeRouteContext() );
			if ( skip ) {
				return null;
			}
			if ( currentRoute == null ) {
				throw MappingLog.INSTANCE.noCurrentRoute( routingBridgeHolder.get() );
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

		@SuppressWarnings("resource") // For the eclipse-compiler: complains on bridge not bing closed
		Collection<DocumentRouteDescriptor> previousDifferentRoutes(DocumentRouteDescriptor currentRoute,
				Object entityIdentifier, E entity, DocumentRoutesDescriptor providedRoutes, BridgeSessionContext context) {
			routingBridgeHolder.get()
					.previousRoutes( this, entityIdentifier, entity, context.routingBridgeRouteContext() );
			if ( skip ) {
				return Collections.emptyList();
			}
			if ( previousRoutes == null || previousRoutes.isEmpty() ) {
				throw MappingLog.INSTANCE.noPreviousRoute( routingBridgeHolder.get() );
			}

			Collection<DocumentRouteDescriptor> result = new LinkedHashSet<>( previousRoutes.size() );
			if ( providedRoutes != null ) {
				// If there are any provided routes, we add them all to the previous routes
				// (including the provided "current" route, which is assumed out-of-date)
				result.addAll( providedRoutes.previousRoutes() );
				DocumentRouteDescriptor providedCurrentRoute = providedRoutes.currentRoute();
				if ( providedCurrentRoute != null ) {
					result.add( providedCurrentRoute );
				}
			}
			for ( DocumentRouteImpl previousRoute : previousRoutes ) {
				result.add( previousRoute.toDescriptor() );
			}
			// Exclude previous routes that are the same as the current one
			result.remove( currentRoute );
			return result;
		}
	}
}
