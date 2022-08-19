/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.logging.impl;

import java.util.Collection;

public final class CommaSeparatedClassesFormatter {

	public static String format(Class<?>[] classes) {
		StringBuilder builder = new StringBuilder();
		for ( int i = 0; i < classes.length; i++ ) {
			if ( i > 0 ) {
				builder.append( ", " );
			}
			builder.append( classes[i].getName() );
		}
		return builder.toString();
	}

	private final Class<?>[] classes;

	public CommaSeparatedClassesFormatter(Collection<Class<?>> classes) {
		this.classes = classes.toArray( new Class<?>[0] );
	}

	public CommaSeparatedClassesFormatter(Class<?>[] classes) {
		this.classes = classes;
	}

	@Override
	public String toString() {
		return '[' + format( classes ) + ']';
	}
}
