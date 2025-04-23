/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.reference.aggregation;

import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface TypedAggregationFieldReference<SR, T> extends AggregationFieldReference<SR> {

	Class<T> aggregationType();

	default ValueModel valueModel() {
		return ValueModel.MAPPING;
	}
}
