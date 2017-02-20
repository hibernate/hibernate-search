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

import org.hibernate.SessionFactory;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.hcore.util.impl.ContextHelper;
import org.hibernate.search.jsr352.massindexing.impl.JobContextData;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.jboss.logging.Logger;

/**
 * ItemProcessor receives entities coming from item reader and process then into an AddLuceneWorks. Only one entity is
 * received and processed at each time.
 *
 * @author Mincong Huang
 */
public class LuceneDocProducer implements ItemProcessor {

	private static final Logger LOGGER = Logger.getLogger( LuceneDocProducer.class );

	@Inject
	private JobContext jobContext;

	@Inject
	@BatchProperty
	private String entityName;

	private EntityManagerFactory emf;

	private ExtendedSearchIntegrator searchIntegrator;
	private EntityIndexBinding entityIndexBinding;
	private DocumentBuilderIndexedEntity docBuilder;
	private boolean isSetup = false;
	private IndexedTypeIdentifier entityTypeIdentifier;

	@Override
	public Object processItem(Object item) throws Exception {
		LOGGER.debug( "processing item ..." );
		if ( !isSetup ) {
			setup();
			isSetup = true;
		}
		AddLuceneWork addWork = buildAddLuceneWork( item );
		return addWork;
	}

	/**
	 * Set up environment for lucene work production.
	 *
	 * @throws ClassNotFoundException if the entityName does not match any indexed class type in the job context data.
	 * @throws NamingException if JNDI lookup for entity manager failed
	 */
	private void setup() throws ClassNotFoundException, NamingException {
		JobContextData jobContextData = (JobContextData) jobContext.getTransientUserData();
		Class<?> entityType = jobContextData.getIndexedType( entityName );
		entityTypeIdentifier = new PojoIndexedTypeIdentifier( entityType );
		emf = jobContextData.getEntityManagerFactory();
		searchIntegrator = ContextHelper.getSearchIntegratorBySF( emf.unwrap( SessionFactory.class ) );
		entityIndexBinding = searchIntegrator.getIndexBindings().get( entityTypeIdentifier );
		docBuilder = entityIndexBinding.getDocumentBuilder();
	}

	/**
	 * Build addLuceneWork using input entity. This method is inspired by the current mass indexer implementation.
	 *
	 * @param entity selected entity, obtained from JPA entity manager. It is used to build Lucene work.
	 * @return an addLuceneWork
	 */
	private AddLuceneWork buildAddLuceneWork(Object entity) {
		// TODO: tenant ID should not be null
		// Or may it be fine to be null? Gunnar's integration test in Hibernate
		// Search: MassIndexingTimeoutIT does not mention the tenant ID neither
		// (The tenant ID is not included mass indexer setup in the
		// ConcertManager)
		String tenantId = null;
		ConversionContext conversionContext = new ContextualExceptionBridgeHelper();

		Serializable id = (Serializable) emf.getPersistenceUnitUtil()
				.getIdentifier( entity );
		TwoWayFieldBridge idBridge = docBuilder.getIdBridge();
		conversionContext.pushIdentifierProperty();
		String idInString = null;
		try {
			idInString = conversionContext
					.setConvertedTypeId( entityTypeIdentifier )
					.twoWayConversionContext( idBridge )
					.objectToString( id );
			LOGGER.debugf( "idInString=%s", idInString );
		}
		finally {
			conversionContext.popProperty();
		}
		AddLuceneWork addWork = docBuilder.createAddWork(
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
		return addWork;
	}
}
