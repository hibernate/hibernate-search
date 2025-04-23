/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.reference.predicate;

import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.reference.FieldReference;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface TypedPredicateFieldReference<SR, T> extends FieldReference<SR> {

	Class<T> predicateType();

	default ValueModel valueModel() {
		return ValueModel.MAPPING;
	}
}
