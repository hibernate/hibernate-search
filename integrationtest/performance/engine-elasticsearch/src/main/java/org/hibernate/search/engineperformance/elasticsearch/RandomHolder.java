/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.elasticsearch;

import java.util.Random;
import java.util.function.Supplier;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * Holds a "fake", thread-local random generator.
 * <p>
 * We just want a sequence of numbers that spreads uniformly over a large interval,
 * but we don't need cryptographically secure randomness,
 * and we want the sequence to be the same from one test run to another.
 * That's why we simply use {@link Random} and that's why we set the seed to
 * a hard-coded value.
 * <p>
 * We use one random generator per thread to avoid contention.
 */
@State(Scope.Thread)
public class RandomHolder implements Supplier<Random> {

	private static final long SEED = 3210140441369L;

	private final Random random;

	public RandomHolder() {
		random = new Random( SEED );
	}

	@Override
	public Random get() {
		return random;
	}

}
