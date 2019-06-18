/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.event.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.mapper.orm.mapping.impl.HibernateSearchContextService;
import org.hibernate.search.mapper.orm.mapping.spi.HibernateOrmMapping;

/**
 * This EventsHibernateSearchState is useful to hold for requests
 * onto the actual {@link HibernateOrmMapping}
 * until the initialization of Hibernate Search has been completed.
 *
 * @author Sanne Grinovero
 */
final class InitializingHibernateSearchState implements EventsHibernateSearchState {

	private final CompletableFuture<HibernateSearchContextService> contextFuture;

	public InitializingHibernateSearchState(CompletableFuture<HibernateSearchContextService> contextFuture) {
		this.contextFuture = contextFuture;
	}

	@Override
	public HibernateSearchContextService getHibernateSearchContext() {
		return contextFuture.join();
	}

}
