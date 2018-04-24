/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.util.impl;

public class ElasticsearchFields {

	private static final String INTERNAL_FIELD_PREFIX = "__HSEARCH_";

	private static final String ID_FIELD_NAME = internalFieldName( "id" );

	private static final String TENANT_ID_FIELD_NAME = internalFieldName( "tenantId" );

	private ElasticsearchFields() {
	}

	private static String internalFieldName(String fieldName) {
		StringBuilder sb = new StringBuilder( INTERNAL_FIELD_PREFIX.length() + fieldName.length() );
		sb.append( INTERNAL_FIELD_PREFIX );
		sb.append( fieldName );
		return sb.toString();
	}

	public static String idFieldName() {
		return ID_FIELD_NAME;
	}

	public static String tenantIdFieldName() {
		return TENANT_ID_FIELD_NAME;
	}
}
