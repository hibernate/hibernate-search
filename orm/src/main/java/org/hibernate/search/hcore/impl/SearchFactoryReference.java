/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.hcore.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.service.Service;

/**
 * This Service lives in the ServiceRegistry controlled by Hibernate ORM to
 * allow to lookup the {@code ExtendedSearchIntegrator} from the context of ORM code (for example,
 * when a user wraps the current Session into a FullTextSession).
 *
 * @author Sanne Grinovero
 * @since 5.0
 */
public final class SearchFactoryReference implements Service {

	private volatile CompletableFuture<ExtendedSearchIntegrator> extendedIntegratorFuture;

	public void initialize(CompletableFuture<ExtendedSearchIntegrator> future) {
		extendedIntegratorFuture = future;
	}

	public ExtendedSearchIntegrator getSearchIntegrator() {
		final CompletableFuture<ExtendedSearchIntegrator> value = this.extendedIntegratorFuture;
		if ( value != null ) {
			return value.join();
		}
		else {
			throw LoggerFactory.make().searchIntegratorNotInitialized();
		}
	}

}
