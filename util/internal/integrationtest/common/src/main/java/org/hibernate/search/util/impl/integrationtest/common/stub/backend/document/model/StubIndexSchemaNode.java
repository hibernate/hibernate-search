/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model;

import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.model.dsl.Store;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubTreeNode;

public final class StubIndexSchemaNode extends StubTreeNode<StubIndexSchemaNode> {

	private enum Type {
		ROOT,
		OBJECT_FIELD,
		NON_OBJECT_FIELD;
	}

	public static Builder schema() {
		return new Builder( null, null, Type.ROOT );
	}

	public static Builder objectField(Builder parent, String relativeName, ObjectFieldStorage storage) {
		return new Builder( parent, relativeName, Type.OBJECT_FIELD )
				.objectFieldStorage( storage );
	}

	public static Builder field(Builder parent, String relativeName, Class<?> inputType) {
		return new Builder( parent, relativeName, Type.NON_OBJECT_FIELD )
				.inputType( inputType );
	}

	private StubIndexSchemaNode(Builder builder) {
		super( builder );
	}

	public static class Builder extends StubTreeNode.Builder<StubIndexSchemaNode> {

		private Builder(Builder parent, String relativeName, Type type) {
			super( parent, relativeName );
			attribute( "type", type );
		}

		public Builder field(String relativeName, Class<?> inputType) {
			return field( relativeName, inputType, b -> {
			} );
		}

		public Builder field(String relativeName, Class<?> inputType, Consumer<Builder> contributor) {
			Builder builder = StubIndexSchemaNode.field( this, relativeName, inputType );
			contributor.accept( builder );
			child( builder );
			return this;
		}

		public Builder objectField(String relativeName, Consumer<Builder> contributor) {
			return objectField( relativeName, ObjectFieldStorage.DEFAULT, contributor );

		}

		public Builder objectField(String relativeName, ObjectFieldStorage storage, Consumer<Builder> contributor) {
			Builder builder = StubIndexSchemaNode.objectField( this, relativeName, storage );
			contributor.accept( builder );
			child( builder );
			return this;
		}

		@Override
		public void child(StubTreeNode.Builder<StubIndexSchemaNode> nodeBuilder) {
			super.child( nodeBuilder );
		}

		public Builder explicitRouting() {
			attribute( "explicitRouting", true );
			return this;
		}

		Builder inputType(Class<?> inputType) {
			attribute( "inputType", inputType );
			return this;
		}

		Builder objectFieldStorage(ObjectFieldStorage objectFieldStorage) {
			attribute( "objectFieldStorage", objectFieldStorage );
			return this;
		}

		public Builder analyzerName(String analyzerName) {
			attribute( "analyzerName", analyzerName );
			return this;
		}

		public Builder normalizerName(String normalizerName) {
			attribute( "normalizerName", normalizerName );
			return this;
		}

		public Builder store(Store store) {
			attribute( "store", store );
			return this;
		}

		public Builder sortable(Sortable sortable) {
			attribute( "sortable", sortable );
			return this;
		}

		@Override
		public StubIndexSchemaNode build() {
			return new StubIndexSchemaNode( this );
		}
	}
}
