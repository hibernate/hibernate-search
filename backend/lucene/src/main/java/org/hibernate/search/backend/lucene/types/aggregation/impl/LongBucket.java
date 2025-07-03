/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.search.Collector;

public class LongBucket {
	public final long termOrd;
	public final List<Collector>[] collectors;
	public long count;

	@SuppressWarnings("unchecked")
	public LongBucket(long termOrd, Collector[] collectors, long count) {
		this.termOrd = termOrd;
		this.collectors = new List[collectors.length];
		for ( int i = 0; i < collectors.length; i++ ) {
			this.collectors[i] = new ArrayList<>();
			this.collectors[i].add( collectors[i] );
		}
		this.count = count;
	}

	public long count() {
		return count;
	}

	public long termOrd() {
		return termOrd;
	}

	@Override
	public String toString() {
		return "LongBucket{" +
				"termOrd=" + termOrd +
				", count=" + count +
				", collectors=" + Arrays.toString( collectors ) +
				'}';
	}
}
