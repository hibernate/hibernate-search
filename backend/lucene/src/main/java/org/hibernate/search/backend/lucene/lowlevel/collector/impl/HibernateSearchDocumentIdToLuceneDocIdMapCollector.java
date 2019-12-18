/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.backend.lucene.lowlevel.common.impl.LuceneFields;

import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;

public final class HibernateSearchDocumentIdToLuceneDocIdMapCollector extends SimpleCollector {

	public static final LuceneCollectorFactory<HibernateSearchDocumentIdToLuceneDocIdMapCollector> FACTORY =
			context -> new HibernateSearchDocumentIdToLuceneDocIdMapCollector();

	private BinaryDocValues currentLeafIdDocValues;
	private int currentLeafDocBase;

	private Map<String, Integer> collected = new HashMap<>();

	private HibernateSearchDocumentIdToLuceneDocIdMapCollector() {
	}

	@Override
	public void collect(int doc) throws IOException {
		currentLeafIdDocValues.advance( doc );
		collected.put( currentLeafIdDocValues.binaryValue().utf8ToString(), currentLeafDocBase + doc );
	}

	@Override
	public ScoreMode scoreMode() {
		return ScoreMode.COMPLETE_NO_SCORES;
	}

	public Integer getLuceneDocId(String documentId) {
		return collected.get( documentId );
	}

	@Override
	protected void doSetNextReader(LeafReaderContext context) throws IOException {
		this.currentLeafIdDocValues = DocValues.getBinary( context.reader(), LuceneFields.idFieldName() );
		this.currentLeafDocBase = context.docBase;
	}
}
