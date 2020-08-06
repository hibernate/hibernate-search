/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public interface StubNextScrollWorkBehavior<H> {

	boolean hasHits();

	List<H> getRawHits();

	@SafeVarargs
	static <H> StubNextScrollWorkBehavior<H> of(H... rawHits) {
		return of( Arrays.asList( rawHits ) );
	}

	static <H> StubNextScrollWorkBehavior<H> of(List<H> rawHits) {
		return new StubNextScrollWorkBehavior<H>() {
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
