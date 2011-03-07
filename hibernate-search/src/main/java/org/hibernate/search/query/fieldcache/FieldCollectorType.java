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

package org.hibernate.search.query.fieldcache;

/**
 * Just an indirection to different constructors, pointing to the proper
 * FieldCache extractor per type.
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public enum FieldCollectorType {
	
	STRING {
		FieldLoadingStrategy<String> createLoadingStrategy(String fieldName) {
			return new StringFieldLoadingStrategy( fieldName );
		}
	}, INT {
		FieldLoadingStrategy<Integer> createLoadingStrategy(String fieldName) {
			return new IntFieldLoadingStrategy( fieldName );
		}
	}, LONG {
		FieldLoadingStrategy<Long> createLoadingStrategy(String fieldName) {
			return new LongFieldLoadingStrategy( fieldName );
		}
	}, DOUBLE {
		FieldLoadingStrategy<Double> createLoadingStrategy(String fieldName) {
			return new DoubleFieldLoadingStrategy( fieldName );
		}
	}, FLOAT {
		FieldLoadingStrategy<Float> createLoadingStrategy(String fieldName) {
			return new FloatFieldLoadingStrategy( fieldName );
		}
	};
	
	abstract <T> FieldLoadingStrategy<T> createLoadingStrategy(String fieldName);

}
