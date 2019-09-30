/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.writer.impl;

import java.io.IOException;

import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;

/**
 * A component exposing features similar to {@link org.apache.lucene.index.IndexWriter},
 * except it only delegates to a writer, opening and closing it as needed.
 * <p>
 * Implementations are not thread safe.
 * <p>
 * This interface also allows to mock the index writer easily in unit tests.
 */
public interface IndexWriterDelegator {

	/**
	 * Checks whether the index exists (on disk, ...), and creates it if necessary.
	 * <p>
	 * Should only be used when starting an index.
	 */
	void ensureIndexExists() throws IOException;

	long addDocuments(Iterable<? extends Iterable<? extends IndexableField>> docs) throws IOException;

	long updateDocuments(Term term, Iterable<? extends Iterable<? extends IndexableField>> docs) throws IOException;

	long deleteDocuments(Term term) throws IOException;

	long deleteDocuments(Query query) throws IOException;

	long deleteAll() throws IOException;

	void commit() throws IOException;

	void flush() throws IOException;

	void forceMerge() throws IOException;

	/**
	 * Forces release of Directory lock. Should be used only to cleanup as error recovery.
	 */
	void forceLockRelease() throws IOException;

}
