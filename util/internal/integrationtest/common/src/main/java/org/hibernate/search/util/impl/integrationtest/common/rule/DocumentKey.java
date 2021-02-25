/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import java.util.Objects;

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
		builder.append( indexName ).append( '#' ).append( documentIdentifier );
		if ( tenantIdentifier != null ) {
			builder.append( "(tenant:" ).append( tenantIdentifier ).append( ')' );
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
