/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.search.backend.lucene.document.model.impl.AbstractLuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaValueFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexValueFieldType;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldFilter;
import org.hibernate.search.engine.backend.document.spi.NoOpDocumentElement;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.document.Document;


abstract class AbstractLuceneDocumentElementBuilder implements DocumentElement {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final LuceneIndexModel model;
	protected final LuceneIndexSchemaObjectNode schemaNode;
	protected final LuceneDocumentContentImpl documentContent;

	private List<LuceneFlattenedObjectFieldBuilder> flattenedObjectDocumentBuilders;
	private List<LuceneNestedObjectFieldBuilder> nestedObjectDocumentBuilders;

	AbstractLuceneDocumentElementBuilder(LuceneIndexModel model, LuceneIndexSchemaObjectNode schemaNode,
			LuceneDocumentContentImpl documentContent) {
		this.model = model;
		this.schemaNode = schemaNode;
		this.documentContent = documentContent;
	}

	@Override
	public <F> void addValue(IndexFieldReference<F> fieldReference, F value) {
		LuceneIndexFieldReference<F> luceneFieldReference = (LuceneIndexFieldReference<F>) fieldReference;

		LuceneIndexSchemaValueFieldNode<F> fieldSchemaNode = luceneFieldReference.getSchemaNode();

		addValue( fieldSchemaNode, value );
	}

	@Override
	public DocumentElement addObject(IndexObjectFieldReference fieldReference) {
		LuceneIndexObjectFieldReference luceneFieldReference = (LuceneIndexObjectFieldReference) fieldReference;

		LuceneIndexSchemaObjectFieldNode fieldSchemaNode = luceneFieldReference.getSchemaNode();

		return addObject( fieldSchemaNode, false );
	}

	@Override
	public void addNullObject(IndexObjectFieldReference fieldReference) {
		LuceneIndexObjectFieldReference luceneFieldReference = (LuceneIndexObjectFieldReference) fieldReference;

		LuceneIndexSchemaObjectFieldNode fieldSchemaNode = luceneFieldReference.getSchemaNode();

		addObject( fieldSchemaNode, true );
	}

	@Override
	public void addValue(String relativeFieldName, Object value) {
		String absoluteFieldPath = schemaNode.absolutePath( relativeFieldName );
		AbstractLuceneIndexSchemaFieldNode node = model.fieldOrNull( absoluteFieldPath, IndexFieldFilter.ALL );

		if ( node == null ) {
			throw log.unknownFieldForIndexing( absoluteFieldPath, model.getEventContext() );
		}

		addValueUnknownType( node.toValueField(), value );
	}

	@Override
	public DocumentElement addObject(String relativeFieldName) {
		String absoluteFieldPath = schemaNode.absolutePath( relativeFieldName );
		AbstractLuceneIndexSchemaFieldNode fieldSchemaNode = model.fieldOrNull( absoluteFieldPath, IndexFieldFilter.ALL );

		if ( fieldSchemaNode == null ) {
			throw log.unknownFieldForIndexing( absoluteFieldPath, model.getEventContext() );
		}

		return addObject( fieldSchemaNode.toObjectField(), false );
	}

	@Override
	public void addNullObject(String relativeFieldName) {
		String absoluteFieldPath = schemaNode.absolutePath( relativeFieldName );
		AbstractLuceneIndexSchemaFieldNode fieldSchemaNode = model.fieldOrNull( absoluteFieldPath, IndexFieldFilter.ALL );

		if ( fieldSchemaNode == null ) {
			throw log.unknownFieldForIndexing( absoluteFieldPath, model.getEventContext() );
		}

		addObject( fieldSchemaNode.toObjectField(), true );
	}

	void checkNoValueYetForSingleValued(String absoluteFieldPath) {
		documentContent.checkNoValueYetForSingleValued( absoluteFieldPath );
	}

