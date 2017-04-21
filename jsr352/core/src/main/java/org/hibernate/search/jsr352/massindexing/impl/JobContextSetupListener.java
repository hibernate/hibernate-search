/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl;

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
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.criterion.Criterion;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.jsr352.context.jpa.EntityManagerFactoryRegistry;
import org.hibernate.search.jsr352.context.jpa.impl.ActiveSessionFactoryRegistry;
import org.hibernate.search.jsr352.logging.impl.Log;
import org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters;
import org.hibernate.search.jsr352.massindexing.impl.util.MassIndexerUtil;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Listener before the start of the job. It aims to setup the job context data, shared by all the steps.
 *
 * @author Mincong Huang
 */
/*
 * Hack to make sure that, when using dependency injection,
 * this bean is resolved using DI and is properly injected.
 * Otherwise it would just be instantiated using its default
 * constructor and would not be injected.
 */
@Named(value = "org.hibernate.search.jsr352.massindexing.impl.JobContextSetupListener")
@Singleton
public class JobContextSetupListener extends AbstractJobListener {

	private static final Log log = LoggerFactory.make( Log.class );

	@Inject
	private JobContext jobContext;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_SCOPE)
	private String entityManagerFactoryScope;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_REFERENCE)
	private String entityManagerFactoryReference;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.ENTITY_TYPES)
	private String entityTypes;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.CUSTOM_QUERY_CRITERIA)
	private String serializedCustomQueryCriteria;

	@Inject
	private EntityManagerFactoryRegistry emfRegistry;

	@Override
	public void beforeJob() throws Exception {
		setUpContext();
	}

	private EntityManagerFactory getEntityManagerFactory() {
		EntityManagerFactoryRegistry registry =
				emfRegistry != null ? emfRegistry : ActiveSessionFactoryRegistry.getInstance();

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
				throw log.entityManagerFactoryReferenceIsEmpty();
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
			em = emf.createEntityManager();
			List<String> entityNamesToIndex = Arrays.asList( entityTypes.split( "," ) );
			Set<Class<?>> entityTypesToIndex = Search
					.getFullTextEntityManager( em )
					.getSearchFactory()
					.getIndexedTypes()
					.stream()
					.filter( clz -> entityNamesToIndex.contains( clz.getName() ) )
					.collect( Collectors.toCollection( HashSet::new ) );

			Set<Criterion> criteria = MassIndexerUtil.deserializeCriteria( serializedCustomQueryCriteria );
			log.criteriaSize( criteria.size() );

			JobContextData jobContextData = new JobContextData();
			jobContextData.setEntityManagerFactory( emf );
			jobContextData.setCustomQueryCriteria( criteria );
			jobContextData.setEntityTypes( entityTypesToIndex );
			jobContext.setTransientUserData( jobContextData );
		}
		finally {
			try {
				em.close();
			}
			catch (Exception e) {
				log.unableToCloseEntityManager( e );
			}
		}
	}

}
