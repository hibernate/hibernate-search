/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.reader.spi;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;

final class SimpleIndexReaderHolder implements IndexReaderHolder {
	private final IndexReader indexReader;

	SimpleIndexReaderHolder(IndexReader indexReader) {
		this.indexReader = indexReader;
	}

	@Override
	public IndexReader get() {
		return indexReader;
	}

	@Override
	public void close() throws IOException {
		indexReader.close();
	}
}
