/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
