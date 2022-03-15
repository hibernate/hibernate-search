/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;

import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.IntObjectMap;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;

public final class IdentifierCollector extends SimpleCollector {

	public static final CollectorKey<IdentifierCollector> KEY = CollectorKey.create();

	public static final CollectorFactory<IdentifierCollector> FACTORY = new CollectorFactory<IdentifierCollector>() {
		@Override
		public IdentifierCollector createCollector(CollectorExecutionContext context) {
			return new IdentifierCollector();
		}

		@Override
		public CollectorKey<IdentifierCollector> getCollectorKey() {
			return KEY;
		}
	};

	private BinaryDocValues currentLeafIdDocValues;
	private int currentLeafDocBase;

	private final IntObjectMap<String> collected = new IntObjectHashMap<>();

	private IdentifierCollector() {
	}

	@Override
	public void collect(int doc) throws IOException {
		currentLeafIdDocValues.advance( doc );
		collected.put( currentLeafDocBase + doc, currentLeafIdDocValues.binaryValue().utf8ToString() );
	}

	@Override
	public ScoreMode scoreMode() {
		return ScoreMode.COMPLETE_NO_SCORES;
	}

	public String get(int doc) {
		return collected.get( doc );
	}

	@Override
	protected void doSetNextReader(LeafReaderContext context) throws IOException {
		this.currentLeafIdDocValues = DocValues.getBinary( context.reader(), MetadataFields.idFieldName() );
		this.currentLeafDocBase = context.docBase;
	}
}
