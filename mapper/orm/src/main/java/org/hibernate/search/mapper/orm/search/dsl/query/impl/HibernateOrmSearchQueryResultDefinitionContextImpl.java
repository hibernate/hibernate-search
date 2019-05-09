/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.dsl.query.impl;

import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.dsl.query.spi.AbstractDelegatingSearchQueryResultDefinitionContext;
import org.hibernate.search.mapper.orm.search.dsl.query.HibernateOrmSearchQueryResultDefinitionContext;
import org.hibernate.search.mapper.orm.search.loading.impl.MutableEntityLoadingOptions;
import org.hibernate.search.mapper.pojo.search.PojoReference;

public class HibernateOrmSearchQueryResultDefinitionContextImpl<E>
		extends AbstractDelegatingSearchQueryResultDefinitionContext<PojoReference, E>
		implements HibernateOrmSearchQueryResultDefinitionContext<E> {
	private final MutableEntityLoadingOptions loadingOptions;

	public HibernateOrmSearchQueryResultDefinitionContextImpl(
			SearchQueryResultDefinitionContext<PojoReference, E, ?> delegate,
			MutableEntityLoadingOptions loadingOptions) {
		super( delegate );
		this.loadingOptions = loadingOptions;
	}

	@Override
	public HibernateOrmSearchQueryResultDefinitionContext<E> fetchSize(int fetchSize) {
		loadingOptions.setFetchSize( fetchSize );
		return this;
	}
}
