/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.common.logging;

import java.lang.reflect.Type;

public class TypeFormatter {

	private final String formatted;

	public TypeFormatter(Type type) {
		if ( type instanceof Class ) {
			this.formatted = ( (Class) type ).getName();
		}
		else if ( type != null ) {
			this.formatted = type.toString();
		}
		else {
			this.formatted = null;
		}
	}

	@Override
	public String toString() {
		return formatted;
	}
}
