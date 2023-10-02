/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.typepattern.impl;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

class ArrayElementTypeMatcher implements ExtractingTypePatternMatcher {
	@Override
	public String toString() {
		return "T[] => T";
	}

	@Override
	public Optional<? extends PojoTypeModel<?>> extract(PojoTypeModel<?> typeToInspect) {
		return typeToInspect.arrayElementType();
	}
}
