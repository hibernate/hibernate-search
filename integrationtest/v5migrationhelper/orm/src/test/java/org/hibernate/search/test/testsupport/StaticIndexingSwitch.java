/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.testsupport;

import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class StaticIndexingSwitch
		implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback, AfterAllCallback {

	private static StaticIndexingSwitch activeInstance = null;

	private boolean enabled = true;
	private boolean callOncePerClass = false;

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
	public void afterAll(ExtensionContext extensionContext) throws Exception {
		if ( callOncePerClass ) {
			activeInstance = null;
			reset();
		}
	}

	@Override
	public void afterEach(ExtensionContext extensionContext) throws Exception {
		if ( !callOncePerClass ) {
			activeInstance = null;
			reset();
		}
	}

	@Override
	public void beforeAll(ExtensionContext extensionContext) throws Exception {
		callOncePerClass = true;
		doBefore();
	}

	@Override
	public void beforeEach(ExtensionContext extensionContext) throws Exception {
		if ( !callOncePerClass ) {
			doBefore();
		}
	}

	private void doBefore() {
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
