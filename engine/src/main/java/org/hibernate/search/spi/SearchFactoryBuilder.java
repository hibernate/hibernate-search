/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.spi;

import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;

/**
 * @deprecated Use SearchIntegrationBuilder instead. This class will be removed!
 */
@Deprecated
public class SearchFactoryBuilder extends SearchIntegratorBuilder {

	public SearchFactoryBuilder configuration(SearchConfiguration configuration) {
		super.configuration( configuration );
		return this;
	}

	public SearchFactoryBuilder currentFactory(SearchIntegrator factory) {
		super.currentSearchIntegrator( factory );
		return this;
	}

	public SearchFactoryBuilder addClass(Class<?> clazz) {
		super.addClass( clazz );
		return this;
	}

	public ExtendedSearchIntegrator buildSearchFactory() {
		return super.buildSearchIntegrator().unwrap( ExtendedSearchIntegrator.class );
	}

}
