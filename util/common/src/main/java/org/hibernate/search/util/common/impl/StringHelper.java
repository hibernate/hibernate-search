/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
