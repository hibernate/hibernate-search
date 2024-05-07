/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.reference.projection;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.common.ValueModel;

public interface TypedProjectionFieldReference<SR, T> extends ProjectionFieldReference<SR> {

	Class<T> projectionType();

	default ValueModel valueModel() {
		return ValueModel.MAPPING;
	}
}
