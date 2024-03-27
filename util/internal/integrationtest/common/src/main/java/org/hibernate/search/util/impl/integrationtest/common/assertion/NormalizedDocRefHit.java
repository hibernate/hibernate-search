/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

	private NormalizedDocRefHit() {
	}

	public static class Builder {

		private final List<DocumentReference> expectedHits = new ArrayList<>();

		private Builder() {
		}

		public Builder doc(String typeName, String firstId, String... otherIds) {
			expectedHits.add( NormalizationUtils.reference( typeName, firstId ) );
			for ( String id : otherIds ) {
				expectedHits.add( NormalizationUtils.reference( typeName, id ) );
			}
			return this;
		}

		private DocumentReference[] build() {
			return expectedHits.toArray( new DocumentReference[0] );
		}
	}
}
