/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.extension;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public interface StubNextScrollWorkBehavior<H> {

	/**
	 * @return The total hit count, which may be larger than the number of {@link #getRawHits()}
	 */
	long getTotalHitCount();

	boolean hasHits();

	List<H> getRawHits();

	static <H> StubNextScrollWorkBehavior<H> of(long totalHitCount, List<H> rawHits) {
		return new StubNextScrollWorkBehavior<H>() {
			@Override
			public long getTotalHitCount() {
				return totalHitCount;
			}

			@Override
			public boolean hasHits() {
				return true;
			}

			@Override
			public List<H> getRawHits() {
				return rawHits;
			}
		};
	}

	static <H> StubNextScrollWorkBehavior<H> afterLast() {
		return new StubNextScrollWorkBehavior<H>() {
			@Override
			public long getTotalHitCount() {
				return 0;
			}

			@Override
			public boolean hasHits() {
				return false;
			}

			@Override
			public List<H> getRawHits() {
				return Collections.emptyList();
			}
		};
	}

	static <H> StubNextScrollWorkBehavior<H> failing(Supplier<RuntimeException> exceptionSupplier) {
		return new StubNextScrollWorkBehavior<H>() {
			@Override
			public long getTotalHitCount() {
				return 0;
			}

			@Override
			public boolean hasHits() {
				return fail();
			}

			@Override
			public List<H> getRawHits() {
				return fail();
			}

			private <T> T fail() {
				throw exceptionSupplier.get();
			}
		};
	}

}
