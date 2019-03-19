/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public interface PropertyHandleFactory {

	PropertyHandle createForField(String propertyName, Field field) throws IllegalAccessException;

	PropertyHandle createForMethod(String propertyName, Method method) throws IllegalAccessException;

	/**
	 * @return A factory producing property handles that rely on {@code java.lang.reflect} to get the value of a property,
	 * i.e {@link Method#invoke(Object, Object...)} and {@link Field#get(Object)}.
	 */
	static PropertyHandleFactory usingJavaLangReflect() {
		return new MemberPropertyHandleFactory();
	}

	/**
	 * @return A factory producing property handles that rely on {@link java.lang.invoke.MethodHandle} to get the value of a property.
	 */
	static PropertyHandleFactory usingMethodHandle(MethodHandles.Lookup lookup) {
		return new MethodHandlePropertyHandleFactory( lookup );
	}

}
