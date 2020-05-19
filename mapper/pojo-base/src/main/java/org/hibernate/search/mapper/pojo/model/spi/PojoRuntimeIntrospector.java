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
	 * Detect the type of a given entity instance.
	 *
	 * @param <T> The type of the entity.
	 * @param entity An instance or proxy of T.
	 * @return The identifier of the instance's type, or of its delegate object's type if the instance is a proxy.
	 * May be {@code null} if the entity type is not known from this mapper,
	 * because it's neither indexed nor contained in an indexed type.
	 */
	<T> PojoRawTypeIdentifier<? extends T> detectEntityType(T entity);

	/**
	 * @param value the object to unproxy
	 * @return if value is a proxy, unwraps it, otherwise works as a pass-through function.
	 */
	Object unproxy(Object value);

	/**
	 * @return A simple {@link PojoRuntimeIntrospector} that relies on the object's class to return entity types,
	 * and assumes objects are not proxyfied.
	 */
	static PojoRuntimeIntrospector simple() {
		return SimplePojoRuntimeIntrospector.get();
	}
}
