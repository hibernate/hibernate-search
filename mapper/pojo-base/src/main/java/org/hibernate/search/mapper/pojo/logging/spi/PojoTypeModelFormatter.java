/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.logging.spi;

import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

public final class PojoTypeModelFormatter {

	private final PojoTypeModel<?> typeModel;

	public PojoTypeModelFormatter(PojoTypeModel<?> typeModel) {
		this.typeModel = typeModel;
	}

	@Override
	public String toString() {
		return typeModel.name();
	}
}
