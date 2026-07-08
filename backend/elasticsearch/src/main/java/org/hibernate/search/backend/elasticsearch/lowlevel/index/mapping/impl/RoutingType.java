/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

/**
 * An enum for Elasticsearch "_routing" attribute values.
 *
 * See https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-routing-field.html
 */
public enum RoutingType {

	REQUIRED,
	OPTIONAL,
	;
}
