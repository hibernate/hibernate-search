/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.dsl.query.impl;

import org.hibernate.search.engine.search.dsl.query.SearchQueryHitTypeStep;
import org.hibernate.search.engine.search.dsl.query.spi.AbstractDelegatingSearchQueryHitTypeStep;
import org.hibernate.search.mapper.orm.search.dsl.query.HibernateOrmSearchQueryHitTypeStep;
import org.hibernate.search.mapper.orm.search.loading.context.impl.HibernateOrmLoadingContext;
import org.hibernate.search.mapper.orm.common.EntityReference;

public class HibernateOrmSearchQueryHitTypeStepImpl<E>
		extends AbstractDelegatingSearchQueryHitTypeStep<EntityReference, E>
		implements HibernateOrmSearchQueryHitTypeStep<E> {
	private final HibernateOrmLoadingContext.Builder<E> loadingContextBuilder;

	public HibernateOrmSearchQueryHitTypeStepImpl(
			SearchQueryHitTypeStep<?, EntityReference, E, ?, ?> delegate,
			HibernateOrmLoadingContext.Builder<E> loadingContextBuilder) {
		super( delegate );
		this.loadingContextBuilder = loadingContextBuilder;
	}

	@Override
	public HibernateOrmSearchQueryHitTypeStep<E> fetchSize(int fetchSize) {
		loadingContextBuilder.fetchSize( fetchSize );
		return this;
	}
}
