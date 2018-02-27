/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.typepattern.impl;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoIntrospector;

class ArrayElementTypeMatcher implements TypePatternMatcher {
	@Override
	public String toString() {
		return "T[] => T";
	}

	@Override
	public Optional<? extends PojoGenericTypeModel<?>> match(
			PojoIntrospector introspector, PojoGenericTypeModel<?> typeToMatch) {
		return typeToMatch.getArrayElementType();
	}
}
