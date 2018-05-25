/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingDefinition;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingDefinition;

/**
 * @author Yoann Rodiere
 */
public interface PojoMappingInitiator<M> {

	ProgrammaticMappingDefinition programmaticMapping();

	AnnotationMappingDefinition annotationMapping();

	M getResult();

}
