/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types.spi;

import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldTypeDescriptor;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.search.common.spi.SearchIndexCompositeNodeContext;
import org.hibernate.search.engine.search.common.spi.SearchIndexCompositeNodeTypeContext;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;

public abstract class AbstractIndexCompositeNodeType<
		SC extends SearchIndexScope<?>,
		N extends SearchIndexCompositeNodeContext<SC>>
		extends AbstractIndexNodeType<SC, N>
		implements IndexObjectFieldTypeDescriptor, SearchIndexCompositeNodeTypeContext<SC, N> {
	private final ObjectStructure objectStructure;

	protected AbstractIndexCompositeNodeType(Builder<SC, N> builder) {
		super( builder );
		this.objectStructure = builder.objectStructure;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "objectStructure=" + objectStructure
				+ ", traits=" + traits()
				+ "]";
	}

	@Override
	public boolean nested() {
		switch ( objectStructure ) {
			case NESTED:
				return true;
			case FLATTENED:
			case DEFAULT:
			default:
				return false;
		}
	}

	public abstract static class Builder<
			SC extends SearchIndexScope<?>,
			N extends SearchIndexCompositeNodeContext<SC>>
			extends AbstractIndexNodeType.Builder<SC, N> {
		private final ObjectStructure objectStructure;

		public Builder(ObjectStructure objectStructure) {
			this.objectStructure = objectStructure;
		}

		public abstract AbstractIndexCompositeNodeType<SC, N> build();
	}
}
