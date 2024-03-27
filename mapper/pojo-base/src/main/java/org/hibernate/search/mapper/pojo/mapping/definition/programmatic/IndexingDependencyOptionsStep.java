/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;

/**
 * The step in an "indexing dependency" definition where optional parameters can be set.
 */
public interface IndexingDependencyOptionsStep extends PropertyMappingStep {

	/**
	 * @param reindexOnUpdate How indexed entities using the annotated property should be reindexed when the value,
	 * or any nested value, is updated.
	 * @return {@code this}, for method chaining.
	 * @see IndexingDependency#reindexOnUpdate()
	 * @see ReindexOnUpdate
	 */
	IndexingDependencyOptionsStep reindexOnUpdate(ReindexOnUpdate reindexOnUpdate);

	/**
	 * @param pojoModelPath A path to other values that are used to generate the value of this property.
	 * @return {@code this}, for method chaining.
	 * @see IndexingDependency#derivedFrom()
	 * @see PojoModelPathValueNode
	 * @see PojoModelPath#ofValue(String)
	 * @see PojoModelPath#ofValue(String, ContainerExtractorPath)
	 * @see PojoModelPath#builder()
	 */
	IndexingDependencyOptionsStep derivedFrom(PojoModelPathValueNode pojoModelPath);

	/**
	 * @param extractorName The name of the container extractor to use.
	 * @return {@code this}, for method chaining.
	 * @see IndexingDependency#extraction()
	 * @see org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors
	 */
	default IndexingDependencyOptionsStep extractor(String extractorName) {
		return extractors( ContainerExtractorPath.explicitExtractor( extractorName ) );
	}

	/**
	 * Indicates that no container extractors should be applied,
	 * not even the default ones.
	 * @return {@code this}, for method chaining.
	 * @see IndexingDependency#extraction()
	 */
	default IndexingDependencyOptionsStep noExtractors() {
		return extractors( ContainerExtractorPath.noExtractors() );
	}

	/**
	 * @param extractorPath A {@link ContainerExtractorPath}.
	 * @return {@code this}, for method chaining.
	 * @see IndexingDependency#extraction()
	 * @see ContainerExtractorPath
	 */
	IndexingDependencyOptionsStep extractors(ContainerExtractorPath extractorPath);

}
