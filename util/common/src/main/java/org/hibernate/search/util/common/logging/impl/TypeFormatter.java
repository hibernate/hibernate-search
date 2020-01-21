/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.logging.impl;

import java.lang.reflect.Type;

public final class TypeFormatter {

	private final Type type;

	public TypeFormatter(Type type) {
		this.type = type;
	}

	@Override
	public String toString() {
		if ( type instanceof Class ) {
			return ( (Class<?>) type ).getName();
		}
		else if ( type != null ) {
			return type.toString();
		}
		else {
			return null;
		}
	}
}
