/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.impl;

/**
 * Inspired from {@code org.hibernate.util.StringHelper}, but removing
 * most methods as they are not needed for Hibernate Search.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public final class StringHelper {

	private StringHelper() { /* static methods only - hide constructor */
	}

	public static boolean isNotEmpty(final String string) {
		return string != null && string.length() > 0;
	}

	public static boolean isEmpty(final String string) {
		return string == null || string.length() == 0;
	}

}
