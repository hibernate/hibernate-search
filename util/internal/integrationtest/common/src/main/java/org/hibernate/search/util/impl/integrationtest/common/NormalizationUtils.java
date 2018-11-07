/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hibernate.search.engine.search.DocumentReference;

/**
 * Utils allowing to normalize data coming from different backends.
 * <p>
 * Mainly useful in search result assertions to compare different types of {@link DocumentReference},
 * but also in {@link EasyMockUtils#projectionMatcher(Object...) EasyMock matchers}.
 */
public final class NormalizationUtils {

	private NormalizationUtils() {
	}

	public static DocumentReference reference(String indexName, String id) {
		return new NormalizedDocumentReference( indexName, id );
	}

	public static DocumentReference normalizeReference(DocumentReference other) {
		return other == null ? null : reference( other.getIndexName(), other.getId() );
	}

	public static List<?> normalizeList(List<?> other) {
		return other.stream()
				.map( projectionItem -> {
					if ( projectionItem instanceof DocumentReference ) {
						return normalizeReference( (DocumentReference) projectionItem );
					}
					else {
						return projectionItem;
					}
				} )
				.collect( Collectors.toList() );
	}

	/**
	 * A pivot format for document references, allowing to compare multiple implementations of {@link DocumentReference}
	 * with each other.
	 */
	static class NormalizedDocumentReference implements DocumentReference {
		private final String indexName;
		private final String id;

		NormalizedDocumentReference(String indexName, String id) {
			this.indexName = indexName;
			this.id = id;
		}

		@Override
		public String getIndexName() {
			return indexName;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public boolean equals(Object obj) {
			if ( obj == null || !NormalizedDocumentReference.class.equals( obj.getClass() ) ) {
				return false;
			}
			NormalizedDocumentReference other = (NormalizedDocumentReference) obj;
			return Objects.equals( indexName, other.indexName )
					&& Objects.equals( id, other.id );
		}

		@Override
		public int hashCode() {
			return Objects.hash( indexName, id );
		}

		@Override
		public String toString() {
			return "DocRef:" + indexName + "/" + id;
		}
	}

}
