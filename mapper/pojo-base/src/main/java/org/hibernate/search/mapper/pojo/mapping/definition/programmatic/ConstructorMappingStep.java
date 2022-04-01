/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

/**
 * The step in a mapping definition where a constructor can be mapped.
 */
public interface ConstructorMappingStep {

	/**
	 * @return A DSL step where the mapping can be defined
	 * for the type hosting this constructor.
	 */
	TypeMappingStep hostingType();

}
