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
 * but simpler to use, and delegated to an actual writer.
 * <p>
 * This interface also allows easy mocking of the index writer in unit tests.
 */
public interface IndexWriterDelegator {

	long addDocuments(Iterable<? extends Iterable<? extends IndexableField>> docs) throws IOException;

	long updateDocuments(Term term, Iterable<? extends Iterable<? extends IndexableField>> docs) throws IOException;

	long deleteDocuments(Term term) throws IOException;

	long deleteDocuments(Query query) throws IOException;

	long deleteAll() throws IOException;

	void flush() throws IOException;

	void mergeSegments() throws IOException;

}
