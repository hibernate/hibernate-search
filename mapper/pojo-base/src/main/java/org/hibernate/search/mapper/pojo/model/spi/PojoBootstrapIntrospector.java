/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.spi;

import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;

/**
 * A Pojo introspector used at bootstrap.
 */
public interface PojoBootstrapIntrospector {

	/**
	 * @param clazz The Java class representing the raw version of the type
	 * @param <T> The type
	 * @return A type model for the given type.
	 */
	<T> PojoRawTypeModel<T> typeModel(Class<T> clazz);

	/**
	 * @param name The name of the type
	 * @return A type model for the requested type.
	 */
	PojoRawTypeModel<?> typeModel(String name);

	/**
	 * @return A {@link ValueHandleFactory} for reading annotation attributes.
	 */
	ValueHandleFactory annotationValueHandleFactory();

	/**
	 * @return A {@link ValueHandleFactory} for reading annotation attributes.
	 * @deprecated Use/implement {@link #annotationValueHandleFactory()} instead.
	 */
	@Deprecated(since = "6.2")
	default org.hibernate.search.util.common.reflect.spi.ValueReadHandleFactory annotationValueReadHandleFactory() {
		return (org.hibernate.search.util.common.reflect.spi.ValueReadHandleFactory) annotationValueHandleFactory();
	}

	@Incubating
	static String noPrefix(String methodName) {
		if ( methodName.startsWith( "get" ) ) {
			return decapitalize( methodName.substring( "get".length() ) );
		}
		if ( methodName.startsWith( "is" ) ) {
			return decapitalize( methodName.substring( "is".length() ) );
		}
		// TODO: handle hasXXX ?
		return methodName;
	}

	// See conventions expressed by https://docs.oracle.com/javase/7/docs/api/java/beans/Introspector.html#decapitalize(java.lang.String)
	@Incubating
	static String decapitalize(String name) {
		if ( name != null && !name.isEmpty() ) {
			if ( name.length() > 1 && Character.isUpperCase( name.charAt( 1 ) ) ) {
				return name;
			}
			else {
				char[] chars = name.toCharArray();
				chars[0] = Character.toLowerCase( chars[0] );
				return new String( chars );
			}
		}
		else {
			return name;
		}
	}
}
