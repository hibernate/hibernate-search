/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.impl;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;


abstract class AbstractLuceneNonFlattenedDocumentBuilder extends AbstractLuceneDocumentBuilder
		implements LuceneDocumentBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	final Document document = new Document();
	private final Map<String, EncounteredFieldStatus> fieldStatus = new HashMap<>();

	AbstractLuceneNonFlattenedDocumentBuilder(LuceneIndexModel model, LuceneIndexSchemaObjectNode schemaNode) {
		super( model, schemaNode );
	}

	@Override
	public void addField(IndexableField field) {
		document.add( field );
	}

	@Override
	public void addFieldName(String absoluteFieldPath) {
		// If the status was already ENCOUNTERED, just replace it.
		fieldStatus.put( absoluteFieldPath, EncounteredFieldStatus.ENCOUNTERED_AND_NAME_INDEXED );
	}

	@Override
	void checkNoValueYetForSingleValued(String absoluteFieldPath) {
		EncounteredFieldStatus previousValue = fieldStatus.putIfAbsent( absoluteFieldPath, EncounteredFieldStatus.ENCOUNTERED );
		if ( previousValue != null ) {
			throw log.multipleValuesForSingleValuedField( absoluteFieldPath );
		}
	}

	@Override
	void contribute(MultiTenancyStrategy multiTenancyStrategy, String tenantId, String routingKey,
			String rootId, List<Document> nestedDocuments) {
		for ( Map.Entry<String, EncounteredFieldStatus> entry : fieldStatus.entrySet() ) {
			EncounteredFieldStatus status = entry.getValue();
			if ( EncounteredFieldStatus.ENCOUNTERED_AND_NAME_INDEXED.equals( status ) ) {
				String fieldName = entry.getKey();
				document.add( MetadataFields.searchableMetadataField( MetadataFields.fieldNamesFieldName(), fieldName ) );
			}
		}

		// The following must be added to both the root document and nested documents,
		// so that delete operations delete nested documents, too.

		if ( routingKey != null ) {
			document.add( MetadataFields.searchableMetadataField(
					MetadataFields.routingKeyFieldName(), routingKey
			) );
		}

		multiTenancyStrategy.contributeToIndexedDocument( document, tenantId );

		super.contribute( multiTenancyStrategy, tenantId, routingKey, rootId, nestedDocuments );
	}

	private enum EncounteredFieldStatus {
		ENCOUNTERED,
		ENCOUNTERED_AND_NAME_INDEXED;
	}
}
