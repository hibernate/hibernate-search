/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import static org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey.of;

import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;

public final class ElasticsearchProjectionTypeKeys {

	private ElasticsearchProjectionTypeKeys() {
	}

	public static final SearchQueryElementTypeKey<?> JSON_HIT = of( "projection:json-hit" );
	public static final SearchQueryElementTypeKey<?> SOURCE = of( "projection:source" );
	public static final SearchQueryElementTypeKey<?> EXPLANATION = of( "projection:explanation" );

}
