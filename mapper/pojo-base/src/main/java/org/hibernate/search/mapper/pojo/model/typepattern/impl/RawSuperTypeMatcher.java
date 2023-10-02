/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.typepattern.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

class RawSuperTypeMatcher implements TypePatternMatcher {
	private final PojoRawTypeModel<?> matchedRawType;

	RawSuperTypeMatcher(PojoRawTypeModel<?> matchedRawType) {
		this.matchedRawType = matchedRawType;
	}

	@Override
	public String toString() {
		return "hasRawSuperType(" + matchedRawType.name() + ")";
	}

	@Override
	public boolean matches(PojoTypeModel<?> typeToInspect) {
		return typeToInspect.rawType().isSubTypeOf( matchedRawType );
	}
}
