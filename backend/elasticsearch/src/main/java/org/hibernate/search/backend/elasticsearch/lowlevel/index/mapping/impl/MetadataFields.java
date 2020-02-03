/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

public final class MetadataFields {

	private static final String INTERNAL_FIELD_PREFIX = "_";

	private static final char PATH_SEPARATOR = '.';

	private MetadataFields() {
	}

	public static String internalFieldName(String fieldName) {
		StringBuilder sb = new StringBuilder( INTERNAL_FIELD_PREFIX.length() + fieldName.length() );
		sb.append( INTERNAL_FIELD_PREFIX );
		sb.append( fieldName );
		return sb.toString();
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
