/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.assertion;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils;

public class NormalizedDocRefHit {

	public static DocumentReference[] of(Consumer<Builder> contributor) {
		Builder builder = new Builder();
		contributor.accept( builder );
		return builder.build();
	}

	public static DocumentReference[] of(String indexName, String firstId, String... otherIds) {
		return of( b -> b.doc( indexName, firstId, otherIds ) );
	}

	private NormalizedDocRefHit() {
	}

	public static class Builder {

		private final List<DocumentReference> expectedHits = new ArrayList<>();

		private Builder() {
		}

		public Builder doc(String indexName, String firstId, String... otherIds) {
			expectedHits.add( NormalizationUtils.reference( indexName, firstId ) );
			for ( String id : otherIds ) {
				expectedHits.add( NormalizationUtils.reference( indexName, id ) );
			}
			return this;
		}

		private DocumentReference[] build() {
			return expectedHits.toArray( new DocumentReference[0] );
		}
	}
}
