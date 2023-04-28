/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.document.impl.LuceneIdReader;

import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.LeafReaderContext;

public final class IdentifierValues implements Values<String> {

	private final LuceneIdReader idReader;
	private BinaryDocValues currentLeafIdDocValues;

	public IdentifierValues(LuceneIdReader idReader) {
		this.idReader = idReader;
	}

	@Override
	public void context(LeafReaderContext context) throws IOException {
		this.currentLeafIdDocValues = idReader.idDocValues( context.reader() );
	}

	@Override
	public String get(int doc) throws IOException {
		currentLeafIdDocValues.advance( doc );
		return currentLeafIdDocValues.binaryValue().utf8ToString();
	}
}
