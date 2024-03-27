/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.function.Consumer;

import org.hibernate.search.util.impl.integrationtest.common.stub.StubTreeNode;

public final class StubProjectionNode extends StubTreeNode<StubProjectionNode> {

	public static Builder root(String typeName) {
		return new Builder( null, null, typeName );
	}

	private StubProjectionNode(Builder builder) {
		super( builder );
	}

	public static class Builder extends AbstractBuilder<StubProjectionNode> {
		protected Builder(AbstractBuilder<?> parent, String innerKey, String typeName) {
			super( parent, innerKey );
			attribute( "type", typeName );
		}

		@Override
		protected void attribute(String name, Object... values) {
			super.attribute( name, values );
		}

		public StubProjectionNode.Builder inner(String innerKey, String typeName,
				Consumer<StubProjectionNode.Builder> contributor) {
			StubProjectionNode.Builder innerBuilder = new Builder( this, innerKey, typeName );
			contributor.accept( innerBuilder );
			child( innerBuilder );
			return this;
		}

		@Override
		public StubProjectionNode build() {
			return new StubProjectionNode( this );
		}
	}
}
