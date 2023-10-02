/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.typepattern.impl;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

class ConstantExtractingTypePatternMatcherAdapter implements ExtractingTypePatternMatcher {
	private final TypePatternMatcher delegate;
	private final PojoTypeModel<?> resultType;

	ConstantExtractingTypePatternMatcherAdapter(TypePatternMatcher delegate, PojoTypeModel<?> resultType) {
		this.delegate = delegate;
		this.resultType = resultType;
	}

	@Override
	public String toString() {
		return delegate.toString() + " => " + resultType.name();
	}

	@Override
	public Optional<? extends PojoTypeModel<?>> extract(PojoTypeModel<?> typeToInspect) {
		if ( delegate.matches( typeToInspect ) ) {
			return Optional.of( resultType );
		}
		else {
			return Optional.empty();
		}
	}
}
