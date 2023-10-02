/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test;

import org.assertj.core.api.Condition;

public final class AssertionAndAssumptionViolationFallThrough {

	private AssertionAndAssumptionViolationFallThrough() {
	}

	public static Condition<Throwable> get() {
		return new Condition<Throwable>(
				(Throwable throwable) -> {
					if ( throwable instanceof org.opentest4j.TestAbortedException ) {
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
