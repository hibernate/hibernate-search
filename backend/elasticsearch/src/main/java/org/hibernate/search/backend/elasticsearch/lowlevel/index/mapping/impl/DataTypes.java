/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

/**
 * The names of Elasticsearch data types used by Hibernate Search/
 *
 * See https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-types.html
 */
public final class DataTypes {

	private DataTypes() {
	}

	public static final String OBJECT = "object";
	public static final String NESTED = "nested";
	public static final String TEXT = "text";
	public static final String KEYWORD = "keyword";
	public static final String LONG = "long";
	public static final String INTEGER = "integer";
	public static final String DOUBLE = "double";
	public static final String FLOAT = "float";
	public static final String BYTE = "byte";
	public static final String SHORT = "short";
	public static final String DATE = "date";
	public static final String BOOLEAN = "boolean";
	public static final String GEO_POINT = "geo_point";
	public static final String SCALED_FLOAT = "scaled_float";
	public static final String DENSE_VECTOR = "dense_vector";
	public static final String KNN_VECTOR = "knn_vector";

}
