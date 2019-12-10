/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.util.impl;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;

public class LuceneFields {

	private static final FieldType METADATA_FIELD_TYPE_WITH_INDEX;
	private static final FieldType METADATA_FIELD_TYPE_WITH_DOCVALUES;
	private static final FieldType METADATA_FIELD_TYPE_WITH_INDEX_WITH_DOCVALUES;
	static {
		METADATA_FIELD_TYPE_WITH_INDEX = new FieldType();
		METADATA_FIELD_TYPE_WITH_INDEX.setTokenized( false );
		METADATA_FIELD_TYPE_WITH_INDEX.setOmitNorms( true );
		METADATA_FIELD_TYPE_WITH_INDEX.setIndexOptions( IndexOptions.DOCS );
		METADATA_FIELD_TYPE_WITH_INDEX.freeze();

		METADATA_FIELD_TYPE_WITH_DOCVALUES = new FieldType( METADATA_FIELD_TYPE_WITH_INDEX );
		METADATA_FIELD_TYPE_WITH_DOCVALUES.setTokenized( false );
		METADATA_FIELD_TYPE_WITH_DOCVALUES.setOmitNorms( true );
		METADATA_FIELD_TYPE_WITH_DOCVALUES.setIndexOptions( IndexOptions.NONE );
		METADATA_FIELD_TYPE_WITH_DOCVALUES.setDocValuesType( DocValuesType.BINARY );
		METADATA_FIELD_TYPE_WITH_DOCVALUES.freeze();

		METADATA_FIELD_TYPE_WITH_INDEX_WITH_DOCVALUES = new FieldType();
		METADATA_FIELD_TYPE_WITH_INDEX_WITH_DOCVALUES.setTokenized( false );
		METADATA_FIELD_TYPE_WITH_INDEX_WITH_DOCVALUES.setOmitNorms( true );
		METADATA_FIELD_TYPE_WITH_INDEX_WITH_DOCVALUES.setIndexOptions( IndexOptions.DOCS );
		METADATA_FIELD_TYPE_WITH_INDEX_WITH_DOCVALUES.setDocValuesType( DocValuesType.BINARY );
		METADATA_FIELD_TYPE_WITH_INDEX_WITH_DOCVALUES.freeze();
	}

	private static final String INTERNAL_FIELD_PREFIX = "__HSEARCH_";

	private static final char PATH_SEPARATOR = '.';

	private static final String ID_FIELD_NAME = internalFieldName( "id" );

	private static final String TENANT_ID_FIELD_NAME = internalFieldName( "tenantId" );

	private static final String TYPE_FIELD_NAME = internalFieldName( "type" );

	private static final String FIELD_NAMES_FIELD_NAME = internalFieldName( "field_names" );

	public static final String TYPE_MAIN_DOCUMENT = "main";

	public static final String TYPE_CHILD_DOCUMENT = "child";

	private static final String ROOT_ID_FIELD_NAME = internalFieldName( "root_id" );

	private static final String ROOT_INDEX_FIELD_NAME = internalFieldName( "root_index" );

	private static final String NESTED_DOCUMENT_PATH = internalFieldName( "nested_document_path" );

	private LuceneFields() {
	}

	public static String internalFieldName(String fieldName) {
		StringBuilder sb = new StringBuilder( INTERNAL_FIELD_PREFIX.length() + fieldName.length() );
		sb.append( INTERNAL_FIELD_PREFIX );
		sb.append( fieldName );
		return sb.toString();
	}

	public static String internalFieldName(String fieldName, String suffix) {
		StringBuilder sb = new StringBuilder( INTERNAL_FIELD_PREFIX.length() + fieldName.length() + suffix.length() + 1 );
		sb.append( INTERNAL_FIELD_PREFIX );
		sb.append( fieldName );
		sb.append( '_' );
		sb.append( suffix );
		return sb.toString();
	}

	public static IndexableField searchableMetadataField(String name, String value) {
		return new Field( name, value, METADATA_FIELD_TYPE_WITH_INDEX );
	}

	public static IndexableField searchableRetrievableMetadataField(String name, String value) {
		return new Field( name, new BytesRef( value ), METADATA_FIELD_TYPE_WITH_INDEX_WITH_DOCVALUES );
	}

	public static IndexableField retrievableMetadataField(String name, String value) {
		return new Field( name, new BytesRef( value ), METADATA_FIELD_TYPE_WITH_DOCVALUES );
	}

	public static String idFieldName() {
		return ID_FIELD_NAME;
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

	public static String rootIdFieldName() {
		return ROOT_ID_FIELD_NAME;
	}

	public static String rootIndexFieldName() {
		return ROOT_INDEX_FIELD_NAME;
	}

	public static String nestedDocumentPathFieldName() {
		return NESTED_DOCUMENT_PATH;
	}

	public static String compose(String absolutePath, String relativeFieldName) {
		if ( absolutePath == null ) {
			return relativeFieldName;
		}

		StringBuilder sb = new StringBuilder( absolutePath.length() + relativeFieldName.length() + 1 );
		sb.append( absolutePath )
				.append( PATH_SEPARATOR )
				.append( relativeFieldName );

		return sb.toString();
	}
}
