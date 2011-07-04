/* 
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
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
	STRING {
		public FieldLoadingStrategy createLoadingStrategy(String fieldName) {
			return new StringFieldLoadingStrategy( fieldName );
		}
	},
	INT {
		public FieldLoadingStrategy createLoadingStrategy(String fieldName) {
			return new IntFieldLoadingStrategy( fieldName );
		}
	},
	LONG {
		public FieldLoadingStrategy createLoadingStrategy(String fieldName) {
			return new LongFieldLoadingStrategy( fieldName );
		}
	},
	DOUBLE {
		public FieldLoadingStrategy createLoadingStrategy(String fieldName) {
			return new DoubleFieldLoadingStrategy( fieldName );
		}
	},
	FLOAT {
		public FieldLoadingStrategy createLoadingStrategy(String fieldName) {
			return new FloatFieldLoadingStrategy( fieldName );
		}
	};

	static Map<Class<?>, FieldCacheLoadingType> fieldCacheTypes = new HashMap<Class<?>, FieldCacheLoadingType>();

	static {
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
