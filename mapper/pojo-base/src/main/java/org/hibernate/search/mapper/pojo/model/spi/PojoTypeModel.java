/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

public interface PojoTypeModel<T> {

	/**
	 * @return A human-readable name for this type.
	 */
	String getName();

	/**
	 * @return A representation of the closest parent Java {@link Class} for this type.
	 */
	PojoRawTypeModel<? super T> getRawType();

	PojoPropertyModel<?> getProperty(String propertyName);
}
