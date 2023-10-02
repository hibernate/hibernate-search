/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import static org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey.of;

import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;

public final class LuceneProjectionTypeKeys {

	private LuceneProjectionTypeKeys() {
	}

	public static final SearchQueryElementTypeKey<?> EXPLANATION = of( "projection:explanation" );
	public static final SearchQueryElementTypeKey<?> DOCUMENT = of( "projection:document" );

}
