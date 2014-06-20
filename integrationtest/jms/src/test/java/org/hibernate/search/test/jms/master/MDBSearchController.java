/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jms.master;

import org.hibernate.search.backend.impl.jms.AbstractJMSHibernateSearchController;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * @author Emmanuel Bernard
 */
public class MDBSearchController extends AbstractJMSHibernateSearchController {

	SessionFactory sessionFactory;

	MDBSearchController( SessionFactory sessionFactory ) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	protected Session getSession() {
		return sessionFactory.openSession( );
	}

	@Override
	protected void cleanSessionIfNeeded(Session session) {
		session.close();
	}
}
