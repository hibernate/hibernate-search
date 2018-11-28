/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing;

public interface MassIndexingMonitor {

	void addToTotalCount(long increment); // Already taken care of

	void documentsAdded(long increment); // see above

	void documentsBuilt(int increment); // see above

	void entitiesLoaded(int increment); // see above

	void indexingCompleted(); // Already taken care of

}
