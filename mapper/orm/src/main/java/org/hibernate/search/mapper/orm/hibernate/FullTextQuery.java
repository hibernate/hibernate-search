/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.hibernate;

import org.hibernate.query.Query;

public interface FullTextQuery<T> extends org.hibernate.search.mapper.orm.jpa.FullTextQuery<T> {

	Query<T> toHibernateOrmQuery();

}
