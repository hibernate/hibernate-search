/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal;

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
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.hibernate.search.jpa.Search;
import org.hibernate.search.jsr352.internal.se.JobSEEnvironment;
import org.hibernate.search.jsr352.internal.util.MassIndexerUtil;
import org.jboss.logging.Logger;

/**
 * Listener before the start of the job. It aims to setup the job context data, shared by all the steps.
 *
 * @author Mincong Huang
 */
@Named
public class JobContextSetupListener extends AbstractJobListener {

	private static final Logger LOGGER = Logger.getLogger( JobContextSetupListener.class );
	private final JobContext jobContext;

	@Inject
	@BatchProperty
	private boolean isJavaSE;

	@Inject
	@BatchProperty
	private String rootEntities;

	@Inject
	@BatchProperty(name = "jobContextData")
	private String serializedJobContextData;

	@PersistenceUnit(unitName = "h2")
	private EntityManagerFactory emf;

	@Inject
	public JobContextSetupListener(JobContext jobContext) {
		this.jobContext = jobContext;
	}

	@Override
	public void beforeJob() throws Exception {

		EntityManager em = null;

		try {
			LOGGER.debug( "Creating entity manager ..." );
			if ( isJavaSE ) {
				emf = JobSEEnvironment.getInstance().getEntityManagerFactory();
			}
			em = emf.createEntityManager();
			List<String> entityNamesToIndex = Arrays.asList( rootEntities.split( "," ) );
			Set<Class<?>> entityTypesToIndex = Search
					.getFullTextEntityManager( em )
					.getSearchFactory()
					.getIndexedTypes()
					.stream()
					.filter( clz -> entityNamesToIndex.contains( clz.getName() ) )
					.collect( Collectors.toCollection( HashSet::new ) );

			JobContextData jobContextData = MassIndexerUtil
					.deserializeJobContextData( serializedJobContextData );
			LOGGER.infof( "%d criterions found.", jobContextData.getCriterions().size() );
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

	@Override
	public void afterJob() throws Exception {
		if ( isJavaSE ) {
			JobSEEnvironment.getInstance().setEntityManagerFactory( null );
		}
	}
}
