/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.reference.sort;

import org.hibernate.search.engine.search.common.ValueModel;

public interface TypedSortFieldReference<SR, T> extends SortFieldReference<SR> {

	Class<T> sortType();

	default ValueModel valueModel() {
		return ValueModel.MAPPING;
	}
}
