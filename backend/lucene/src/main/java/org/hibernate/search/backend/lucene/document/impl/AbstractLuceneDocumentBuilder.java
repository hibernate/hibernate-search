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

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.util.impl.common.LoggerFactory;

import org.apache.lucene.document.Document;

/**
 * @author Guillaume Smet
 */
public abstract class AbstractLuceneDocumentBuilder implements LuceneDocumentBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final LuceneIndexSchemaObjectNode schemaNode;

	private List<LuceneFlattenedObjectDocumentBuilder> flattenedObjectDocumentBuilders;

	private List<LuceneNestedObjectDocumentBuilder> nestedObjectDocumentBuilders;

	protected AbstractLuceneDocumentBuilder(LuceneIndexSchemaObjectNode schemaNode) {
		this.schemaNode = schemaNode;
	}

	@Override
	public void addNestedObjectDocumentBuilder(LuceneNestedObjectDocumentBuilder nestedObjectDocumentBuilder) {
		if ( nestedObjectDocumentBuilders == null ) {
			nestedObjectDocumentBuilders = new ArrayList<>();
		}

		nestedObjectDocumentBuilders.add( nestedObjectDocumentBuilder );
	}

	@Override
	public void addFlattenedObjectDocumentBuilder(LuceneFlattenedObjectDocumentBuilder flattenedObjectDocumentBuilder) {
		if ( flattenedObjectDocumentBuilders == null ) {
			flattenedObjectDocumentBuilders = new ArrayList<>();
		}

		flattenedObjectDocumentBuilders.add( flattenedObjectDocumentBuilder );
	}

	@Override
	public void checkTreeConsistency(LuceneIndexSchemaObjectNode expectedParentNode) {
		if ( !Objects.equals( expectedParentNode, schemaNode ) ) {
			throw log.invalidParentDocumentObjectState( expectedParentNode.getAbsolutePath(), schemaNode.getAbsolutePath() );
		}
	}

	void contribute(String rootIndexName, MultiTenancyStrategy multiTenancyStrategy, String tenantId, String rootId, Document currentDocument,
			List<Document> nestedDocuments) {
		if ( flattenedObjectDocumentBuilders != null ) {
			for ( LuceneFlattenedObjectDocumentBuilder flattenedObjectDocumentBuilder : flattenedObjectDocumentBuilders ) {
				flattenedObjectDocumentBuilder.contribute( rootIndexName, multiTenancyStrategy, tenantId, rootId, currentDocument, nestedDocuments );
			}
		}

		if ( nestedObjectDocumentBuilders != null ) {
			for ( LuceneNestedObjectDocumentBuilder nestedObjectDocumentBuilder : nestedObjectDocumentBuilders ) {
				nestedObjectDocumentBuilder.contribute( rootIndexName, multiTenancyStrategy, tenantId, rootId, currentDocument, nestedDocuments );
			}
		}
	}
}
