/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;

import com.carrotsearch.hppc.IntIntMap;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.ScoreDoc;

public class ScoreValues implements Values<Float> {

	private final IntIntMap docIdToScoreDocIndex;
	private final ScoreDoc[] scoreDocs;
	private int currentLeafDocBase;

	public ScoreValues(TopDocsDataCollectorExecutionContext context) {
		this.docIdToScoreDocIndex = context.docIdToScoreDocIndex();
		this.scoreDocs = context.topDocs().scoreDocs;
	}

	@Override
	public void context(LeafReaderContext context) throws IOException {
		this.currentLeafDocBase = context.docBase;
	}

	@Override
	public Float get(int doc) throws IOException {
		return scoreDocs[docIdToScoreDocIndex.get( currentLeafDocBase + doc )].score;
	}
}
