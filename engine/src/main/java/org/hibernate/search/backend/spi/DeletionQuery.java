/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.spi;

import org.apache.lucene.search.Query;
import org.hibernate.search.util.impl.ScopedAnalyzer;

/**
 * interface for Serializable Queries that can be used to delete from an index.
 * These have to be passed safely between VMs (we cannot rely on Lucene's queries here
 * because of that).
 *
 * @hsearch.experimental
 *
 * @author Martin Braun
 */
public interface DeletionQuery {

	/**
	 * used to identify the type of query faster (no need for instanceof checks)
	 * @return the unique query key
	 */
	int getQueryKey();

	/**
	 * converts this DeletionQuery to a Lucene Query
	 *
	 * @param analyzerForEntity the analyzer to be used for this query
	 * @return the deletion query as Lucene {@link Query}
	 */
	Query toLuceneQuery(ScopedAnalyzer analyzerForEntity);

	/**
	 * We are serializing to a String array here instead of a byte array since we don't want implementors to use the
	 * standard Java Serialization API by mistake
	 * @return a String[] representation of this DeletionQuery
	 */
	String[] serialize();

}
