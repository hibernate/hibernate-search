/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.mapping;

import org.hibernate.search.mapper.pojo.bridge.mapping.BridgesConfigurationContext;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.util.common.annotation.Incubating;

public interface HibernateOrmMappingConfigurationContext {

	/**
	 * Starts the definition of the programmatic mapping.
	 * @return A context to define the programmatic mapping.
	 */
	ProgrammaticMappingConfigurationContext programmaticMapping();

	/**
	 * Starts the definition of the annotation mapping.
	 * @return A context to define the annotation mapping.
	 */
	AnnotationMappingConfigurationContext annotationMapping();

	/**
	 * Starts the definition of container extractors available for use in mappings.
	 * @return A context to define container extractors.
	 */
	@Incubating
	ContainerExtractorConfigurationContext containerExtractors();

	/**
	 * Starts the definition of bridges to apply by default in mappings.
	 * @return A context to define default bridges.
	 */
	BridgesConfigurationContext bridges();

}
