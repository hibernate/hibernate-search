/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.impl;

import org.hibernate.search.engine.mapper.model.spi.IndexedTypeIdentifier;

/**
 * @author Yoann Rodiere
 */
public class PojoIndexedTypeIdentifier implements IndexedTypeIdentifier {

	private Class<?> javaType;

	public PojoIndexedTypeIdentifier(Class<?> javaType) {
		this.javaType = javaType;
	}

	public Class<?> toJavaType() {
		return javaType;
	}

	@Override
	public int hashCode() {
		return javaType.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		else if ( obj == null ) {
			return false;
		}
		else {
			//Be paranoid about type matching: converting among types on Map isn't entirely typesafe as Map#get, remove, etc.. accept Object rather than
			//restricting to the generic type of the Map.
			//This new "type system" is designed having in mind that only one type model will be used, so no mixed implementations of IndexedTypeIdentifier
			//are allowed.
			//We prefer a ClassCastException here over "return false" to spot any mistakes aggressively.
			assert PojoIndexedTypeIdentifier.class == obj.getClass() : "This should never happen. If it happens, you're mixing types in the same Map and that's a bug";
			PojoIndexedTypeIdentifier other = (PojoIndexedTypeIdentifier) obj;
			return javaType.equals( other.javaType );
		}
	}

}
