/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.dependency;

import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface PojoOtherEntityIndexingDependencyConfigurationContext {

	/**
	 * Declare that the given path is read by the bridge at index time to populate the indexed document.
	 *
	 * @param pathFromOtherEntityTypeToUsedValue The path from the entity type to the value used by the bridge,
	 * as a String. The string is interpreted with default value extractors: see {@link PojoModelPath#parse(String)}.
	 * @return {@code this}, for method chaining.
	 * @throws org.hibernate.search.util.common.SearchException If the given path cannot be applied to the entity type.
	 * @see #use(PojoModelPathValueNode)
	 */
	@Incubating
	default PojoOtherEntityIndexingDependencyConfigurationContext use(String pathFromOtherEntityTypeToUsedValue) {
		return use( PojoModelPath.parse( pathFromOtherEntityTypeToUsedValue ) );
	}

	/**
	 * Declare that the given path is read by the bridge at index time to populate the indexed document.
	 * <p>
	 * Every component of this path will be considered as a dependency,
	 * so it is not necessary to call this method for every subpath.
	 * In other words, if the path {@code "myProperty.someOtherProperty"} is declared as used,
	 * Hibernate Search will automatically assume that {@code "myProperty"} is also used.
	 *
	 * @param pathFromBridgedTypeToUsedValue The path from the entity type to the value used by the bridge.
	 * @return {@code this}, for method chaining.
	 * @throws org.hibernate.search.util.common.SearchException If the given path cannot be applied to the entity type.
	 */
	@Incubating
	PojoOtherEntityIndexingDependencyConfigurationContext use(PojoModelPathValueNode pathFromBridgedTypeToUsedValue);

}
