/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.common;

import java.util.HashMap;
import java.util.Map;

public final class ReflectionHelper {

	private static final Map<Class<?>, Class<?>> PRIMITIVES_TO_WRAPPERS;
	static {
		Map<Class<?>, Class<?>> map = new HashMap<>();
		map.put( boolean.class, Boolean.class );
		map.put( byte.class, Byte.class );
		map.put( char.class, Character.class );
		map.put( double.class, Double.class );
		map.put( float.class, Float.class );
		map.put( int.class, Integer.class );
		map.put( long.class, Long.class );
		map.put( short.class, Short.class );
		map.put( void.class, Void.class );
		PRIMITIVES_TO_WRAPPERS = CollectionHelper.toImmutableMap( map );
	}

	private ReflectionHelper() {
		// Private, don't use.
	}

	public static Class<?> getPrimitiveWrapperType(Class<?> primitiveType) {
		if ( !primitiveType.isPrimitive() ) {
			throw new IllegalArgumentException( "Argument primitiveType must be a Class representing a primitive Java type" );
		}
		return PRIMITIVES_TO_WRAPPERS.get( primitiveType );
	}
}
