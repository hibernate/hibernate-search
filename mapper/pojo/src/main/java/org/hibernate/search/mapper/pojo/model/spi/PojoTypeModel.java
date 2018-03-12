/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.util.Optional;

public interface PojoTypeModel<T> {

	/**
	 * @return A representation of the closest parent Java {@link Class} for this type.
	 */
	PojoRawTypeModel<? super T> getRawType();

	/**
	 * @param superClassCandidate The Java Class representing the candidate supertype
	 * @return The {@link PojoTypeModel} for this class if it is a supertype of the current type,
	 * or an empty {@link Optional} if it is not.
	 */
	<U> Optional<PojoTypeModel<U>> getSuperType(Class<U> superClassCandidate);

	PojoPropertyModel<?> getProperty(String propertyName);

	PojoCaster<T> getCaster();
}
