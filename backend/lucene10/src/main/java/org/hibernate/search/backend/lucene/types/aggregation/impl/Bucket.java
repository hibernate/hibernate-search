/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

final class Bucket<F> {
	final F term;
	final long count;

	Bucket(F term, long count) {
		this.term = term;
		this.count = count;
	}
}
