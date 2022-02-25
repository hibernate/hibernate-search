/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
