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

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldFilter;
import org.hibernate.search.engine.backend.document.spi.NoOpDocumentElement;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.document.Document;


abstract class AbstractLuceneDocumentBuilder implements LuceneDocumentBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final LuceneIndexModel model;
	protected final LuceneIndexSchemaObjectNode schemaNode;

	private List<LuceneFlattenedObjectDocumentBuilder> flattenedObjectDocumentBuilders;
	private List<LuceneNestedObjectDocumentBuilder> nestedObjectDocumentBuilders;

	AbstractLuceneDocumentBuilder(LuceneIndexModel model, LuceneIndexSchemaObjectNode schemaNode) {
		this.model = model;
		this.schemaNode = schemaNode;
	}

	@Override
	public <F> void addValue(IndexFieldReference<F> fieldReference, F value) {
		LuceneIndexFieldReference<F> luceneFieldReference = (LuceneIndexFieldReference<F>) fieldReference;

		LuceneIndexSchemaFieldNode<F> fieldSchemaNode = luceneFieldReference.getSchemaNode();

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
		LuceneIndexSchemaFieldNode<?> node = model.getFieldNode( absoluteFieldPath, IndexFieldFilter.ALL );

		if ( node == null ) {
			throw log.unknownFieldForIndexing( absoluteFieldPath, model.getEventContext() );
		}

		addValueUnknownType( node, value );
	}

	@Override
	public DocumentElement addObject(String relativeFieldName) {
		String absoluteFieldPath = schemaNode.absolutePath( relativeFieldName );
		LuceneIndexSchemaObjectFieldNode fieldSchemaNode = model.getObjectFieldNode( absoluteFieldPath, IndexFieldFilter.ALL );

		if ( fieldSchemaNode == null ) {
			throw log.unknownFieldForIndexing( absoluteFieldPath, model.getEventContext() );
		}

		return addObject( fieldSchemaNode, false );
	}

	@Override
	public void addNullObject(String relativeFieldName) {
		String absoluteFieldPath = schemaNode.absolutePath( relativeFieldName );
		LuceneIndexSchemaObjectFieldNode fieldSchemaNode = model.getObjectFieldNode( absoluteFieldPath, IndexFieldFilter.ALL );

		if ( fieldSchemaNode == null ) {
			throw log.unknownFieldForIndexing( absoluteFieldPath, model.getEventContext() );
		}

		addObject( fieldSchemaNode, true );
	}

	abstract void checkNoValueYetForSingleValued(String absoluteFieldPath);

	private void addNestedObjectDocumentBuilder(LuceneNestedObjectDocumentBuilder nestedObjectDocumentBuilder) {
		if ( nestedObjectDocumentBuilders == null ) {
			nestedObjectDocumentBuilders = new ArrayList<>();
		}

		nestedObjectDocumentBuilders.add( nestedObjectDocumentBuilder );
	}

	private void addFlattenedObjectDocumentBuilder(
			LuceneFlattenedObjectDocumentBuilder flattenedObjectDocumentBuilder) {
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
			for ( LuceneFlattenedObjectDocumentBuilder flattenedObjectDocumentBuilder : flattenedObjectDocumentBuilders ) {
				flattenedObjectDocumentBuilder.contribute(
						multiTenancyStrategy, tenantId, routingKey,
						rootId, nestedDocuments
				);
			}
		}

		if ( nestedObjectDocumentBuilders != null ) {
			for ( LuceneNestedObjectDocumentBuilder nestedObjectDocumentBuilder : nestedObjectDocumentBuilders ) {
				nestedObjectDocumentBuilder.contribute(
						multiTenancyStrategy, tenantId, routingKey,
						rootId, nestedDocuments
				);
			}
		}
	}

	private <F> void addValue(LuceneIndexSchemaFieldNode<F> node, F value) {
		LuceneIndexSchemaObjectNode expectedParentNode = node.parent();
		checkTreeConsistency( expectedParentNode );

		LuceneIndexFieldType<F> type = node.type();
		String absolutePath = node.absolutePath();

		if ( !node.isMultiValued() ) {
			checkNoValueYetForSingleValued( absolutePath );
		}

		type.getCodec().encode( this, absolutePath, value );
	}

	private DocumentElement addObject(LuceneIndexSchemaObjectFieldNode node, boolean nullObject) {
		LuceneIndexSchemaObjectNode expectedParentNode = node.parent();
		checkTreeConsistency( expectedParentNode );

		String absolutePath = node.absolutePath();

		if ( !node.isMultiValued() ) {
			checkNoValueYetForSingleValued( absolutePath );
		}

		if ( nullObject ) {
			return NoOpDocumentElement.get();
		}

		switch ( node.getStorage() ) {
			case NESTED:
				LuceneNestedObjectDocumentBuilder nestedDocumentBuilder =
						new LuceneNestedObjectDocumentBuilder( model, node );
				addNestedObjectDocumentBuilder( nestedDocumentBuilder );
				return nestedDocumentBuilder;
			default:
				LuceneFlattenedObjectDocumentBuilder flattenedDocumentBuilder =
						new LuceneFlattenedObjectDocumentBuilder( model, node, this );
				addFlattenedObjectDocumentBuilder( flattenedDocumentBuilder );
				return flattenedDocumentBuilder;
		}
	}

	@SuppressWarnings("unchecked") // We check types explicitly using reflection
	private void addValueUnknownType(LuceneIndexSchemaFieldNode<?> node, Object value) {
		if ( value == null ) {
			addValue( node, null );
		}
		else {
			@SuppressWarnings("rawtypes")
			LuceneIndexSchemaFieldNode typeCheckedNode =
					node.withValueType( value.getClass(), model.getEventContext() );
			addValue( typeCheckedNode, value );
		}
	}
}
