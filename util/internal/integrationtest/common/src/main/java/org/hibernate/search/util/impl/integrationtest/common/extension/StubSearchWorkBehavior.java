/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.extension;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public interface StubSearchWorkBehavior<H> {

	/**
	 * @return The total hit count (which may be larger than the number of hits)
	 */
	long getTotalHitCount();

	List<H> getRawHits();

	static <H> StubSearchWorkBehavior<H> empty() {
		return of( 0L );
	}

	@SafeVarargs
	static <H> StubSearchWorkBehavior<H> of(long totalHitCount, H... rawHits) {
		return of( totalHitCount, Arrays.asList( rawHits ) );
	}

	static <H> StubSearchWorkBehavior<H> of(long totalHitCount, List<H> rawHits) {
		return new StubSearchWorkBehavior<H>() {
			@Override
			public long getTotalHitCount() {
				return totalHitCount;
			}

			@Override
			public List<H> getRawHits() {
				return rawHits;
			}
		};
	}

	static <H> StubSearchWorkBehavior<H> failing(Supplier<RuntimeException> exceptionSupplier) {
		return new StubSearchWorkBehavior<H>() {
			@Override
			public long getTotalHitCount() {
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
