/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.document.model.spi;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexObjectFieldBuilder;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;

public enum IndexFieldFilter {

	/**
	 * Returns all fields independently from their inclusion.
	 */
	ALL {
		@Override
		public <T> T filter(T field, TreeNodeInclusion inclusion) {
			return field;
		}
	},
	/**
	 * Returns fields that are actually included in the schema.
	 */
	INCLUDED_ONLY {
		@Override
		public <T> T filter(T field, TreeNodeInclusion inclusion) {
			return TreeNodeInclusion.EXCLUDED.equals( inclusion ) ? null : field;
		}
	};

	/**
	 * @param field The field to filter.
	 * @param inclusion The inclusion of that field (see
	 * {@link IndexObjectFieldBuilder#addField(String, TreeNodeInclusion, IndexFieldType)} for example).
	 * @param <T> The type of {@code field}.
	 * @return {@code field} if it is included; {@code null} otherwise.
	 */
	public abstract <T> T filter(T field, TreeNodeInclusion inclusion);
}
