/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

import java.io.IOException;

import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;

public class CountDocumentsCollector extends SimpleCollector {

	private long count = 0L;

	@Override
	public void collect(int doc) throws IOException {
		count++;
	}

	@Override
	public ScoreMode scoreMode() {
		return ScoreMode.COMPLETE;
	}

	public long count() {
		return count;
	}
}
