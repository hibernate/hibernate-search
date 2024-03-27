/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
