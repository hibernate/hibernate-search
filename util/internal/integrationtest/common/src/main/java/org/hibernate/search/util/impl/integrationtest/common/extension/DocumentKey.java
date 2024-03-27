/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.extension;

import java.util.Objects;

import org.hibernate.search.util.impl.test.logging.TestEscapers;

class DocumentKey {
	private final String indexName;
	private final String tenantIdentifier;
	private final String documentIdentifier;

	DocumentKey(String indexName, String tenantIdentifier, String documentIdentifier) {
		this.indexName = indexName;
		this.tenantIdentifier = tenantIdentifier;
		this.documentIdentifier = documentIdentifier;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append( indexName ).append( '#' ).append( TestEscapers.escape( documentIdentifier ) );
		if ( tenantIdentifier != null ) {
			builder.append( "(tenant:" ).append( TestEscapers.escape( tenantIdentifier ) ).append( ')' );
		}
		return builder.toString();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		DocumentKey that = (DocumentKey) o;
		return Objects.equals( indexName, that.indexName )
				&& Objects.equals( tenantIdentifier, that.tenantIdentifier )
				&& Objects.equals( documentIdentifier, that.documentIdentifier );
	}

	@Override
	public int hashCode() {
		return Objects.hash( indexName, tenantIdentifier, documentIdentifier );
	}
}
