/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.typepattern.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

class NegatingTypePatternMatcher implements TypePatternMatcher {
	private final TypePatternMatcher delegate;

	NegatingTypePatternMatcher(TypePatternMatcher delegate) {
		this.delegate = delegate;
	}

	@Override
	public String toString() {
		return "not(" + delegate.toString() + ")";
	}

	@Override
	public boolean matches(PojoTypeModel<?> typeToInspect) {
		return !delegate.matches( typeToInspect );
	}
}
