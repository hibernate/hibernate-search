/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.tree.spi;

import org.hibernate.search.engine.common.tree.TreeFilterDefinition;

public enum TreeNodeInclusion {

	/**
	 * The node is included. This is the default.
	 * <p>
	 * For index fields, this means field can be populated when indexing and can be referenced in the Search DSL.
	 */
	INCLUDED,
	/**
	 * The node is excluded. This can only happen because of {@link TreeFilterDefinition tree filters},
	 * e.g. in {@code @IndexedEmbedded}.
	 * <p>
	 * For fields, this means the field can be populated when indexing, but its values will be ignored,
	 * and the field cannot be referenced in the Search DSL;
	 * trying to reference it in the Search DSL will lead to an exception stating that the field does not exist.
	 */
	EXCLUDED;

	public TreeNodeInclusion compose(TreeNodeInclusion childInclusion) {
		return this == INCLUDED && childInclusion == INCLUDED ? INCLUDED : EXCLUDED;
	}

}
