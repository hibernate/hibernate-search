/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.assertion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils;

public class SearchHitAssert<H> {

	private final H actual;
	private String description;

	SearchHitAssert(H actual) {
		this.actual = actual;
	}

	public SearchHitAssert<H> as(String description) {
		this.description = description;
		return this;
	}

	public void isDocRefHit(String typeName, String id, String... orIds) {
		Set<DocumentReference> references = new HashSet<>();
		references.add( NormalizationUtils.reference( typeName, id ) );
		for ( String orId : orIds ) {
			references.add( NormalizationUtils.reference( typeName, orId ) );
		}

		DocumentReference actualReference = NormalizationUtils.normalize( (DocumentReference) actual );

		assertThat( references )
				.as( description )
				.contains( actualReference );
	}
}
