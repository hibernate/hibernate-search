/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

final class SearchIntegrationHandle implements SearchIntegration.Handle {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private SearchIntegration integration;

	void initialize(SearchIntegration integration) {
		this.integration = integration;
	}

	@Override
	public SearchIntegration getOrFail() {
		if ( integration == null ) {
			throw log.noIntegrationBecauseInitializationNotComplete();
		}
		return integration;
	}

	@Override
	public SearchIntegration getOrNull() {
		return integration;
	}
}
