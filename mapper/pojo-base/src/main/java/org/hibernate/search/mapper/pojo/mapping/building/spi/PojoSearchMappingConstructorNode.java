/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
