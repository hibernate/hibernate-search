/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations;

public class IndexNullAsMatchPredicateExpectactions<F> {

	private final F indexNullAsValue;
	private final F differentValue;

	public IndexNullAsMatchPredicateExpectactions(F indexNullAsValue, F differentValue) {
		this.indexNullAsValue = indexNullAsValue;
		this.differentValue = differentValue;
	}

	public final F getIndexNullAsValue() {
		return indexNullAsValue;
	}

	public final F getDifferentValue() {
		return differentValue;
	}
}
