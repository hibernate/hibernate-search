/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderMetadataResolver;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneDocumentReference;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.engine.backend.common.DocumentReference;

import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.IntObjectMap;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;

public final class DocumentReferenceCollector extends SimpleCollector {

	public static final CollectorKey<DocumentReferenceCollector> KEY = CollectorKey.create();

	public static final CollectorFactory<DocumentReferenceCollector> FACTORY = new CollectorFactory<DocumentReferenceCollector>() {
		@Override
		public DocumentReferenceCollector createCollector(CollectorExecutionContext context) {
			return new DocumentReferenceCollector( context );
		}

		@Override
		public CollectorKey<DocumentReferenceCollector> getCollectorKey() {
			return KEY;
		}
	};

	private final IndexReaderMetadataResolver metadataResolver;

	private String currentLeafMappedTypeName;
	private BinaryDocValues currentLeafIdDocValues;
	private int currentLeafDocBase;

	private final IntObjectMap<DocumentReference> collected = new IntObjectHashMap<>();

	private DocumentReferenceCollector(CollectorExecutionContext executionContext) {
		this.metadataResolver = executionContext.getMetadataResolver();
	}

	@Override
	public void collect(int doc) throws IOException {
		currentLeafIdDocValues.advance( doc );
		collected.put( currentLeafDocBase + doc, new LuceneDocumentReference(
				currentLeafMappedTypeName,
				currentLeafIdDocValues.binaryValue().utf8ToString()
		) );
	}

	@Override
	public ScoreMode scoreMode() {
		return ScoreMode.COMPLETE_NO_SCORES;
	}

	public DocumentReference get(int doc) {
		return collected.get( doc );
	}

	@Override
	protected void doSetNextReader(LeafReaderContext context) throws IOException {
		this.currentLeafMappedTypeName = metadataResolver.resolveMappedTypeName( context );
		this.currentLeafIdDocValues = DocValues.getBinary( context.reader(), MetadataFields.idFieldName() );
		this.currentLeafDocBase = context.docBase;
	}
}
