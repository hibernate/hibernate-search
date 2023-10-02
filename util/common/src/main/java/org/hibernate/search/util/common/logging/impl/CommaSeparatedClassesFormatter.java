/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.logging.impl;

import java.util.Collection;

public final class CommaSeparatedClassesFormatter {

	public static String format(Class<?>[] classes) {
		return formatHighlighted( classes, -1 );
	}

	public static String formatHighlighted(Class<?>[] classes, int position) {
		StringBuilder builder = new StringBuilder();
		for ( int i = 0; i < classes.length; i++ ) {
			if ( i > 0 ) {
				builder.append( ", " );
			}
			if ( position == i ) {
				builder.append( '*' );
			}
			builder.append( classes[i].getName() );
			if ( position == i ) {
				builder.append( '*' );
			}
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
