/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.path.impl;

/**
 * A representation of a single path ordinal
 * with a little context for easier debugging.
 *
 * @see PojoPathOrdinals
 */
public final class PojoPathOrdinalReference {
	public final int ordinal;
	public final PojoPathOrdinals ordinals;

	public PojoPathOrdinalReference(int ordinal, PojoPathOrdinals ordinals) {
		this.ordinal = ordinal;
		this.ordinals = ordinals;
	}

	@Override
	public String toString() {
		return ordinal + " (" + ordinals.toPath( ordinal ) + ")";
	}
}
