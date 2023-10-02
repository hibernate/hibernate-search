/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.logging.spi;

import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;

public final class MappableTypeModelFormatter {

	private final MappableTypeModel typeModel;

	public MappableTypeModelFormatter(MappableTypeModel typeModel) {
		this.typeModel = typeModel;
	}

	@Override
	public String toString() {
		return typeModel != null ? typeModel.name() : "null";
	}
}
