/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model;

import java.util.Collection;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaBuildContext;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Highlightable;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinition;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubTreeNode;

/**
 * A representation of the index schema as simplified data (a tree with attributes for each node).
 * <p>
 * Used in assertions to easily compare the expected schema against the actual schema.
 */
public final class StubIndexSchemaDataNode extends StubTreeNode<StubIndexSchemaDataNode> {

	public enum Kind {
		ROOT,
		OBJECT_FIELD,
		NAMED_PREDICATE,
		VALUE_FIELD,
		OBJECT_FIELD_TEMPLATE,
		VALUE_FIELD_TEMPLATE
	}

	public static Builder schema() {
		return new Builder( null, null, Kind.ROOT );
	}

	public static Builder objectField(Builder parent, String relativeFieldName) {
		return new Builder( parent, relativeFieldName, Kind.OBJECT_FIELD );
	}

	public static Builder field(Builder parent, String relativeFieldName) {
		return new Builder( parent, relativeFieldName, Kind.VALUE_FIELD );
	}

	public static Builder objectFieldTemplate(Builder parent, String templateName) {
		return new Builder( parent, templateName, Kind.OBJECT_FIELD_TEMPLATE );
	}

	public static Builder fieldTemplate(Builder parent, String templateName) {
		return new Builder( parent, templateName, Kind.VALUE_FIELD_TEMPLATE );
	}

	public static Builder namedPredicate(Builder parent, String relativeNamedPredicateName) {
		return new Builder( parent, relativeNamedPredicateName, Kind.NAMED_PREDICATE );
	}

	/*
	 * The following properties are purposely ignored when comparing two nodes,
	 * to make it easier to define nodes that should be matched.
	 */
	private final Kind kind;

	private StubIndexSchemaDataNode(Builder builder) {
		super( builder );
		this.kind = builder.kind;
	}

	public Kind kind() {
		return kind;
	}

	public static class Builder extends AbstractBuilder<StubIndexSchemaDataNode> implements IndexSchemaBuildContext {
		private final Kind kind;

		private Builder(Builder parent, String relativeFieldName, Kind kind) {
			super( parent, relativeFieldName );
			this.kind = kind;
			attribute( "kind", kind );
		}

		@Override
		public EventContext eventContext() {
			return EventContexts.fromIndexFieldAbsolutePath( getAbsolutePath() );
		}

		public Builder with(Consumer<Builder> consumer) {
			consumer.accept( this );
			return this;
		}

		public Builder field(String relativeFieldName, Class<?> valueClass) {
			return field( relativeFieldName, valueClass, b -> {} );
		}

		public Builder field(String relativeFieldName, Class<?> valueClass, Consumer<Builder> contributor) {
			Builder builder = StubIndexSchemaDataNode.field( this, relativeFieldName )
					.valueClass( valueClass );
			contributor.accept( builder );
			child( builder );
			return this;
		}

		public Builder namedPredicate(String relativeNamedPredicateName, Consumer<Builder> contributor) {
			Builder builder = StubIndexSchemaDataNode.namedPredicate( this, relativeNamedPredicateName );
			contributor.accept( builder );
			child( builder );
			return this;
		}

		public Builder objectField(String relativeFieldName, Consumer<Builder> contributor) {
			return objectField( relativeFieldName, ObjectStructure.DEFAULT, contributor );
		}

		public Builder objectField(String relativeFieldName, ObjectStructure structure, Consumer<Builder> contributor) {
			Builder builder = StubIndexSchemaDataNode.objectField( this, relativeFieldName );
			contributor.accept( builder );
			child( builder );
			return this;
		}

		public Builder fieldTemplate(String relativeFieldName, Class<?> inputType, Consumer<Builder> contributor) {
			Builder builder = StubIndexSchemaDataNode.fieldTemplate( this, relativeFieldName )
					.valueClass( inputType );
			contributor.accept( builder );
			child( builder );
			return this;
		}

		public Builder objectFieldTemplate(String relativeFieldName, Consumer<Builder> contributor) {
			Builder builder = StubIndexSchemaDataNode.objectFieldTemplate( this, relativeFieldName );
			contributor.accept( builder );
			child( builder );
			return this;
		}

		@Override
		public void child(AbstractBuilder<StubIndexSchemaDataNode> nodeBuilder) {
			super.child( nodeBuilder );
		}

		public Builder explicitRouting() {
			attribute( "explicitRouting", true );
			return this;
		}

		public Builder valueClass(Class<?> valueClass) {
			attribute( "valueClass", valueClass );
			return this;
		}

		public Builder objectStructure(ObjectStructure objectStructure) {
			attribute( "objectStructure", objectStructure );
			return this;
		}

		public Builder multiValued(boolean multiValued) {
			attribute( "multiValued", multiValued );
			return this;
		}

		public Builder predicateDefinition(PredicateDefinition predicateDefinition) {
			attribute( "predicateDefinition", predicateDefinition );
			return this;
		}

		public Builder analyzerName(String analyzerName) {
			attribute( "analyzerName", analyzerName );
			return this;
		}

		public Builder searchAnalyzerName(String searchAnalyzerName) {
			attribute( "searchAnalyzerName", searchAnalyzerName );
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

		public Builder aggregable(Aggregable aggregable) {
			attribute( "aggregable", aggregable );
			return this;
		}

		public Builder termVector(TermVector termVector) {
			attribute( "termVector", termVector );
			return this;
		}

		public Builder highlightable(Collection<Highlightable> highlightable) {
			attribute( "highlightable", highlightable.toArray() );
			return this;
		}

		public Builder matchingPathGlob(String pathGlob) {
			attribute( "matchingPathGlob", pathGlob );
			return this;
		}

		public Builder dimension(int dimension) {
			attribute( "dimension", dimension );
			return this;
		}

		public Builder m(int m) {
			attribute( "m", m );
			return this;
		}

		public Builder efConstruction(int efConstruction) {
			attribute( "efConstruction", efConstruction );
			return this;
		}

		public Builder vectorSimilarity(VectorSimilarity vectorSimilarity) {
			attribute( "vectorSimilarity", vectorSimilarity );
			return this;
		}

		@Override
		public StubIndexSchemaDataNode build() {
			return new StubIndexSchemaDataNode( this );
		}

	}
}
