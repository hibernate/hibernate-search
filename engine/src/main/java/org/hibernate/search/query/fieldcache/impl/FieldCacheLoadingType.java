/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.fieldcache.impl;

import java.util.HashMap;
import java.util.Map;

/**
 * Just an indirection to different constructors, pointing to the proper
 * FieldCache extractor per type.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public enum FieldCacheLoadingType {
	STRING_ARRAY {
		@Override
		public FieldLoadingStrategy createLoadingStrategy(String fieldName) {
			return new MultiStringFieldLoadingStrategy( fieldName );
		}
	},
	STRING {
		@Override
		public FieldLoadingStrategy createLoadingStrategy(String fieldName) {
			return new StringFieldLoadingStrategy( fieldName );
		}
	},
	BYTE_AS_SHORT {
		@Override
		public FieldLoadingStrategy createLoadingStrategy(String fieldName) {
			return new IntFieldAsByteLoadingStrategy( fieldName );
		}
	},
	INT_AS_SHORT {
		@Override
		public FieldLoadingStrategy createLoadingStrategy(String fieldName) {
			return new IntFieldAsShortLoadingStrategy( fieldName );
		}
	},
	INT {
		@Override
		public FieldLoadingStrategy createLoadingStrategy(String fieldName) {
			return new IntFieldLoadingStrategy( fieldName );
		}
	},
	LONG {
		@Override
		public FieldLoadingStrategy createLoadingStrategy(String fieldName) {
			return new LongFieldLoadingStrategy( fieldName );
		}
	},
	DOUBLE {
		@Override
		public FieldLoadingStrategy createLoadingStrategy(String fieldName) {
			return new DoubleFieldLoadingStrategy( fieldName );
		}
	},
	FLOAT {
		@Override
		public FieldLoadingStrategy createLoadingStrategy(String fieldName) {
			return new FloatFieldLoadingStrategy( fieldName );
		}
	};

	static Map<Class<?>, FieldCacheLoadingType> fieldCacheTypes = new HashMap<Class<?>, FieldCacheLoadingType>();

	static {
		fieldCacheTypes.put( String[].class, STRING_ARRAY );
		fieldCacheTypes.put( String.class, STRING );
		fieldCacheTypes.put( Integer.class, INT );
		fieldCacheTypes.put( Long.class, LONG );
		fieldCacheTypes.put( Double.class, DOUBLE );
		fieldCacheTypes.put( Float.class, FLOAT );
	}

	public abstract FieldLoadingStrategy createLoadingStrategy(String fieldName);

	public static FieldLoadingStrategy getLoadingStrategy(String fieldName, Class<?> type) {
		return fieldCacheTypes.get( type ).createLoadingStrategy( fieldName );
	}
}
