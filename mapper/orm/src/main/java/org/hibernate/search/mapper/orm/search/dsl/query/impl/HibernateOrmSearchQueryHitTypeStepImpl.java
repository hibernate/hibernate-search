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
import org.hibernate.search.mapper.orm.search.loading.impl.MutableEntityLoadingOptions;
import org.hibernate.search.mapper.orm.common.EntityReference;

public class HibernateOrmSearchQueryHitTypeStepImpl<E>
		extends AbstractDelegatingSearchQueryHitTypeStep<EntityReference, E>
		implements HibernateOrmSearchQueryHitTypeStep<E> {
	private final MutableEntityLoadingOptions loadingOptions;

	public HibernateOrmSearchQueryHitTypeStepImpl(
			SearchQueryHitTypeStep<?, EntityReference, E, ?, ?> delegate,
			MutableEntityLoadingOptions loadingOptions) {
		super( delegate );
		this.loadingOptions = loadingOptions;
	}

	@Override
	public HibernateOrmSearchQueryHitTypeStep<E> fetchSize(int fetchSize) {
		loadingOptions.setFetchSize( fetchSize );
		return this;
	}
}
