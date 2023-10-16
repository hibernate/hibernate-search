/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.reflect;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@SuppressWarnings("unused")
public abstract class TypeCapture<T> {
	private final Type type;

	protected TypeCapture() {
		this.type = capture();
	}

	@Override
	public String toString() {
		return type.toString();
	}

	Type capture() {
		return captureTypeArgument( TypeCapture.class, this );
	}

	public Type getType() {
		return type;
	}

	public static <T> Type captureTypeArgument(Class<T> superType, T instance) {
		Class<?> clazz = instance.getClass();
		ParameterizedType captureParameterizedType = null;
		Type genericSuperclass = clazz.getGenericSuperclass();
		if ( genericSuperclass instanceof ParameterizedType ) {
			ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
			if ( superType.equals( parameterizedType.getRawType() ) ) {
				captureParameterizedType = parameterizedType;
			}
		}
		if ( captureParameterizedType == null ) {
			throw new IllegalArgumentException( clazz + " doesn't extend or implement " + superType
					+ " directly with a type argument" );
		}
		return captureParameterizedType.getActualTypeArguments()[0];
	}

}
