/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.spi;

import java.util.List;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.indexes.spi.IndexManager;

/**
 * A helper to dispatch a list of works to the appropriate index managers
 * (initializing them as necessary),
 * executing the {@link IndexManager#performOperations(List, org.hibernate.search.backend.IndexingMonitor)}
 * or {@link IndexManager#performStreamOperation(LuceneWork, IndexingMonitor, boolean)}
 * method.
 * <p>
 * Intended for use by message consumers in the JMS/JGroups backends.
 *
 * @author Yoann Rodiere
 *
 * @hsearch.experimental This type is under active development. You should be prepared
 * for incompatible changes in future releases.
 */
public interface OperationDispatcher {

	void dispatch(LuceneWork work, IndexingMonitor monitor);

	void dispatch(List<LuceneWork> queue, IndexingMonitor monitor);

}
