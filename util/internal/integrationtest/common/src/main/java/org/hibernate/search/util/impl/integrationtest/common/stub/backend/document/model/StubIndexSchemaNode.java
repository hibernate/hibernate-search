/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model;

import java.util.function.Consumer;

import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaBuildContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubTreeNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.converter.impl.StubFieldConverter;

public final class StubIndexSchemaNode extends StubTreeNode<StubIndexSchemaNode> {

	private enum Type {
		ROOT,
		OBJECT_FIELD,
		NON_OBJECT_FIELD
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

	/*
	 * The following properties are purposely ignored when comparing two nodes,
	 * to make it easier to define nodes that should be matched.
	 */
	private final StubFieldConverter<?> converter;
	private final ToDocumentIdentifierValueConverter<?> idDslConverter;

	private StubIndexSchemaNode(Builder builder) {
		super( builder );
		this.converter = builder.converter;
		this.idDslConverter = builder.idDslConverter;
	}

	public ToDocumentIdentifierValueConverter<?> getIdDslConverter() {
		return idDslConverter;
	}

	public StubFieldConverter<?> getConverter() {
		return converter;
	}

	public static class Builder extends AbstractBuilder<StubIndexSchemaNode> implements IndexSchemaBuildContext {
		private StubFieldConverter<?> converter;
		private ToDocumentIdentifierValueConverter<?> idDslConverter;

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

		public Builder idDslConverter(ToDocumentIdentifierValueConverter<?> idDslConverter) {
			this.idDslConverter = idDslConverter;
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

		public Builder multiValued(boolean multiValued) {
			attribute( "multiValued", multiValued );
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

		public Builder norms(Norms norms) {
			attribute( "norms", norms );
			return this;
		}

		public Builder sortable(Sortable sortable) {
			attribute( "sortable", sortable );
			return this;
		}

		public <F> Builder indexNullAs(F indexNullAs) {
			attribute( "indexNullAs", indexNullAs );
			return this;
		}

		public Builder decimalScale(int decimalScale) {
			attribute( "decimalScale", decimalScale );
			return this;
		}

		public Builder defaultDecimalScale(int decimalScale) {
			attribute( "defaultDecimalScale", decimalScale );
			return this;
		}

		public Builder searchable(Searchable searchable) {
			attribute( "searchable", searchable );
			return this;
		}

		public Builder converter(StubFieldConverter<?> converter) {
			this.converter = converter;
			return this;
		}

		@Override
		public StubIndexSchemaNode build() {
			return new StubIndexSchemaNode( this );
		}
	}
}
