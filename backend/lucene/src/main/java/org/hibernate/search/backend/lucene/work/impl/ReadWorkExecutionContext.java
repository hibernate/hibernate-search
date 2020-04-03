/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.impl;

import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderMetadataResolver;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.index.IndexReader;
import org.hibernate.search.backend.lucene.LuceneBackend;


public interface ReadWorkExecutionContext {

	IndexReader getIndexReader();

	IndexReaderMetadataResolver getIndexReaderMetadataResolver();

	EventContext getEventContext();

	LuceneBackend getBackend();

}
