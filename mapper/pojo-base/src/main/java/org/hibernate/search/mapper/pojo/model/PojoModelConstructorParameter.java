/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBinder;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A model element representing a constructor parameter to bind.
 *
 * @see ProjectionBinder
 */
@Incubating
public interface PojoModelConstructorParameter extends PojoModelElement {

	/**
	 * @return An optional containing the name of this constructor parameter,
	 * or an empty optional if it's not available (e.g. if the class was compiled without the `-parameters` compiler flag).
	 */
	Optional<String> name();

	/**
	 * @return The {@link Class} representing the raw type of this constructor parameter.
	 */
	Class<?> rawType();

}
