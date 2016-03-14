/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jms.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.ejb.Stateful;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.lucene.search.Query;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.test.integration.jms.DeploymentJmsMasterSlave;
import org.hibernate.search.test.integration.jms.model.RegisteredMember;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

@Stateful
public class RegistrationController {

	private static final Log log = LoggerFactory.make();

	@PersistenceContext
	private EntityManager em;

	private RegisteredMember newMember;

	@Named
	public RegisteredMember getNewMember() {
		return newMember;
	}

	public void register() throws Exception {
		em.persist( newMember );
		resetNewMember();
	}

	public void rollbackedRegister() throws Exception {
		em.persist( newMember );
		resetNewMember();
		em.flush();
		// force to flush the backend to send the JMS messages into the queue
		Search.getFullTextEntityManager( em ).flushToIndexes();
		throw new RuntimeException( "Shit happens" );
	}

	public int deleteAllMembers() throws Exception {
		return em.createQuery( "DELETE FROM RegisteredMember" ).executeUpdate();
	}

	public RegisteredMember findById(Long id) {
		return em.find( RegisteredMember.class, id );
	}

	@SuppressWarnings("unchecked")
	public List<RegisteredMember> search(String name) {
		FullTextEntityManager fullTextEm = Search.getFullTextEntityManager( em );
		Query luceneQuery = fullTextEm.getSearchFactory().buildQueryBuilder()
				.forEntity( RegisteredMember.class ).get()
				.keyword().onField( "name" ).matching( name ).createQuery();

		return fullTextEm.createFullTextQuery( luceneQuery ).getResultList();
	}

	public List<String> searchName(String name) {
		FullTextEntityManager fullTextEm = Search.getFullTextEntityManager( em );
		Query luceneQuery = fullTextEm.getSearchFactory().buildQueryBuilder()
				.forEntity( RegisteredMember.class ).get()
				.keyword().onField( "name" ).matching( name ).createQuery();

		List<?> resultList = fullTextEm.createFullTextQuery( luceneQuery ).setProjection( "name" ).getResultList();
		List<String> names = new ArrayList<>( resultList.size() );
		for ( Object projection : resultList ) {
			names.add( (String) ( ( (Object[]) projection )[0] ) );
		}
		return names;
	}

	@PostConstruct
	public void resetNewMember() {
		newMember = new RegisteredMember();
	}

	/**
	 * Verifies this test is being run on the expected deployment and the expected
	 * backend Implementation
	 * @throws IOException
	 */
	public void assertConfiguration(String testLabel, String expectedDeploymentName, String expectedBackendImplementation) throws IOException {
		log.debug( testLabel + " / " + expectedDeploymentName );

		// Check the deployment
		ClassLoader classLoader = this.getClass().getClassLoader();
		Properties p = new Properties();
		try ( InputStream inputStream = classLoader.getResourceAsStream( DeploymentJmsMasterSlave.CONFIGURATION_PROPERTIES_RESOURCENAME ) ) {
			p.load( inputStream );
		}
		String actualDeployment = p.getProperty( "deploymentName" );
		if ( actualDeployment == null ) {
			throw new IllegalStateException( "Deployment Name not found in properties" );
		}
		if ( ! expectedDeploymentName.equals( actualDeployment ) ) {
			throw new IllegalStateException( "Was expecting to run on deployment " + expectedDeploymentName + " but is running on " + actualDeployment
					+ ". Defined by looking into classloader: " + classLoader );
		}

		// Check the running backend type
		ExtendedSearchIntegrator searchIntegrator = Search.getFullTextEntityManager( em ).getSearchFactory().unwrap( ExtendedSearchIntegrator.class );
		BackendQueueProcessor backendQueueProcessor = searchIntegrator.getIndexManagerHolder().getBackendQueueProcessor( "membersIndex" );
		final String backendName = backendQueueProcessor.getClass().getName();
		if ( ! backendName.equals( expectedBackendImplementation ) ) {
			throw new IllegalStateException( "Not running the expected backend '" + expectedBackendImplementation + "' but running '" + backendName + "'" );
		}
	}

}
