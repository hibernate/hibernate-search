/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.reader.spi;

import java.io.Closeable;
import java.io.IOException;

import org.apache.lucene.index.IndexReader;

public interface IndexReaderHolder extends Closeable {

	/**
	 * Release any resource currently held by the {@link IndexReaderHolder},
	 * including (but not limiting to) the reader itself.
	 * <p>
	 * After this method has been called, the result of calling any other method on the same instance is undefined.
	 *
	 * @throws IOException If an error occurs while releasing resources.
	 * @throws RuntimeException If an error occurs while releasing resources.
	 */
	@Override
	void close() throws IOException;

	/**
	 * @return The reader held by this {@link IndexReaderHolder}.
	 */
	IndexReader get();

	/**
	 * @param indexReader The {@link IndexReader} to hold.
	 * @return An {@link IndexReaderHolder} that returns the given directory when {@link #get()} is called
	 * and simply closes the directory when {@link #close()} is called.
	 */
	static IndexReaderHolder of(IndexReader indexReader) {
		return new SimpleIndexReaderHolder( indexReader );
	}

}
