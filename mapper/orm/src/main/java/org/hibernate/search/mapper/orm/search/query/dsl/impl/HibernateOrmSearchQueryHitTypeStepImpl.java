/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.query.dsl.impl;

import org.hibernate.search.engine.search.query.dsl.SearchQueryHitTypeStep;
import org.hibernate.search.engine.search.query.dsl.spi.AbstractDelegatingSearchQueryHitTypeStep;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.orm.search.loading.dsl.SearchLoadingOptionsStep;
import org.hibernate.search.mapper.orm.search.query.dsl.HibernateOrmSearchQueryHitTypeStep;

public class HibernateOrmSearchQueryHitTypeStepImpl<E>
		extends AbstractDelegatingSearchQueryHitTypeStep<EntityReference, E, SearchLoadingOptionsStep>
		implements HibernateOrmSearchQueryHitTypeStep<E> {
	private final SearchLoadingOptionsStep loadingOptions;

	public HibernateOrmSearchQueryHitTypeStepImpl(
			SearchQueryHitTypeStep<?, EntityReference, E, SearchLoadingOptionsStep, ?, ?> delegate,
			SearchLoadingOptionsStep loadingOptions) {
		super( delegate );
		this.loadingOptions = loadingOptions;
	}

	@Override
	public HibernateOrmSearchQueryHitTypeStep<E> fetchSize(int fetchSize) {
		loadingOptions.fetchSize( fetchSize );
		return this;
	}

	@Override
	public HibernateOrmSearchQueryHitTypeStep<E> cacheLookupStrategy(EntityLoadingCacheLookupStrategy strategy) {
		loadingOptions.cacheLookupStrategy( strategy );
		return this;
	}
}
