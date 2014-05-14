/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jms.controller;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.MessageListener;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.Session;
import org.hibernate.search.backend.impl.jms.AbstractJMSHibernateSearchController;
import org.hibernate.search.test.integration.jms.util.RegistrationConfiguration;

@MessageDriven(activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = RegistrationConfiguration.DESTINATION_QUEUE) })
public class RegistrationMdb extends AbstractJMSHibernateSearchController implements MessageListener {

	@PersistenceContext
	private EntityManager em;

	@Override
	protected Session getSession() {
		return (Session) em.getDelegate();
	}

	@Override
	protected void cleanSessionIfNeeded(Session session) {
	}

}
