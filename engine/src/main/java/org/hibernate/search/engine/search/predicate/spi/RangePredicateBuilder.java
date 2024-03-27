/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.data.Range;

public interface RangePredicateBuilder extends SearchPredicateBuilder {

	void range(Range<?> range, ValueConvert convertLowerBound, ValueConvert convertUpperBound);

}
