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

	static PojoRuntimeIntrospector noProxy() {
		return NoProxyPojoRuntimeIntrospector.get();
	}
}