	private void addNestedObjectDocumentBuilder(LuceneNestedObjectFieldBuilder nestedObjectDocumentBuilder) {
		if ( nestedObjectDocumentBuilders == null ) {
			nestedObjectDocumentBuilders = new ArrayList<>();
		}

		nestedObjectDocumentBuilders.add( nestedObjectDocumentBuilder );
	}

	private void addFlattenedObjectDocumentBuilder(
			LuceneFlattenedObjectFieldBuilder flattenedObjectDocumentBuilder) {
		if ( flattenedObjectDocumentBuilders == null ) {
			flattenedObjectDocumentBuilders = new ArrayList<>();
		}

		flattenedObjectDocumentBuilders.add( flattenedObjectDocumentBuilder );
	}

	private void checkTreeConsistency(LuceneIndexSchemaObjectNode expectedParentNode) {
		if ( !Objects.equals( expectedParentNode, schemaNode ) ) {
			throw log.invalidFieldForDocumentElement( expectedParentNode.absolutePath(), schemaNode.absolutePath() );
		}
	}

	void contribute(MultiTenancyStrategy multiTenancyStrategy, String tenantId, String routingKey,
			String rootId, List<Document> nestedDocuments) {
		if ( flattenedObjectDocumentBuilders != null ) {
			for ( LuceneFlattenedObjectFieldBuilder flattenedObjectDocumentBuilder : flattenedObjectDocumentBuilders ) {
				flattenedObjectDocumentBuilder.contribute(
						multiTenancyStrategy, tenantId, routingKey,
						rootId, nestedDocuments
				);
			}
		}

		if ( nestedObjectDocumentBuilders != null ) {
			for ( LuceneNestedObjectFieldBuilder nestedObjectDocumentBuilder : nestedObjectDocumentBuilders ) {
				nestedObjectDocumentBuilder.contribute(
						multiTenancyStrategy, tenantId, routingKey,
						rootId, nestedDocuments
				);
			}
		}
	}

	private <F> void addValue(LuceneIndexSchemaValueFieldNode<F> node, F value) {
		LuceneIndexSchemaObjectNode expectedParentNode = node.parent();
		checkTreeConsistency( expectedParentNode );

		LuceneIndexValueFieldType<F> type = node.type();
		String absolutePath = node.absolutePath();

		if ( !node.multiValued() ) {
			checkNoValueYetForSingleValued( absolutePath );
		}

		type.codec().addToDocument( documentContent, absolutePath, value );
	}

	private DocumentElement addObject(LuceneIndexSchemaObjectFieldNode node, boolean nullObject) {
		LuceneIndexSchemaObjectNode expectedParentNode = node.parent();
		checkTreeConsistency( expectedParentNode );

		String absolutePath = node.absolutePath();

		if ( !node.multiValued() ) {
			checkNoValueYetForSingleValued( absolutePath );
		}

		if ( nullObject ) {
			return NoOpDocumentElement.get();
		}

		switch ( node.structure() ) {
			case NESTED:
				LuceneNestedObjectFieldBuilder nestedDocumentBuilder =
						new LuceneNestedObjectFieldBuilder( model, node );
				addNestedObjectDocumentBuilder( nestedDocumentBuilder );
				return nestedDocumentBuilder;
			default:
				LuceneFlattenedObjectFieldBuilder flattenedDocumentBuilder =
						new LuceneFlattenedObjectFieldBuilder( model, node, documentContent );
				addFlattenedObjectDocumentBuilder( flattenedDocumentBuilder );
				return flattenedDocumentBuilder;
		}
	}

	@SuppressWarnings("unchecked") // We check types explicitly using reflection
	private void addValueUnknownType(LuceneIndexSchemaValueFieldNode<?> node, Object value) {
		if ( value == null ) {
			addValue( node, null );
		}
		else {
			@SuppressWarnings("rawtypes")
			LuceneIndexSchemaValueFieldNode typeCheckedNode =
					node.withValueType( value.getClass(), model.getEventContext() );
			addValue( typeCheckedNode, value );
		}
	}
}
