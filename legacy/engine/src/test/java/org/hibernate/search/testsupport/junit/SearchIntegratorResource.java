/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.junit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.junit.rules.ExternalResource;

/**
 * Test rule taking care of closing created search factories.
 * <p>
 * Useful when {@link SearchFactoryHolder} cannot be used,
 * for instance because errors are expected upon creating
 * the integrator (but may not be triggered, in which case
 * we want some cleanup).
 */
public class SearchIntegratorResource extends ExternalResource {

	private final List<SearchIntegrator> searchIntegrators = new ArrayList<>();

	public ExtendedSearchIntegrator create(SearchConfiguration configuration) {
		SearchIntegrator integrator = new SearchIntegratorBuilder().configuration( configuration ).buildSearchIntegrator();
		searchIntegrators.add( integrator );
		return integrator.unwrap( ExtendedSearchIntegrator.class );
	}

	public ExtendedSearchIntegrator create(SearchIntegrator current, Class<?> ... addedClasses) {
		SearchIntegratorBuilder builder = new SearchIntegratorBuilder()
				.currentSearchIntegrator( current );
		for ( Class<?> clazz : addedClasses ) {
			builder.addClass( clazz );
		}
		SearchIntegrator integrator = builder.buildSearchIntegrator();
		searchIntegrators.add( integrator );

		/*
		 * Avoid closing both the incremented and non-incremented factory:
		 * this is likely to fail.
		 */
		searchIntegrators.remove( current );

		return integrator.unwrap( ExtendedSearchIntegrator.class );
	}

	@Override
	protected void after() {
		Collections.reverse( searchIntegrators );
		RuntimeException exception = null;
		try {
			for ( SearchIntegrator sf : searchIntegrators ) {
				try {
					sf.close();
				}
				catch (RuntimeException e) {
					if ( exception == null ) {
						exception = e;
					}
					else {
						exception.addSuppressed( e );
					}
				}
			}
			if ( exception != null ) {
				throw exception;
			}
		}
		finally {
			searchIntegrators.clear();
		}
	}

}
