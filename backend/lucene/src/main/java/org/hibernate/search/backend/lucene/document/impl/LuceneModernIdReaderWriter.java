/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReader;

public final class LuceneModernIdReaderWriter implements LuceneIdReaderWriter {

	public static final LuceneIdReaderWriter INSTANCE = new LuceneModernIdReaderWriter();

	private LuceneModernIdReaderWriter() {
	}

	@Override
	public BinaryDocValues idDocValues(LeafReader reader) throws IOException {
		return DocValues.getBinary( reader, MetadataFields.idDocValueFieldName() );
	}

	@Override
	public void write(String id, Document document) {
		document.add( MetadataFields.searchableMetadataField( MetadataFields.idFieldName(), id ) );
		document.add( MetadataFields.retrievableMetadataField( MetadataFields.idDocValueFieldName(), id ) );
	}
}
