/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.spi;

import org.apache.lucene.index.IndexReader;

/**
 * Responsible for providing and managing the lifecycle of a read only reader.
 * Note that the reader must be closed once opened using this same service.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public interface ReaderProvider {

	IndexReader openIndexReader();

	void closeIndexReader(IndexReader reader);

}
