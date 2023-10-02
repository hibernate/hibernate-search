/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.util.impl.integrationtest.common.stub.StubTreeNode;

public class StubDocumentNode extends StubTreeNode<StubDocumentNode> {

	public static Builder document() {
		return new Builder( null, null );
	}

	public static Builder object(Builder parent, String relativeFieldName) {
		return new Builder( parent, relativeFieldName );
	}

	private StubDocumentNode(Builder builder) {
		super( builder );
	}

	public static class Builder extends AbstractBuilder<StubDocumentNode> {

		private Builder(Builder parent, String relativeFieldName) {
			super( parent, relativeFieldName );
		}

		public Builder field(String relativeFieldName, Object value) {
			attribute( relativeFieldName, value );
			return this;
		}

		/*
		 * The signature is a bit weird, but that's on purpose:
		 * we want to avoid ambiguity on the call site when passing null
		 * to the other version of this method.
		 */
		public Builder field(String relativeFieldName, Object value, Object... values) {
			List<Object> list = new ArrayList<>();
			list.add( value );
			Collections.addAll( list, values );
			attribute( relativeFieldName, list.toArray() );
			return this;
		}

		public Builder objectField(String relativeFieldName, Consumer<Builder> contributor) {
			Builder childBuilder = StubDocumentNode.object( this, relativeFieldName );
			contributor.accept( childBuilder );
			child( childBuilder );
			return this;
		}

		public Builder missingObjectField(String relativeFieldName) {
			missingChild( relativeFieldName );
			return this;
		}

		@Override
		public void child(AbstractBuilder<StubDocumentNode> nodeBuilder) {
			super.child( nodeBuilder );
		}

		@Override
		public StubDocumentNode build() {
			return new StubDocumentNode( this );
		}
	}


}
