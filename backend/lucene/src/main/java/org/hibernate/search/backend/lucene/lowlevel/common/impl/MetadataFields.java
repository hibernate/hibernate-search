/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.common.impl;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;

public class MetadataFields {

	private static final FieldType METADATA_FIELD_TYPE_WITH_INDEX;
	private static final FieldType METADATA_FIELD_TYPE_WITH_INDEX_WITH_DOCVALUES;
	static {
		METADATA_FIELD_TYPE_WITH_INDEX = new FieldType();
		METADATA_FIELD_TYPE_WITH_INDEX.setTokenized( false );
		METADATA_FIELD_TYPE_WITH_INDEX.setOmitNorms( true );
		METADATA_FIELD_TYPE_WITH_INDEX.setIndexOptions( IndexOptions.DOCS );
		METADATA_FIELD_TYPE_WITH_INDEX.freeze();

		METADATA_FIELD_TYPE_WITH_INDEX_WITH_DOCVALUES = new FieldType();
		METADATA_FIELD_TYPE_WITH_INDEX_WITH_DOCVALUES.setTokenized( false );
		METADATA_FIELD_TYPE_WITH_INDEX_WITH_DOCVALUES.setOmitNorms( true );
		METADATA_FIELD_TYPE_WITH_INDEX_WITH_DOCVALUES.setIndexOptions( IndexOptions.DOCS );
		METADATA_FIELD_TYPE_WITH_INDEX_WITH_DOCVALUES.setDocValuesType( DocValuesType.BINARY );
		METADATA_FIELD_TYPE_WITH_INDEX_WITH_DOCVALUES.freeze();
	}

	private static final String INTERNAL_FIELD_PREFIX = "__HSEARCH_";

	private static final String ID_FIELD_NAME = internalFieldName( "id" );

	private static final String ROUTING_KEY_FIELD_NAME = internalFieldName( "routing_key" );

	private static final String TENANT_ID_FIELD_NAME = internalFieldName( "tenantId" );

	private static final String TYPE_FIELD_NAME = internalFieldName( "type" );

	private static final String FIELD_NAMES_FIELD_NAME = internalFieldName( "field_names" );

	public static final String TYPE_MAIN_DOCUMENT = "main";

	public static final String TYPE_CHILD_DOCUMENT = "child";

	private static final String NESTED_DOCUMENT_PATH = internalFieldName( "nested_document_path" );

	private MetadataFields() {
	}

	public static String internalFieldName(String fieldName) {
		StringBuilder sb = new StringBuilder( INTERNAL_FIELD_PREFIX.length() + fieldName.length() );
		sb.append( INTERNAL_FIELD_PREFIX );
		sb.append( fieldName );
		return sb.toString();
	}

	public static IndexableField searchableMetadataField(String name, String value) {
		return new Field( name, value, METADATA_FIELD_TYPE_WITH_INDEX );
	}

	public static IndexableField searchableRetrievableMetadataField(String name, String value) {
		return new Field( name, new BytesRef( value ), METADATA_FIELD_TYPE_WITH_INDEX_WITH_DOCVALUES );
	}

	public static String idFieldName() {
		return ID_FIELD_NAME;
	}

	public static String routingKeyFieldName() {
		return ROUTING_KEY_FIELD_NAME;
	}

	public static String tenantIdFieldName() {
		return TENANT_ID_FIELD_NAME;
	}

	public static String typeFieldName() {
		return TYPE_FIELD_NAME;
	}

	public static String fieldNamesFieldName() {
		return FIELD_NAMES_FIELD_NAME;
	}

	public static String nestedDocumentPathFieldName() {
		return NESTED_DOCUMENT_PATH;
	}

}
