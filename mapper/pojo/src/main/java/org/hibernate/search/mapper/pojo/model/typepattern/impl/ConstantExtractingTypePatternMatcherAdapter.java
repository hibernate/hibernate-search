/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.typepattern.impl;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;

class ConstantExtractingTypePatternMatcherAdapter implements ExtractingTypePatternMatcher {
	private final TypePatternMatcher delegate;
	private final PojoGenericTypeModel<?> resultType;

	ConstantExtractingTypePatternMatcherAdapter(TypePatternMatcher delegate, PojoGenericTypeModel<?> resultType) {
		this.delegate = delegate;
		this.resultType = resultType;
	}

	@Override
	public String toString() {
		return delegate.toString() + " => " + resultType.getName();
	}

	@Override
	public Optional<? extends PojoGenericTypeModel<?>> extract(PojoGenericTypeModel<?> typeToInspect) {
		if ( delegate.matches( typeToInspect ) ) {
			return Optional.of( resultType );
		}
		else {
			return Optional.empty();
		}
	}
}
