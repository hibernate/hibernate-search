/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.rule;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Rule useful when mocks are not an option, for instance because objects are instantiated using reflection.
 * This rule ensures static counters are set to zero before the test,
 * allows to increment them from static methods, and allows to check the counters from the rule itself.
 */
public final class StaticCounters implements TestRule {

	private static StaticCounters dummyInstance = new StaticCounters();
	private static StaticCounters activeInstance = null;

	public static final class Key {
		private Key() {
		}
	}

	public static Key createKey() {
		return new Key();
	}

	/**
	 * @return A {@link StaticCounters} instance for use by stubs and mocks.
	 * May be a dummy instance if no test is currently using static counters.
	 */
	public static StaticCounters get() {
		if ( activeInstance != null ) {
			return activeInstance;
		}
		else {
			return dummyInstance;
		}
	}

	private final Map<Key, Integer> counters = new ConcurrentHashMap<>();

	@Override
	public Statement apply(Statement base, Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				counters.clear();
				if ( activeInstance != null ) {
					throw new IllegalStateException( "Using StaticCounters twice in a single test is forbidden."
							+ " Make sure you added one (and only one)"
							+ " '@Rule public StaticCounter counters = new StaticCounters()' to your test." );
				}
				activeInstance = StaticCounters.this;
				try {
					base.evaluate();
				}
				finally {
					activeInstance = null;
					counters.clear();
				}
			}
		};
	}

	public void increment(Key key) {
		counters.merge( key, 1, (left, right) -> left + right );
	}

	public int get(Key key) {
		if ( activeInstance == null ) {
			throw new IllegalStateException( "Checking StaticCounters outside of a test is forbidden."
					+ " Make sure you added one (and only one)"
					+ " '@Rule public StaticCounter counters = new StaticCounters()' to your test." );
		}
		return counters.computeIfAbsent( key, ignored -> 0 );
	}
}
