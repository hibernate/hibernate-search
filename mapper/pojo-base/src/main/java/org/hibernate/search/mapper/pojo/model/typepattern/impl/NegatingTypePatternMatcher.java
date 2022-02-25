/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
