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
import org.hibernate.search.engine.backend.document.model.dsl.Projectable;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaContext;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.util.EventContext;
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

	public static Builder objectField(Builder parent, String relativeFieldName, ObjectFieldStorage storage) {
		return new Builder( parent, relativeFieldName, Type.OBJECT_FIELD )
				.objectFieldStorage( storage );
	}

	public static Builder field(Builder parent, String relativeFieldName) {
		return new Builder( parent, relativeFieldName, Type.NON_OBJECT_FIELD );
	}

	private StubIndexSchemaNode(Builder builder) {
		super( builder );
	}

	public static class Builder extends AbstractBuilder<StubIndexSchemaNode> implements IndexSchemaContext {

		private Builder(Builder parent, String relativeFieldName, Type type) {
			super( parent, relativeFieldName );
			attribute( "type", type );
		}

		@Override
		public EventContext getEventContext() {
			return EventContexts.fromIndexFieldAbsolutePath( getAbsolutePath() );
		}

		public Builder field(String relativeFieldName, Class<?> inputType) {
			return field( relativeFieldName, inputType, b -> {
			} );
		}

		public Builder field(String relativeFieldName, Class<?> inputType, Consumer<Builder> contributor) {
			Builder builder = StubIndexSchemaNode.field( this, relativeFieldName )
					.inputType( inputType );
			contributor.accept( builder );
			child( builder );
			return this;
		}

		public Builder objectField(String relativeFieldName, Consumer<Builder> contributor) {
			return objectField( relativeFieldName, ObjectFieldStorage.DEFAULT, contributor );

		}

		public Builder objectField(String relativeFieldName, ObjectFieldStorage storage, Consumer<Builder> contributor) {
			Builder builder = StubIndexSchemaNode.objectField( this, relativeFieldName, storage );
			contributor.accept( builder );
			child( builder );
			return this;
		}

		@Override
		public void child(AbstractBuilder<StubIndexSchemaNode> nodeBuilder) {
			super.child( nodeBuilder );
		}

		public Builder explicitRouting() {
			attribute( "explicitRouting", true );
			return this;
		}

		public Builder inputType(Class<?> inputType) {
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

		public Builder projectable(Projectable projectable) {
			attribute( "projectable", projectable );
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
