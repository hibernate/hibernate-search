/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.document.impl;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneDocumentContent;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

public class LuceneDocumentContentImpl implements LuceneDocumentContent {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Document document = new Document();
	private final Map<String, EncounteredFieldStatus> fieldStatus = new HashMap<>();

	@Override
	public void addField(IndexableField field) {
		document.add( field );
	}

	@Override
	public void addFieldName(String absoluteFieldPath) {
		// If the status was already ENCOUNTERED, just replace it.
		fieldStatus.put( absoluteFieldPath, EncounteredFieldStatus.ENCOUNTERED_AND_NAME_INDEXED );
	}

	void checkNoValueYetForSingleValued(String absoluteFieldPath) {
		EncounteredFieldStatus previousValue = fieldStatus.putIfAbsent( absoluteFieldPath, EncounteredFieldStatus.ENCOUNTERED );
		if ( previousValue != null ) {
			throw log.multipleValuesForSingleValuedField( absoluteFieldPath );
		}
	}

	Document finalizeDocument(MultiTenancyStrategy multiTenancyStrategy, String tenantId, String routingKey) {
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

		return document;
	}

	private enum EncounteredFieldStatus {
		ENCOUNTERED,
		ENCOUNTERED_AND_NAME_INDEXED;
	}

}
