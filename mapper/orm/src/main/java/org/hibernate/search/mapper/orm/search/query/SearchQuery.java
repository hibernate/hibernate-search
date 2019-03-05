/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.query;

import java.util.List;
import java.util.Optional;
import javax.persistence.TypedQuery;

import org.hibernate.query.Query;

public interface SearchQuery<T> {

	List<T> getResultList();

	T getSingleResult();

	Optional<T> getOptionalResult();

	long getResultSize();

	SearchQuery<T> setMaxResults(int maxResults);

	SearchQuery<T> setFirstResult(int firstResult);

	SearchQuery<T> setFetchSize(int fetchSize);

	TypedQuery<T> toJpaQuery();

	Query<T> toHibernateOrmQuery();

}
