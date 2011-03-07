/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.query;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.FieldCache;

/**
 * A container wrapping around cache arrays from {@link FieldCache#DEFAULT}.
 *
 * @author Hardy Ferentschik
 */
public class FieldCacheContainer {
	private String[] stringCache;
	private byte[] byteCache;
	private double[] doubleCache;
	private float[] floatCache;
	private int[] intCache;
	private long[] longCache;
	private short[] shortCache;

	public void storeCacheArray(IndexReader reader, String fieldName, Class<?> cacheType)
			throws IOException {
		if ( String.class.equals( cacheType ) ) {
			stringCache = FieldCache.DEFAULT.getStrings( reader, fieldName );
		}
		else if ( Byte.class.equals( cacheType ) ) {
			byteCache = FieldCache.DEFAULT.getBytes( reader, fieldName );
		}
		else if ( Double.class.equals( cacheType ) ) {
			doubleCache = FieldCache.DEFAULT.getDoubles( reader, fieldName );
		}
		else if ( Float.class.equals( cacheType ) ) {
			floatCache = FieldCache.DEFAULT.getFloats( reader, fieldName );
		}
		else if ( Integer.class.equals( cacheType ) ) {
			intCache = FieldCache.DEFAULT.getInts( reader, fieldName );
		}
		else if ( Long.class.equals( cacheType ) ) {
			longCache = FieldCache.DEFAULT.getLongs( reader, fieldName );
		}
		else if ( Short.class.equals( cacheType ) ) {
			shortCache = FieldCache.DEFAULT.getShorts( reader, fieldName );
		}
		else {
			throw new IllegalArgumentException( "Unsupported cache type" );
		}
	}

	public boolean containsCacheArray(Class<?> type) {
		if ( String.class.equals( type ) ) {
			return stringCache != null;
		}
		else if ( Byte.class.equals( type ) ) {
			return byteCache != null;
		}
		else if ( Double.class.equals( type ) ) {
			return doubleCache != null;
		}
		else if ( Float.class.equals( type ) ) {
			return floatCache != null;
		}
		else if ( Integer.class.equals( type ) ) {
			return intCache != null;
		}
		else if ( Long.class.equals( type ) ) {
			return longCache != null;
		}
		else if ( Short.class.equals( type ) ) {
			return shortCache != null;
		}
		return false;
	}

	public Object getCacheValue(Class<?> type, int index) {
		if ( String.class.equals( type ) ) {
			if ( stringCache != null ) {
				return stringCache[index];
			}
		}
		else if ( Byte.class.equals( type ) ) {
			if ( byteCache != null ) {
				return byteCache[index];
			}
		}
		else if ( Double.class.equals( type ) ) {
			if ( doubleCache != null ) {
				return doubleCache[index];
			}
		}
		else if ( Float.class.equals( type ) ) {
			if ( floatCache != null ) {
				return floatCache[index];
			}
		}
		else if ( Integer.class.equals( type ) ) {
			if ( intCache != null ) {
				return intCache[index];
			}
		}
		else if ( Long.class.equals( type ) ) {
			if ( longCache != null ) {
				return longCache[index];
			}
		}
		else if ( Short.class.equals( type ) ) {
			if ( shortCache != null ) {
				return shortCache[index];
			}
		}
		return null;
	}
}


