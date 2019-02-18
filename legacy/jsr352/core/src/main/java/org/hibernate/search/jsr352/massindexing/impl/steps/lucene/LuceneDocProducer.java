/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.steps.lucene;

import java.io.Serializable;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemProcessor;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.jsr352.logging.impl.Log;
import org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters;
import org.hibernate.search.jsr352.massindexing.impl.JobContextData;
import org.hibernate.search.jsr352.massindexing.impl.util.MassIndexingPartitionProperties;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

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

	private ExtendedSearchIntegrator searchIntegrator;
	private EntityIndexBinding entityIndexBinding;
	private DocumentBuilderIndexedEntity docBuilder;
	private boolean isSetup = false;
	private IndexedTypeIdentifier entityTypeIdentifier;

	@Override
	public Object processItem(Object item) throws Exception {
		log.processEntity( item );
		if ( !isSetup ) {
			setup();
			isSetup = true;
		}
		return buildWork( item );
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
		entityTypeIdentifier = new PojoIndexedTypeIdentifier( entityType );
		searchIntegrator = jobContextData.getSearchIntegrator();
		entityIndexBinding = searchIntegrator.getIndexBindings().get( entityTypeIdentifier );
		docBuilder = entityIndexBinding.getDocumentBuilder();
		emf = jobContextData.getEntityManagerFactory();
	}

	private LuceneWork buildWork(Object entity) {
		ConversionContext conversionContext = new ContextualExceptionBridgeHelper();

		Serializable id = (Serializable) emf.getPersistenceUnitUtil()
				.getIdentifier( entity );
		TwoWayFieldBridge idBridge = docBuilder.getIdBridge();
		conversionContext.pushIdentifierProperty();
		String idInString;
		try {
			idInString = conversionContext
					.setConvertedTypeId( entityTypeIdentifier )
					.twoWayConversionContext( idBridge )
					.objectToString( id );
		}
		finally {
			conversionContext.popProperty();
		}
		// The default value of job parameter is the empty string "" in JSR-352 batch runtime (Spec 1.0, §8.8.1.5), but
		// the default value of tenant identifier should be null in Hibernate Search.
		if ( StringHelper.isEmpty( tenantId ) ) {
			tenantId = null;
		}

		/*
		 * Always create an add work, and let the writer decide whether
		 * an update work should be executed instead.
		 */
		return docBuilder.createAddWork(
				tenantId,
				entityTypeIdentifier,
				entity,
				id,
				idInString,
				/*
				 * Use the default instance initializer (likely HibernateStatelessInitializer),
				 * because we don't need the fancy features provided by HibernateSessionLoadingInitializer:
				 * in our case, we never mix entities from different sessions, since
				 * each partition uses its own session.
				 */
				null,
				conversionContext );
	}
}
