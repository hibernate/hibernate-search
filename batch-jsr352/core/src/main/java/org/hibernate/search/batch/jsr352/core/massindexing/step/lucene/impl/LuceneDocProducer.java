/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batch.jsr352.core.massindexing.step.lucene.impl;

import java.lang.invoke.MethodHandles;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemProcessor;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.batch.jsr352.core.logging.impl.Log;
import org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJobParameters;
import org.hibernate.search.batch.jsr352.core.massindexing.impl.JobContextData;
import org.hibernate.search.batch.jsr352.core.massindexing.util.impl.MassIndexingPartitionProperties;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * ItemProcessor receives entities coming from item reader and process then into an AddLuceneWorks. Only one entity is
 * received and processed at each time.
 *
 * @author Mincong Huang
 */
public class LuceneDocProducer implements ItemProcessor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Inject
	private JobContext jobContext;

	@Inject
	@BatchProperty(name = MassIndexingPartitionProperties.ENTITY_NAME)
	private String entityName;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.TENANT_ID)
	private String tenantId;

	private EntityManagerFactory emf;

	private boolean isSetup = false;

	@Override
	public Object processItem(Object item) throws Exception {
		log.processEntity( item );
		if ( !isSetup ) {
			setup();
			isSetup = true;
		}
		// TODO HSEARCH-3269 restore processing
		// return buildWork( item );
		return null;
	}

	/**
	 * Set up environment for lucene work production.
	 *
	 * @throws ClassNotFoundException if the entityName does not match any indexed class type in the job context data.
	 * @throws NamingException if JNDI lookup for entity manager failed
	 */
	private void setup() throws ClassNotFoundException, NamingException {
		JobContextData jobContextData = (JobContextData) jobContext.getTransientUserData();
		Class<?> entityType = jobContextData.getEntityType( entityName );
		emf = jobContextData.getEntityManagerFactory();
	}
}
