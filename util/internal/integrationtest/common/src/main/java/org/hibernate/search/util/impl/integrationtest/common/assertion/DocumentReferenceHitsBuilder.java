/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.assertion;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils;

public class DocumentReferenceHitsBuilder {

	private final List<DocumentReference> expectedHits = new ArrayList<>();

	DocumentReferenceHitsBuilder() {
	}

	public DocumentReferenceHitsBuilder doc(String indexName, String firstId, String... otherIds) {
		expectedHits.add( NormalizationUtils.reference( indexName, firstId ) );
		for ( String id : otherIds ) {
			expectedHits.add( NormalizationUtils.reference( indexName, id ) );
		}
		return this;
	}

	DocumentReference[] getExpectedHits() {
		return expectedHits.toArray( new DocumentReference[0] );
	}
}
