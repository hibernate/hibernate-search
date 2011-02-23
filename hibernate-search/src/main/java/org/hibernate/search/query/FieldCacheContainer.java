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
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.FieldCache;

import static org.hibernate.search.util.CollectionHelper.newHashMap;

/**
 * A container wrapping around cache arrays from {@link FieldCache#DEFAULT}.
 *
 * @author Hardy Ferentschik
 */
public class FieldCacheContainer {
	private final Map<FieldName, String[]> stringCacheMap = newHashMap();
	private final Map<FieldName, byte[]> byteCacheMap = newHashMap();
	private final Map<FieldName, double[]> doubleCacheMap = newHashMap();
	private final Map<FieldName, float[]> floatCacheMap = newHashMap();
	private final Map<FieldName, int[]> intCacheMap = newHashMap();
	private final Map<FieldName, long[]> longCacheMap = newHashMap();
	private final Map<FieldName, short[]> shortCacheMap = newHashMap();

	public void storeCacheArray(IndexReader reader, FieldName fieldNameAndType, Class<?> cacheType)
			throws IOException {
		if ( String.class.equals( cacheType ) ) {
			stringCacheMap.put( fieldNameAndType, FieldCache.DEFAULT.getStrings( reader, fieldNameAndType.getName() ) );
		}
		else if ( Byte.class.equals( cacheType ) ) {
			byteCacheMap.put( fieldNameAndType, FieldCache.DEFAULT.getBytes( reader, fieldNameAndType.getName() ) );
		}
		else if ( Double.class.equals( cacheType ) ) {
			doubleCacheMap.put( fieldNameAndType, FieldCache.DEFAULT.getDoubles( reader, fieldNameAndType.getName() ) );
		}
		else if ( Float.class.equals( cacheType ) ) {
			floatCacheMap.put( fieldNameAndType, FieldCache.DEFAULT.getFloats( reader, fieldNameAndType.getName() ) );
		}
		else if ( Integer.class.equals( cacheType ) ) {
			intCacheMap.put( fieldNameAndType, FieldCache.DEFAULT.getInts( reader, fieldNameAndType.getName() ) );
		}
		else if ( Long.class.equals( cacheType ) ) {
			longCacheMap.put( fieldNameAndType, FieldCache.DEFAULT.getLongs( reader, fieldNameAndType.getName() ) );
		}
		else if ( Short.class.equals( cacheType ) ) {
			shortCacheMap.put( fieldNameAndType, FieldCache.DEFAULT.getShorts( reader, fieldNameAndType.getName() ) );
		}
		else {
			throw new IllegalArgumentException( "Unsupported cache type" );
		}
	}

	public boolean containsCacheArray(FieldName key) {
		if ( String.class.equals( key.getType() ) ) {
			return stringCacheMap.containsKey( key );
		}
		else if ( Byte.class.equals( key.getType() ) ) {
			return byteCacheMap.containsKey( key );
		}
		else if ( Double.class.equals( key.getType() ) ) {
			return doubleCacheMap.containsKey( key );
		}
		else if ( Float.class.equals( key.getType() ) ) {
			return floatCacheMap.containsKey( key );
		}
		else if ( Integer.class.equals( key.getType() ) ) {
			return intCacheMap.containsKey( key );
		}
		else if ( Long.class.equals( key.getType() ) ) {
			return longCacheMap.containsKey( key );
		}
		else if ( Short.class.equals( key.getType() ) ) {
			return shortCacheMap.containsKey( key );
		}
		return false;
	}

	public Object getCacheValue(FieldName key, int index) {
		if ( String.class.equals( key.getType() ) ) {
			String[] values = stringCacheMap.get( key );
			if ( values != null ) {
				return values[index];
			}
		}
		else if ( Byte.class.equals( key.getType() ) ) {
			byte[] values = byteCacheMap.get( key );
			if ( values != null ) {
				return values[index];
			}
		}
		else if ( Double.class.equals( key.getType() ) ) {
			double[] values = doubleCacheMap.get( key );
			if ( values != null ) {
				return values[index];
			}
		}
		else if ( Float.class.equals( key.getType() ) ) {
			float[] values = floatCacheMap.get( key );
			if ( values != null ) {
				return values[index];
			}
		}
		else if ( Integer.class.equals( key.getType() ) ) {
			int[] values = intCacheMap.get( key );
			if ( values != null ) {
				return values[index];
			}
		}
		else if ( Long.class.equals( key.getType() ) ) {
			long[] values = longCacheMap.get( key );
			if ( values != null ) {
				return values[index];
			}
		}
		else if ( Short.class.equals( key.getType() ) ) {
			short[] values = shortCacheMap.get( key );
			if ( values != null ) {
				return values[index];
			}
		}
		return null;
	}
}


