/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.spi;

import java.util.Optional;

public interface PojoSearchMappingConstructorNode {

	Class<?>[] parametersJavaTypes();

	/**
	 * @return Whether this constructor is a projection constructor ({@code true}) or not ({@code false}).
	 */
	default boolean isProjectionConstructor() {
		return false;
	}

	default Optional<PojoSearchMappingMethodParameterNode> parameterNode(int index) {
		return Optional.empty();
	}

}
