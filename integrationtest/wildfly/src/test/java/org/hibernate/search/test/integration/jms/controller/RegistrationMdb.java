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
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.hibernate.search.backend.jms.spi.AbstractJMSHibernateSearchController;
import org.hibernate.search.orm.spi.SearchIntegratorHelper;
import org.hibernate.search.spi.SearchIntegrator;

@MessageDriven(activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = RegistrationMdb.DESTINATION_QUEUE) })
public class RegistrationMdb extends AbstractJMSHibernateSearchController implements MessageListener {

	public static final String DESTINATION_QUEUE = "jms/queue/hsearch";

	@PersistenceUnit
	private EntityManagerFactory emf;

	@Override
	protected SearchIntegrator getSearchIntegrator() {
		return SearchIntegratorHelper.extractFromEntityManagerFactory( emf );
	}

}
