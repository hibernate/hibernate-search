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

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class StaticIndexingSwitch implements TestRule {

	private static StaticIndexingSwitch activeInstance = null;

	private boolean enabled = true;

	@Override
	public Statement apply(Statement base, Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				if ( activeInstance != null ) {
					throw new IllegalStateException( "Using StaticCounters twice in a single test is forbidden."
							+ " Make sure you added one (and only one)"
							+ " '@Rule public StaticIndexingSwitch indexingSwitch = new StaticIndexingSwitch()' to your test." );
				}
				activeInstance = StaticIndexingSwitch.this;
				try {
					base.evaluate();
				}
				finally {
					activeInstance = null;
					reset();
				}
			}
		};
	}

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
			throw new IllegalStateException( "Using StaticCounters without an appropriate @Rule is forbidden."
					+ " Make sure you added one (and only one)"
					+ " '@Rule public StaticIndexingSwitch indexingSwitch = new StaticIndexingSwitch()' to your test." );
		}
		return activeInstance;
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
