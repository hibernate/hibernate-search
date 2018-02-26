/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

/**
 * @author Yoann Rodiere
 */
public interface PojoIntrospector {

	/**
	 * @param clazz The Java class representing the raw version of the type
	 * @param <T> The type
	 * @return A type model for the given type.
	 */
	<T> PojoRawTypeModel<T> getTypeModel(Class<T> clazz);

	/**
	 * @param clazz The Java class representing the raw version of the type
	 * @return A type model for the given type.
	 */
	<T> PojoGenericTypeModel<T> getGenericTypeModel(Class<T> clazz);

	/**
	 * @param <T> the type of the entity
	 * @param entity an instance or proxy of T
	 * @return the class from the instance, or the underlying class as a proxy.
	 */
	<T> Class<? extends T> getClass(T entity);

}
