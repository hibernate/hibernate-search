/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.spi;

import java.io.Closeable;
import java.io.IOException;

import org.apache.lucene.search.TopDocs;

/**
 * DocumentExtractor is a traverser over the full-text results (EntityInfo)
 *
 * This operation is as lazy as possible:
 *  - the query is executed eagerly
 *  - results are not retrieved until actually requested
 *
 *  {@link #getFirstIndex()} and {@link #getMaxIndex()} define the boundaries available to {@link #extract(int)}.
 *
 * DocumentExtractor objects *must* be closed when the results are no longer traversed. See {@link #close()}
 *
 * @author Emmanuel Bernard
 */
public interface DocumentExtractor extends Closeable {

	EntityInfo extract(int index) throws IOException;

	int getFirstIndex();

	int getMaxIndex();

	void close();

	/**
	 * @hsearch.experimental We are thinking at ways to encapsulate needs for exposing TopDocs (and whether or not it makes sense)
	 * Try to avoid it if you can
	 * @return {@link TopDocs} instance
	 */
	TopDocs getTopDocs();

}
