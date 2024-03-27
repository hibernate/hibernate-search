/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

/**
 * The names of Elasticsearch data matching types used by Hibernate Search in dynamic templates.
 *
 * See https://www.elastic.co/guide/en/elasticsearch/reference/current/dynamic-templates.html#match-mapping-type
 */
public final class DataMatchingTypes {

	private DataMatchingTypes() {
	}

	public static final String OBJECT = "object";

}
