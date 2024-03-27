/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.spi;


final class SimplePojoRuntimeIntrospector implements PojoRuntimeIntrospector {

	private static final SimplePojoRuntimeIntrospector INSTANCE = new SimplePojoRuntimeIntrospector();

	public static SimplePojoRuntimeIntrospector get() {
		return INSTANCE;
	}

	private SimplePojoRuntimeIntrospector() {
	}

	@Override
	@SuppressWarnings("unchecked") // The class of an object of type T is always a Class<? extends T>
	public <T> PojoRawTypeIdentifier<? extends T> detectEntityType(T entity) {
		if ( entity == null ) {
			return null;
		}
		return PojoRawTypeIdentifier.of( (Class<? extends T>) entity.getClass() );
	}

	@Override
	public boolean isIgnorableDataAccessThrowable(Throwable throwable) {
		// Don't ignore any throwable by default.
		return false;
	}

	@Override
	public Object unproxy(Object value) {
		return value;
	}

}
