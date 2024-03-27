/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;

/**
 * The step in a "association inverse side" definition where optional parameters can be set.
 */
public interface AssociationInverseSideOptionsStep extends PropertyMappingStep {

	/**
	 * @param extractorName The name of the container extractor to use.
	 * @return {@code this}, for method chaining.
	 * @see AssociationInverseSide#extraction()
	 * @see org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors
	 */
	default AssociationInverseSideOptionsStep extractor(String extractorName) {
		return extractors( ContainerExtractorPath.explicitExtractor( extractorName ) );
	}

	/**
	 * Indicates that no container extractors should be applied,
	 * not even the default ones.
	 * @return {@code this}, for method chaining.
	 * @see AssociationInverseSide#extraction()
	 */
	default AssociationInverseSideOptionsStep noExtractors() {
		return extractors( ContainerExtractorPath.noExtractors() );
	}

	/**
	 * @param extractorPath A {@link ContainerExtractorPath}.
	 * @return {@code this}, for method chaining.
	 * @see AssociationInverseSide#extraction()
	 * @see ContainerExtractorPath
	 */
	AssociationInverseSideOptionsStep extractors(ContainerExtractorPath extractorPath);

}
