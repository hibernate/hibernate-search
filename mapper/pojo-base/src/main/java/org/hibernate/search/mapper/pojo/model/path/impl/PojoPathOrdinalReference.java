/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
