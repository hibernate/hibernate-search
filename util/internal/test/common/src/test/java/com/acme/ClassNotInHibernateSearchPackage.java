/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package com.acme;

import java.util.function.Supplier;

public final class ClassNotInHibernateSearchPackage {
	private ClassNotInHibernateSearchPackage() {
	}

	public static <T> T call(Supplier<T> supplier) {
		return supplier.get();
	}
}
