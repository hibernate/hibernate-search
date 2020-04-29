/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.spi;

public enum IndexFieldInclusion {

	/**
	 * The field is included. This is the default.
	 * <p>
	 * The field can be populated when indexing and can be referenced in the Search DSL.
	 */
	INCLUDED,
	/**
	 * The field is excluded. This can only happen because of {@code @IndexedEmbedded} filters.
	 * <p>
	 * The field can be populated when indexing, but its values will be ignored.
	 * <p>
	 * The field cannot be referenced in the Search DSL;
	 * trying to reference it in the Search DSL will lead to an exception stating that the field does not exist.
	 */
	EXCLUDED;

	public IndexFieldInclusion compose(IndexFieldInclusion childInclusion) {
		return this == INCLUDED && childInclusion == INCLUDED ? INCLUDED : EXCLUDED;
	}

}
