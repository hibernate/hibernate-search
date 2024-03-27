/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.spi;

public interface MinimumShouldMatchBuilder {
	void minimumShouldMatchNumber(int ignoreConstraintCeiling, int matchingClausesNumber);

	void minimumShouldMatchPercent(int ignoreConstraintCeiling, int matchingClausesPercent);

}
