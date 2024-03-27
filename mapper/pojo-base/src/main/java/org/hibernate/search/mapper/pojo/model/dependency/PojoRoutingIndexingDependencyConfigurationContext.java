/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.dependency;

import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;

public interface PojoRoutingIndexingDependencyConfigurationContext {

	/**
	 * Declare that the bridge will only use the indexed entity directly,
	 * and will not access any mutable property.
	 * <p>
	 * This is generally called for bridges that rely exclusively on the identifier for routing.
	 * <p>
	 * Note that calling this method prevents from declaring any other dependency,
	 * and trying to do so will trigger an exception.
	 */
	void useRootOnly();

	/**
	 * Declare that the given path is read by the bridge at indexing time to route the indexed document.
	 *
	 * @param pathFromEntityTypeToUsedValue The path from the indexed entity type to the value used by the bridge,
	 * as a String. The string is interpreted with default value extractors: see {@link PojoModelPath#parse(String)}.
	 * @return {@code this}, for method chaining.
	 * @throws org.hibernate.search.util.common.SearchException If the given path cannot be applied to the indexed entity type.
	 * @see #use(PojoModelPathValueNode)
	 */
	default PojoRoutingIndexingDependencyConfigurationContext use(String pathFromEntityTypeToUsedValue) {
		return use( PojoModelPath.parse( pathFromEntityTypeToUsedValue ) );
	}

	/**
	 * Declare that the given path is read by the bridge at indexing time to route the indexed document.
	 * <p>
	 * Every component of this path will be considered as a dependency,
	 * so it is not necessary to call this method for every subpath.
	 * In other words, if the path {@code "myProperty.someOtherProperty"} is declared as used,
	 * Hibernate Search will automatically assume that {@code "myProperty"} is also used.
	 *
	 * @param pathFromEntityTypeToUsedValue The path from the indexed entity type to the value used by the bridge.
	 * @return {@code this}, for method chaining.
	 * @throws org.hibernate.search.util.common.SearchException If the given path cannot be applied to the indexed entity type.
	 */
	PojoRoutingIndexingDependencyConfigurationContext use(PojoModelPathValueNode pathFromEntityTypeToUsedValue);

}
