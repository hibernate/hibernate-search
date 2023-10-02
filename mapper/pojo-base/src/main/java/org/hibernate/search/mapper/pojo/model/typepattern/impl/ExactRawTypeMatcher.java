/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.typepattern.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

class ExactRawTypeMatcher implements TypePatternMatcher {
	private final PojoRawTypeModel<?> exactTypeToMatch;

	ExactRawTypeMatcher(PojoRawTypeModel<?> exactTypeToMatch) {
		this.exactTypeToMatch = exactTypeToMatch;
	}

	@Override
	public String toString() {
		return "hasExactRawType(" + exactTypeToMatch.name() + ")";
	}

	@Override
	public boolean matches(PojoTypeModel<?> typeToInspect) {
		PojoRawTypeModel<?> typeToMatchRawType = typeToInspect.rawType();
		return typeToInspect.rawType().isSubTypeOf( exactTypeToMatch )
				&& exactTypeToMatch.isSubTypeOf( typeToMatchRawType );
	}
}
