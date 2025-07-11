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

	public LongBucket(long termOrd, List<Collector>[] collectors, long count) {
		this.termOrd = termOrd;
		this.collectors = collectors;
		this.count = count;
	}

	public void add(Collector[] collectors, long count) {
		this.count += count;
		for ( int i = 0; i < collectors.length; i++ ) {
			this.collectors[i].add( collectors[i] );
		}
	}

	public void add(LongBucket bucket) {
		this.count += bucket.count;
		for ( int i = 0; i < collectors.length; i++ ) {
			this.collectors[i].addAll( bucket.collectors[i] );
		}
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
