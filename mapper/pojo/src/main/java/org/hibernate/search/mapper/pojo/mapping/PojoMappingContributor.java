/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping;

import java.util.Set;

import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MappingDefinition;

/**
 * @author Yoann Rodiere
 */
public interface PojoMappingContributor<M> {

	MappingDefinition programmaticMapping();

	void annotationMapping(Set<Class<?>> classes);

	M getResult();

}
