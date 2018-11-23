/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.jpa;

import java.util.Collection;
import javax.persistence.EntityManager;

import org.hibernate.search.mapper.orm.massindexing.MassIndexer;

public interface FullTextEntityManager extends EntityManager {

	<T> FullTextSearchTarget<T> search(Class<T> type);

	<T> FullTextSearchTarget<T> search(Collection<? extends Class<? extends T>> types);

	MassIndexer createIndexer(Class<?>... types);
}
