/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;

import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;

public final class IdentifierValues implements Values<String> {

	private BinaryDocValues currentLeafIdDocValues;

	@Override
	public void context(LeafReaderContext context) throws IOException {
		this.currentLeafIdDocValues = DocValues.getBinary( context.reader(), MetadataFields.idFieldName() );
	}

	@Override
	public String get(int doc) throws IOException {
		// Only move forward if the current doc position is not far enough already.
		// Without this check we might end up calling advance() more than we need to in case we have an id projection within
		// a multivalued object. Even though we are within the same top-level object advance() will be called for each
		// element in the list of nested objects messing up the result.
		if ( currentLeafIdDocValues.docID() < doc ) {
			currentLeafIdDocValues.advance( doc );
		}
		return currentLeafIdDocValues.binaryValue().utf8ToString();
	}
}
