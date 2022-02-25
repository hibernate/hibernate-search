/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.typepattern.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

class AndTypePatternMatcher implements TypePatternMatcher {
	private final TypePatternMatcher left;
	private final TypePatternMatcher right;

	AndTypePatternMatcher(TypePatternMatcher left, TypePatternMatcher right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public String toString() {
		return "and(" + left.toString() + ", " + right.toString() + ")";
	}

	@Override
	public boolean matches(PojoTypeModel<?> typeToInspect) {
		return left.matches( typeToInspect ) && right.matches( typeToInspect );
	}
}
