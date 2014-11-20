/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jms.master;

import org.hibernate.search.backend.impl.jms.AbstractJMSHibernateSearchController;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.spi.SearchIntegrator;

/**
 * @author Emmanuel Bernard
 */
public class MDBSearchController extends AbstractJMSHibernateSearchController {

	final SearchIntegrator searchFactory;

	MDBSearchController( SearchFactoryImplementor searchFactory ) {
		this.searchFactory = searchFactory;
	}

	@Override
	protected SearchIntegrator getSearchIntegrator() {
		return searchFactory;
	}

}
