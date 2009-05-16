//$Id$
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

	protected Session getSession() {
		return sessionFactory.openSession( );
	}

	protected void cleanSessionIfNeeded(Session session) {
		session.close();
	}
}
