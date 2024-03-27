/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
