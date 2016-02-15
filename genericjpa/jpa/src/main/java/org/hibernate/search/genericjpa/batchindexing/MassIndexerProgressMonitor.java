/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.batchindexing;

/**
 * @author Martin Braun
 */
public interface MassIndexerProgressMonitor {

	void idsLoaded(Class<?> entityType, int count);

	void objectsLoaded(Class<?> entityType, int count);

	void documentsBuilt(Class<?> entityType, int count);

	void documentsAdded(int count);

}
