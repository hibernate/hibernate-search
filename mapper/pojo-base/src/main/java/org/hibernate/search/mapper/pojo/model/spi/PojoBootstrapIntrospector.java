/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

import org.hibernate.search.util.common.reflect.spi.ValueReadHandleFactory;

/**
 * A Pojo introspector used at bootstrap.
 */
public interface PojoBootstrapIntrospector {

	/**
	 * @param clazz The Java class representing the raw version of the type
	 * @param <T> The type
	 * @return A type model for the given type.
	 */
	<T> PojoRawTypeModel<T> getTypeModel(Class<T> clazz);

	/**
	 * @param clazz The Java class representing the raw version of the type
	 * @param <T> The type
	 * @return A type model for the given type.
	 */
	<T> PojoGenericTypeModel<T> getGenericTypeModel(Class<T> clazz);

	/**
	 * @return A {@link ValueReadHandleFactory} for reading annotation attributes.
	 */
	ValueReadHandleFactory getAnnotationValueReadHandleFactory();

}
