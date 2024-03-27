/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
