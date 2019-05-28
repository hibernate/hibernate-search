/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

/**
 * A Pojo introspector used at runtime.
 */
public interface PojoRuntimeIntrospector {

	/**
	 * @param <T> the type of the entity
	 * @param entity an instance or proxy of T
	 * @return the class from the instance, or the underlying class as a proxy.
	 */
	<T> Class<? extends T> getClass(T entity);

	/**
	 * @param value the object to unproxy
	 * @return if value is a proxy, unwraps it, otherwise works as a pass-through function.
	 */
	Object unproxy(Object value);

	// TODO HSEARCH-3067 also add the following as necessary
//	/**
//	 * @param <T> the type of the elements in the collection
//	 * @param value the collection to initialize
//	 * @return the initialized Collection, to be used on lazily-loading collections
//	 */
//	<T> Collection<T> initializeCollection(Collection<T> value);
//
//	/**
//	 * @param <K> key
//	 * @param <V> value
//	 * @param value the map to initialize
//	 * @return the initialized Map, to be used on lazily-loading maps
//	 */
//	<K,V> Map<K,V> initializeMap(Map<K,V> value);
//
//	/**
//	 * @param value the array to initialize
//	 * @return the initialized array, to be used on lazily-loading arrays
//	 */
//	Object[] initializeArray(Object[] value);

	static PojoRuntimeIntrospector noProxy() {
		return NoProxyPojoRuntimeIntrospector.get();
	}
}
