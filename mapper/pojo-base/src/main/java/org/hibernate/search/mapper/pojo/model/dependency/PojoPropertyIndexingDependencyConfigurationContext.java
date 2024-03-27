/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.dependency;

import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.util.common.annotation.Incubating;

public interface PojoPropertyIndexingDependencyConfigurationContext {

	/**
	 * Declare that the bridge will only use the property value directly,
	 * and will not access any mutable property nested in the property value.
	 * <p>
	 * This is unusual, and generally only possible for bridges that are applied to immutable types ({@code String}, an enum, ...)
	 * or collections of immutable types ({@code List<String>}, ...),
	 * or that do not rely on the bridged element at all (constant bridges, bridges adding the last indexing date, ...).
	 * <p>
	 * Note that calling this method prevents from declaring any other dependency,
	 * and trying to do so will trigger an exception.
	 */
	void useRootOnly();

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
	default PojoPropertyIndexingDependencyConfigurationContext use(String pathFromBridgedPropertyToUsedValue) {
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
	default PojoPropertyIndexingDependencyConfigurationContext use(PojoModelPathValueNode pathFromBridgedPropertyToUsedValue) {
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
	default PojoPropertyIndexingDependencyConfigurationContext use(ContainerExtractorPath extractorPathFromBridgedProperty,
			String pathFromExtractedBridgedPropertyValueToUsedValue) {
		return use( extractorPathFromBridgedProperty, PojoModelPath.parse( pathFromExtractedBridgedPropertyValueToUsedValue ) );
	}

	/**
	 * Declare that the given path is read by the bridge at index time to populate the indexed document.
	 * <p>
	 * Every component of this path will be considered as a dependency,
	 * so it is not necessary to call this method for every subpath.
	 * In other words, if the path {@code "myProperty.someOtherProperty"} is declared as used,
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
	PojoPropertyIndexingDependencyConfigurationContext use(ContainerExtractorPath extractorPathFromBridgedProperty,
			PojoModelPathValueNode pathFromExtractedBridgedPropertyValueToUsedValue);

	/**
	 * Start the declaration of dependencies to properties of another entity,
	 * without specifying the path to that other entity.
	 * <p>
	 * <strong>Note:</strong> this is only useful when the path from the bridged type to that other entity
	 * cannot be easily represented, but the inverse path can.
	 * For almost all use cases, this method won't be useful and calling {@link #use(String)} will be enough.
	 *
	 * @param otherEntityType The raw type of the other entity.
	 * @param pathFromOtherEntityTypeToBridgedType The path from the other entity type to the bridged type,
	 * as a String. The string is interpreted with default value extractors: see {@link PojoModelPath#parse(String)}.
	 * Used when the other entity changes, to collect the instances that must be reindexed.
	 * @return A context allowing to declare which properties
	 * @throws org.hibernate.search.util.common.SearchException If the bridged property does not point to an entity type,
	 * or the given type is not an entity type,
	 * or the given path cannot be applied to the given entity type.
	 */
	@Incubating
	default PojoOtherEntityIndexingDependencyConfigurationContext fromOtherEntity(Class<?> otherEntityType,
			String pathFromOtherEntityTypeToBridgedType) {
		return fromOtherEntity( otherEntityType, PojoModelPath.parse( pathFromOtherEntityTypeToBridgedType ) );
	}

	/**
	 * Start the declaration of dependencies to properties of another entity,
	 * without specifying the path to that other entity.
	 * <p>
	 * <strong>Note:</strong> this is only useful when the path from the bridged type to that other entity
	 * cannot be easily represented, but the inverse path can.
	 * For almost all use cases, this method won't be useful and calling {@link #use(PojoModelPathValueNode)} will be enough.
	 *
	 * @param otherEntityType The raw type of the other entity.
	 * @param pathFromOtherEntityTypeToBridgedType The path from the other entity type to the bridged type.
	 * Used when the other entity changes, to collect the instances that must be reindexed.
	 * @return A context allowing to declare which properties
	 * @throws org.hibernate.search.util.common.SearchException If the bridged property does not point to an entity type,
	 * or the given type is not an entity type,
	 * or the given path cannot be applied to the given entity type.
	 */
	@Incubating
	default PojoOtherEntityIndexingDependencyConfigurationContext fromOtherEntity(Class<?> otherEntityType,
			PojoModelPathValueNode pathFromOtherEntityTypeToBridgedType) {
		return fromOtherEntity(
				ContainerExtractorPath.defaultExtractors(), otherEntityType, pathFromOtherEntityTypeToBridgedType
		);
	}

	/**
	 * Start the declaration of dependencies to properties of another entity,
	 * without specifying the path to that other entity.
	 * <p>
	 * <strong>Note:</strong> this is only useful when the path from the bridged type to that other entity
	 * cannot be easily represented, but the inverse path can.
	 * For almost all use cases, this method won't be useful and calling {@link #use(PojoModelPathValueNode)} will be enough.
	 *
	 * @param otherEntityType The raw type of the other entity.
	 * @param extractorPathFromBridgedProperty A container extractor path from the bridged property.
	 * @param pathFromOtherEntityTypeToBridgedPropertyExtractedType The path from the other entity type to the type of extracted values for the bridged property.
	 * Used when the other entity changes, to collect the instances that must be reindexed.
	 * @return A context allowing to declare which properties
	 * @throws org.hibernate.search.util.common.SearchException If the bridged property does not point to an entity type,
	 * or the given type is not an entity type,
	 * or the given extractor path cannot be applied to the bridged property,
	 * or the given path cannot be applied to the given entity type.
	 */
	@Incubating
	PojoOtherEntityIndexingDependencyConfigurationContext fromOtherEntity(
			ContainerExtractorPath extractorPathFromBridgedProperty,
			Class<?> otherEntityType,
			PojoModelPathValueNode pathFromOtherEntityTypeToBridgedPropertyExtractedType);

}
