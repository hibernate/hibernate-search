/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.extension;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Rule useful when mocks are not an option, for instance because objects are instantiated using reflection.
 * This rule ensures static counters are set to zero before the test,
 * allows to increment them from static methods, and allows to check the counters from the rule itself.
 */
public final class StaticCounters implements BeforeEachCallback, AfterEachCallback {

	private static final StaticCounters DUMMY_INSTANCE = create();
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
			return DUMMY_INSTANCE;
		}
	}

	public static StaticCounters create() {
		return new StaticCounters();
	}

	private StaticCounters() {
	}

	private final Map<Key, Integer> counters = new ConcurrentHashMap<>();

	@Override
	public void beforeEach(ExtensionContext context) {
		counters.clear();
		if ( activeInstance != null ) {
			throw new IllegalStateException( "Using StaticCounters twice in a single test is forbidden."
					+ " Make sure you added one (and only one)"
					+ " '@RegisterExtension public StaticCounter counters = StaticCounters.create()' to your test." );
		}
		activeInstance = StaticCounters.this;
	}

	@Override
	public void afterEach(ExtensionContext context) {
		activeInstance = null;
		counters.clear();
	}

	public void increment(Key key) {
		add( key, 1 );
	}

	public void add(Key key, int count) {
		counters.merge( key, count, (left, right) -> left + right );
	}

	public int get(Key key) {
		if ( activeInstance == null ) {
			throw new IllegalStateException( "Checking StaticCounters outside of a test is forbidden."
					+ " Make sure you added one (and only one)"
					+ " '@Rule public StaticCounter counters = new StaticCounters()' to your test." );
		}
		return counters.computeIfAbsent( key, ignored -> 0 );
	}

	public void clear() {
		counters.clear();
	}

}
