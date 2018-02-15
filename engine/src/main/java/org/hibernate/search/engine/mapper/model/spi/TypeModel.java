/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.model.spi;

import java.util.stream.Stream;

public interface TypeModel {

	boolean isSubTypeOf(TypeModel other);

	Stream<? extends TypeModel> getAscendingSuperTypes();

	Stream<? extends TypeModel> getDescendingSuperTypes();

	/**
	 * @return A human-readable description of this type.
	 */
	@Override
	String toString();

	/**
	 * @return {@code true} if {@code obj} is a {@link TypeModel} referencing the exact same type
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
