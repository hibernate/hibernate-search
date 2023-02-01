/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.spi;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface PojoSearchMappingTypeNode {

	/**
	 * @return Search mapping relative to constructors.
	 */
	default Map<List<Class<?>>, ? extends PojoSearchMappingConstructorNode> constructors() {
		return Collections.emptyMap();
	}

}
