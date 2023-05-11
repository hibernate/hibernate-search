/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.util.Optional;

import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;

public interface PojoMethodParameterModel<T> {

	int index();

	Optional<String> name();

	PojoTypeModel<T> typeModel();

	/**
	 * @return {@code true} if this parameter is expected to receive an "enclosing instance",
	 * e.g. an instance of the enclosing class in the case of Java inner classes or method-local classes.
	 */
	boolean isEnclosingInstance();

	/**
	 * @return {@code true} if {@code obj} is a {@link MappableTypeModel} referencing the exact same type
	 * with the exact same exposed metadata.
	 */
	@Override
	boolean equals(Object obj);

	/*
	 * Note to implementors: you must override hashCode to be consistent with equals().
	 */
	@Override
	int hashCode();

}
