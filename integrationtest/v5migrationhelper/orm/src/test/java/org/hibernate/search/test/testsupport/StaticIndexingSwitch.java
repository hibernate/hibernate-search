/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.testsupport;

import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class StaticIndexingSwitch implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback {

	private static StaticIndexingSwitch activeInstance = null;

	private boolean enabled = true;

	public void enable(boolean enabled) {
		this.enabled = enabled;
	}

	private boolean enabled() {
		return enabled;
	}

	private void reset() {
		enabled = true;
	}

	public static StaticIndexingSwitch activeSwitch() {
		if ( activeInstance == null ) {
			throw new IllegalStateException( "Using StaticCounters without an appropriate @RegisterExtension is forbidden."
					+ " Make sure you added one (and only one)"
					+ " '@RegisterExtension public StaticIndexingSwitch indexingSwitch = new StaticIndexingSwitch()' to your test." );
		}
		return activeInstance;
	}

	@Override
	public void afterEach(ExtensionContext extensionContext) {
		activeInstance = null;
		reset();
	}

	@Override
	public void beforeAll(ExtensionContext extensionContext) {
		throw new IllegalStateException(
				"StaticIndexingSwitch is only available as nonstatic extension, i.e. @RegisterExtension StaticIndexingSwitch staticIndexingSwitch = new StaticIndexingSwitch();" );
	}

	@Override
	public void beforeEach(ExtensionContext extensionContext) {
		if ( activeInstance != null ) {
			throw new IllegalStateException( "Using StaticCounters twice in a single test is forbidden."
					+ " Make sure you added one (and only one)"
					+ " '@Rule public StaticIndexingSwitch indexingSwitch = new StaticIndexingSwitch()' to your test." );
		}
		activeInstance = StaticIndexingSwitch.this;
	}

	public static class Binder implements RoutingBinder {
		@Override
		public void bind(RoutingBindingContext context) {
			context.dependencies().useRootOnly();
			context.bridge( Object.class, new Bridge() );
		}
	}

	private static class Bridge implements RoutingBridge<Object> {
		@Override
		public void route(DocumentRoutes routes, Object entityIdentifier, Object indexedEntity,
				RoutingBridgeRouteContext context) {
			if ( activeSwitch().enabled() ) {
				routes.addRoute();
			}
			else {
				routes.notIndexed();
			}
		}

		@Override
		public void previousRoutes(DocumentRoutes routes, Object entityIdentifier, Object indexedEntity,
				RoutingBridgeRouteContext context) {
			if ( activeSwitch().enabled() ) {
				routes.addRoute();
			}
			else {
				routes.notIndexed();
			}
		}
	}
}
