/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	 * @param throwable A {@link Throwable} thrown while accessing data on an entity: calling a getter, accessing a field,
	 * accessing the elements of a container, etc.
	 * @return {@code true} if this exception should be ignored
	 * and the data should be assumed empty ({@code null}, empty container, ...).
	 * {@code false} if this exception should be propagated.
	 * Note this is currently only taken into account while performing reindexing resolution.
	 */
	boolean isIgnorableDataAccessThrowable(Throwable throwable);

	/**
	 * @return A simple {@link PojoRuntimeIntrospector} that relies on the object's class to return entity types,
	 * and assumes objects are not proxyfied.
	 */
	static PojoRuntimeIntrospector simple() {
		return SimplePojoRuntimeIntrospector.get();
	}
}
