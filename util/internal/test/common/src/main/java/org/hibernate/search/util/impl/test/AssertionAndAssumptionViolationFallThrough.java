/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test;

import org.assertj.core.api.Condition;

public final class AssertionAndAssumptionViolationFallThrough {

	private AssertionAndAssumptionViolationFallThrough() {
	}

	public static Condition<Throwable> get() {
		return new Condition<Throwable>(
				(Throwable throwable) -> {
					if ( throwable instanceof org.junit.internal.AssumptionViolatedException ) {
						throw (RuntimeException) throwable;
					}
					if ( throwable instanceof AssertionError ) {
						throw (Error) throwable;
					}
					return true;
				},
				"Always true: this is a hack to have assertions violations and assumption violations"
						+ " fall through in AssertJ assertions on throwables"
		);
	}

}
