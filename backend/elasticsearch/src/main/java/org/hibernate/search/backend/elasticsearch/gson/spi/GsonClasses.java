/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.gson.spi;

import java.util.Set;

public final class GsonClasses {

	private GsonClasses() {
	}

	/**
	 * @return A set of names of all classes that will be involved in GSON serialization and will require reflection support.
	 * Useful to enable reflection for these classes in GraalVM-based native images.
	 */
	public static Set<String> typesRequiringReflection() {
		return Set.of();
	}

}
