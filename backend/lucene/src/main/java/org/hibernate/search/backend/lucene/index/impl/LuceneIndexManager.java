/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import org.hibernate.search.engine.backend.index.spi.IndexManager;
import org.hibernate.search.backend.lucene.document.impl.LuceneRootDocumentBuilder;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.index.spi.ReaderProvider;

/**
 * @author Guillaume Smet
 */
// TODO the ReaderProvider will need some work as we will probably need all the complexity
// of the current ReaderProvider hierarchy but for now having it here will do
public interface LuceneIndexManager extends IndexManager<LuceneRootDocumentBuilder>, ReaderProvider {

	String getName();

	LuceneIndexModel getModel();
}
