/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

public final class MetadataFields {

	private static final String INTERNAL_FIELD_PREFIX = "_";

	private MetadataFields() {
	}

	public static String internalFieldName(String fieldName) {
		StringBuilder sb = new StringBuilder( INTERNAL_FIELD_PREFIX.length() + fieldName.length() );
		sb.append( INTERNAL_FIELD_PREFIX );
		sb.append( fieldName );
		return sb.toString();
	}

}
