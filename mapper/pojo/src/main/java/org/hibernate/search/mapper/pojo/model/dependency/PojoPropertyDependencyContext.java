/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.dependency;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;

public interface PojoPropertyDependencyContext {

	/**
	 * Declare that the given path is read by the bridge at index time to populate the indexed document.
	 *
	 * @param pathFromBridgedPropertyToUsedValue The path from the value of the bridged property
	 * to the values used by the bridge, as a String.
	 * The string is interpreted with default value extractors: see {@link PojoModelPath#parse(String)}.
	 * @return {@code this}, for method chaining.
	 * @throws org.hibernate.search.util.common.SearchException If the given path cannot be applied to the values of the bridged property.
	 * @see #use(ContainerExtractorPath, PojoModelPathValueNode)
	 */
	default PojoPropertyDependencyContext use(String pathFromBridgedPropertyToUsedValue) {
		return use( ContainerExtractorPath.defaultExtractors(), pathFromBridgedPropertyToUsedValue );
	}

	/**
	 * Declare that the given path is read by the bridge at index time to populate the indexed document.
	 *
	 * @param pathFromBridgedPropertyToUsedValue The path from the value of the bridged property
	 * to the values used by the bridge.
	 * @return {@code this}, for method chaining.
	 * @throws org.hibernate.search.util.common.SearchException If the given extractor path cannot be applied to the bridged property,
	 * or if the given path cannot be applied to the values of the bridged property.
	 * @see #use(ContainerExtractorPath, PojoModelPathValueNode)
	 */
	default PojoPropertyDependencyContext use(PojoModelPathValueNode pathFromBridgedPropertyToUsedValue) {
		return use( ContainerExtractorPath.defaultExtractors(), pathFromBridgedPropertyToUsedValue );
	}

	/**
	 * Declare that the given path is read by the bridge at index time to populate the indexed document.
	 *
	 * @param extractorPathFromBridgedProperty A container extractor path from the bridged property.
	 * @param pathFromExtractedBridgedPropertyValueToUsedValue The path from the value of the bridged property
	 * to the values used by the bridge, as a String.
	 * The string is interpreted with default value extractors: see {@link PojoModelPath#parse(String)}.
	 * @return {@code this}, for method chaining.
	 * @throws org.hibernate.search.util.common.SearchException If the given extractor path cannot be applied to the bridged property,
	 * or if the given path cannot be applied to the values of the bridged property.
	 * @see #use(ContainerExtractorPath, PojoModelPathValueNode)
	 */
	default PojoPropertyDependencyContext use(ContainerExtractorPath extractorPathFromBridgedProperty,
			String pathFromExtractedBridgedPropertyValueToUsedValue) {
		return use( extractorPathFromBridgedProperty, PojoModelPath.parse( pathFromExtractedBridgedPropertyValueToUsedValue ) );
	}

	/**
	 * Declare that the given path is read by the bridge at index time to populate the indexed document.
	 * <p>
	 * Every component of this path will be considered as a dependency,
	 * so it is not necessary to call this method for every subpath.
	 * In other words, if the path {@code "myProperty.someOtherPropety"} is declared as used,
	 * Hibernate Search will automatically assume that {@code "myProperty"} is also used.
	 *
	 * @param extractorPathFromBridgedProperty A container extractor path from the bridged property.
	 * @param pathFromExtractedBridgedPropertyValueToUsedValue The path from the values extracted from the bridged property
	 * to the values used by the bridge.
	 * @return {@code this}, for method chaining.
	 * @throws org.hibernate.search.util.common.SearchException If the given extractor path cannot be applied to the bridged property,
	 * or if the given path cannot be applied to the values of the bridged property.
	 * @see #use(ContainerExtractorPath, PojoModelPathValueNode)
	 */
	PojoPropertyDependencyContext use(ContainerExtractorPath extractorPathFromBridgedProperty,
			PojoModelPathValueNode pathFromExtractedBridgedPropertyValueToUsedValue);

}
