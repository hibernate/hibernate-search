/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.batch.api.BatchProperty;
import javax.batch.api.listener.AbstractJobListener;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.criterion.Criterion;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.jsr352.context.jpa.EntityManagerFactoryRegistry;
import org.hibernate.search.jsr352.context.jpa.impl.ActiveSessionFactoryRegistry;
import org.hibernate.search.jsr352.massindexing.impl.JobContextData;
import org.hibernate.search.jsr352.massindexing.impl.util.MassIndexerUtil;
import org.hibernate.search.util.StringHelper;
import org.jboss.logging.Logger;

/**
 * Listener before the start of the job. It aims to setup the job context data, shared by all the steps.
 * <p>
 * This is the default implementation, meant to be overridden (mainly its
 * {@link #getEntityManagerFactoryRegistry()} method) when one needs to retrieve the entity manager
 * factories differently.
 * This default implementation uses an {@link ActiveSessionFactoryRegistry}, which has
 * some limitations (see its javadoc).
 *
 * @author Mincong Huang
 */
public class JobContextSetupListener extends AbstractJobListener {

	private static final Logger LOGGER = Logger.getLogger( JobContextSetupListener.class );

	@Inject
	private JobContext jobContext;

	@Inject
	@BatchProperty
	private String entityManagerFactoryScope;

	@Inject
	@BatchProperty
	private String entityManagerFactoryReference;

	@Inject
	@BatchProperty
	private String rootEntities;

	@Inject
	@BatchProperty(name = "criteria")
	private String serializedCriteria;

	@Override
	public void beforeJob() throws Exception {
		setUpContext();
	}

	/**
	 * Method to be overridden to retrieve the entity manager factory by different means (CDI, Spring DI, ...).
	 *
	 * @return The entity manager factory registry used to convert the entity manager factory reference to an actual instance.
	 */
	protected EntityManagerFactoryRegistry getEntityManagerFactoryRegistry() {
		return ActiveSessionFactoryRegistry.getInstance();
	}

	private EntityManagerFactory getEntityManagerFactory() {
		EntityManagerFactoryRegistry registry = getEntityManagerFactoryRegistry();

		if ( StringHelper.isEmpty( entityManagerFactoryScope ) ) {
			if ( StringHelper.isEmpty( entityManagerFactoryReference ) ) {
				return registry.getDefault();
			}
			else {
				return registry.get( entityManagerFactoryReference );
			}
		}
		else {
			if ( StringHelper.isEmpty( entityManagerFactoryReference ) ) {
				throw new SearchException( "An 'entityManagerFactoryScope' was defined, but"
						+ " the 'entityManagerFactoryReference' parameter is empty."
						+ " Please also set the 'entityManagerFactoryReference' parameter to"
						+ " select an entity manager factory, or do not set the"
						+ " 'entityManagerFactoryScope' to try to use a default entity manager factory." );
			}
			else {
				return registry.get( entityManagerFactoryScope, entityManagerFactoryReference );
			}
		}
	}

	private void setUpContext() throws ClassNotFoundException, IOException {
		EntityManagerFactory emf = getEntityManagerFactory();
		EntityManager em = null;

		try {
			LOGGER.debug( "Creating entity manager ..." );

			em = emf.createEntityManager();
			List<String> entityNamesToIndex = Arrays.asList( rootEntities.split( "," ) );
			Set<Class<?>> entityTypesToIndex = Search
					.getFullTextEntityManager( em )
					.getSearchFactory()
					.getIndexedTypes()
					.stream()
					.filter( clz -> entityNamesToIndex.contains( clz.getName() ) )
					.collect( Collectors.toCollection( HashSet::new ) );

			Set<Criterion> criteria = MassIndexerUtil.deserializeCriteria( serializedCriteria );
			LOGGER.infof( "%d criteria found.", criteria.size() );

			JobContextData jobContextData = new JobContextData();
			jobContextData.setEntityManagerFactory( emf );
			jobContextData.setCriteria( criteria );
			jobContextData.setEntityTypes( entityTypesToIndex );
			jobContext.setTransientUserData( jobContextData );
		}
		finally {
			try {
				em.close();
			}
			catch (Exception e) {
				LOGGER.error( e );
			}
		}
	}

}
