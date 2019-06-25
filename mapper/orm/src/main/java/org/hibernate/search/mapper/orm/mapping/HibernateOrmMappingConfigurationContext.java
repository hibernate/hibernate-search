/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingDefinitionContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingDefinitionContext;

public interface HibernateOrmMappingConfigurationContext {

	/**
	 * Start the definition of a programmatic mapping.
	 * @return A context to define the programmatic mapping.
	 */
	ProgrammaticMappingDefinitionContext programmaticMapping();

	/**
	 * Start the definition of the annotation mapping.
	 * @return A context to define the annotation mapping.
	 */
	AnnotationMappingDefinitionContext annotationMapping();

	/**
	 * Start the definition of container extractors available for use in mappings.
	 * @return A context to define container extractors.
	 */
	ContainerExtractorConfigurationContext containerExtractors();

}
