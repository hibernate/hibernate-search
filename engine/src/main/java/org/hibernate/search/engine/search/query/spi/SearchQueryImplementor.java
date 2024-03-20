/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.spi;

import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.search.query.SearchQuery;

/**
 * Defines the "service program contract" for {@link SearchQuery}.
 * <p>
 * Methods on {@link SearchQuery} are not supposed to change the internal state of the instance.
 * Methods here can do that.
 *
 * @param <H> The type of query hits.
 */
public interface SearchQueryImplementor<H> extends SearchQuery<H> {

	@Deprecated
	default void failAfter(long timeout, TimeUnit timeUnit) {
		failAfter( (Long) timeout, timeUnit );
	}

	void failAfter(Long timeout, TimeUnit timeUnit);
}
