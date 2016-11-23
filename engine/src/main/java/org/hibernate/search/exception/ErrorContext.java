/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.exception;

import java.util.List;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.indexes.spi.IndexManager;

/**
 * @author Amin Mohammed-Coleman
 * @since 3.2
 */
public interface ErrorContext {

	List<LuceneWork> getFailingOperations();

	LuceneWork getOperationAtFault();

	Throwable getThrowable();

	boolean hasErrors();

	IndexManager getIndexManager();

}
