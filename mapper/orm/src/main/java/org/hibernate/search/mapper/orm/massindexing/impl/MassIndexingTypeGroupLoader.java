/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.Query;

public interface MassIndexingTypeGroupLoader<E, I> {

	Query<Long> createCountQuery(SharedSessionContractImplementor session);

	Query<I> createIdentifiersQuery(SharedSessionContractImplementor session);

	Query<E> createLoadingQuery(SessionImplementor session, String idParameterName);

}
