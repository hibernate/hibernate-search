/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.query.dsl.impl;

import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.query.dsl.spi.AbstractDelegatingSearchQuerySelectStep;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.orm.search.loading.dsl.SearchLoadingOptionsStep;
import org.hibernate.search.mapper.orm.search.query.dsl.HibernateOrmSearchQuerySelectStep;

public class HibernateOrmSearchQuerySelectStepImpl<E>
		extends AbstractDelegatingSearchQuerySelectStep<EntityReference, E, SearchLoadingOptionsStep>
		implements HibernateOrmSearchQuerySelectStep<E> {
	private final SearchLoadingOptionsStep loadingOptions;

	public HibernateOrmSearchQuerySelectStepImpl(
			SearchQuerySelectStep<?, EntityReference, E, SearchLoadingOptionsStep, ?, ?> delegate,
			SearchLoadingOptionsStep loadingOptions) {
		super( delegate );
		this.loadingOptions = loadingOptions;
	}

	@Override
	public HibernateOrmSearchQuerySelectStep<E> fetchSize(int fetchSize) {
		loadingOptions.fetchSize( fetchSize );
		return this;
	}

	@Override
	public HibernateOrmSearchQuerySelectStep<E> cacheLookupStrategy(EntityLoadingCacheLookupStrategy strategy) {
		loadingOptions.cacheLookupStrategy( strategy );
		return this;
	}
}
