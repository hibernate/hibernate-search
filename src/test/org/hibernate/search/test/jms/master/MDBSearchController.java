//$Id$
package org.hibernate.search.test.jms.master;

import javax.ejb.MessageDriven;
import javax.ejb.ActivationConfigProperty;
import javax.jms.MessageListener;

import org.hibernate.search.backend.impl.jms.AbstractJMSHibernateSearchController;
import org.hibernate.Session;

/**
 * @author Emmanuel Bernard
 */
@MessageDriven(activationConfig = {
      @ActivationConfigProperty(propertyName="destinationType", propertyValue="javax.jms.Queue"),
      @ActivationConfigProperty(propertyName="destination", propertyValue="queue/searchtest"),
      @ActivationConfigProperty(propertyName="DLQMaxResent", propertyValue="1")
   } )
public class MDBSearchController extends AbstractJMSHibernateSearchController implements MessageListener {
	protected Session getSession() {
		return MyHibernateUtil.sessionFactory.openSession( );
	}

	protected void cleanSessionIfNeeded(Session session) {
		session.close();
	}
}
