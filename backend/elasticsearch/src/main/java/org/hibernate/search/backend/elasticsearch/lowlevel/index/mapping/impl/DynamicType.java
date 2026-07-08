/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

import org.hibernate.search.backend.elasticsearch.index.DynamicMapping;

/**
 * An enum for Elasticsearch "dynamic" attribute values.
 *
 * See https://www.elastic.co/guide/en/elasticsearch/reference/current/dynamic.html
 */
public enum DynamicType {

	TRUE,
	FALSE,
	STRICT;

	public static DynamicType create(DynamicMapping dynamicMapping) {
		return switch ( dynamicMapping ) {
			case TRUE -> DynamicType.TRUE;
			case FALSE -> DynamicType.FALSE;
			case STRICT -> DynamicType.STRICT;
		};
	}
}
