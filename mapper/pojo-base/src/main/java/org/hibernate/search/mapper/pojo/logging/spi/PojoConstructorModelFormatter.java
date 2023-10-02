/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.logging.spi;

import org.hibernate.search.mapper.pojo.model.spi.PojoConstructorModel;
import org.hibernate.search.util.common.logging.impl.CommaSeparatedClassesFormatter;

public final class PojoConstructorModelFormatter {

	private final PojoConstructorModel<?> constructorModel;

	public PojoConstructorModelFormatter(PojoConstructorModel<?> constructorModel) {
		this.constructorModel = constructorModel;
	}

	@Override
	public String toString() {
		return constructorModel.typeModel().name() + "("
				+ CommaSeparatedClassesFormatter.format( constructorModel.parametersJavaTypes() ) + ")";
	}
}
