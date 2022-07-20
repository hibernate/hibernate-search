/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.reflect.spi;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.hibernate.search.util.common.AssertionFailure;

/**
 * @deprecated Use/implement {@link ValueHandleFactory} instead.
 */
@Deprecated
public interface ValueReadHandleFactory extends ValueHandleFactory {

	@Override
	default <T> ValueCreateHandle<T> createForConstructor(Constructor<T> constructor) throws IllegalAccessException {
		throw new AssertionFailure( this + " doesn't support constructor handles."
				+ " '" + getClass().getName() + " should be updated to implement createForConstructor(Constructor)." );
	}

	/**
	 * @return A factory producing value handles that rely on {@code java.lang.reflect}
	 * to get the value of a field/method,
	 * i.e {@link Method#invoke(Object, Object...)} and {@link Field#get(Object)}.
	 * @deprecated Use {@link ValueHandleFactory#usingJavaLangReflect()} instead.
	 */
	@Deprecated
	static ValueReadHandleFactory usingJavaLangReflect() {
		return new MemberValueHandleFactory();
	}

	/**
	 * @param lookup A lookup with sufficient access rights to access all relevant fields and methods.
	 * @return A factory producing value handles that rely on {@link java.lang.invoke.MethodHandle}
	 * to get the value of a field/method.
	 * @deprecated Use {@link ValueHandleFactory#usingMethodHandle(MethodHandles.Lookup)} instead.
	 */
	@Deprecated
	static ValueReadHandleFactory usingMethodHandle(MethodHandles.Lookup lookup) {
		return new MethodHandleValueHandleFactory( lookup );
	}
}
