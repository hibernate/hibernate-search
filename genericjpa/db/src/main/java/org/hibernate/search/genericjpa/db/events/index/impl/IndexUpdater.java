/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.db.events.index.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.search.backend.spi.SingularTermDeletionQuery;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.genericjpa.db.EventType;
import org.hibernate.search.genericjpa.db.events.UpdateConsumer.UpdateEventInfo;
import org.hibernate.search.genericjpa.entity.EntityProvider;
import org.hibernate.search.genericjpa.entity.ReusableEntityProvider;
import org.hibernate.search.genericjpa.exception.SearchException;
import org.hibernate.search.genericjpa.factory.Transaction;
import org.hibernate.search.genericjpa.metadata.impl.RehashedTypeMetadata;
import org.hibernate.search.genericjpa.util.NamingThreadFactory;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.HSQuery;

/**
 * This class is the "glue" between the updating mechanism and the actual
 * Hibernate-Search index. It consumes Events coming from the AsyncUpdateSource and updates the Hibernate-Search index
 * accordingly
 *
 * @author Martin Braun
 */
public final class IndexUpdater {

	// TODO: think of a clever way of doing batching here
	// or maybe leave it as it is

	// TODO: unit test this with several batches

	private static final Logger LOGGER = Logger.getLogger( IndexUpdater.class.getName() );

	private static final int HSQUERY_BATCH = 50;

	private final Map<Class<?>, RehashedTypeMetadata> metadataForIndexRoot;
	private final Map<Class<?>, List<Class<?>>> containedInIndexOf;
	private final ReusableEntityProvider entityProvider;
	private final ExecutorService exec;
	private IndexWrapper indexWrapper;

	public IndexUpdater(
			Map<Class<?>, RehashedTypeMetadata> metadataForIndexRoot, Map<Class<?>, List<Class<?>>> containedInIndexOf,
			ReusableEntityProvider entityProvider, IndexWrapper indexWrapper) {
		this.metadataForIndexRoot = metadataForIndexRoot;
		this.containedInIndexOf = containedInIndexOf;
		this.entityProvider = entityProvider;
		this.indexWrapper = indexWrapper;
		this.exec = Executors.newSingleThreadExecutor( new NamingThreadFactory( "IndexUpdater Thread" ) );
	}

	public IndexUpdater(
			Map<Class<?>, RehashedTypeMetadata> metadataPerForIndexRoot,
			Map<Class<?>, List<Class<?>>> containedInIndexOf,
			ReusableEntityProvider entityProvider,
			ExtendedSearchIntegrator searchIntegrator) {
		this( metadataPerForIndexRoot, containedInIndexOf, entityProvider, (IndexWrapper) null );
		this.indexWrapper = new DefaultIndexWrapper( searchIntegrator );
	}

	public void updateEvent(List<UpdateEventInfo> updateInfos) {
		if(updateInfos.size() == 0) {
			return;
		}
		this.updateEvent( updateInfos, this.entityProvider );
	}

	public void updateEvent(List<UpdateEventInfo> updateInfos, EntityProvider provider) {
		//this is a hack so we can start/end our transactions properly in JTA
		//as transactions are bound to threads
		final SearchException[] exception = {null};
		final CountDownLatch latch = new CountDownLatch( 1 );
		this.exec.submit(
				() ->
				{
					try {
						if ( provider instanceof ReusableEntityProvider ) {
							((ReusableEntityProvider) provider).open();
						}
						try {
							Transaction tx = new Transaction();
							try {
								for ( UpdateEventInfo updateInfo : updateInfos ) {
									Class<?> entityClass = updateInfo.getEntityClass();
									Map<String, Object> hints = Collections.unmodifiableMap( updateInfo.getHints() );
									List<Class<?>> inIndexOf = IndexUpdater.this.containedInIndexOf.get( entityClass );
									if ( inIndexOf != null && inIndexOf.size() != 0 ) {
										int eventType = updateInfo.getEventType();
										Object id = updateInfo.getId();
										switch ( eventType ) {
											case EventType.INSERT: {
												Object obj = provider.get( entityClass, id, hints );
												if ( obj != null ) {
													IndexUpdater.this.indexWrapper.index( obj, tx );
												}
												break;
											}
											case EventType.UPDATE: {
												Object obj = provider.get( entityClass, id, hints );
												if ( obj != null ) {
													IndexUpdater.this.indexWrapper.update( obj, tx );
												}
												break;
											}
											case EventType.DELETE: {
												IndexUpdater.this.indexWrapper.delete(
														entityClass, inIndexOf, id, this.entityProvider,
														tx
												);
												break;
											}
											default: {
												LOGGER.warning( "unknown eventType-id found: " + eventType );
											}
										}
									}
									else {
										LOGGER.warning( "class: " + entityClass + " not found in any index!" );
									}
								}
								tx.commit();
							}
							catch (Exception e) {
								tx.rollback();
								LOGGER.log(
										Level.WARNING,
										"Error while updating the index! Your index might be corrupt!",
										e
								);
								exception[0] = new SearchException(
										"Error while updating the index! Your index might be corrupt!",
										e
								);
							}
						}
						finally {
							if ( provider instanceof ReusableEntityProvider ) {
								((ReusableEntityProvider) provider).close();
							}
						}
					}
					finally {
						latch.countDown();
					}

				}
		);
		try {
			latch.await();
			//while we did things on a different thread we still
			//want to throw the exceptions from there
			//so the AsyncUpdateSource stumbles on this Exception
			if ( exception[0] != null ) {
				throw exception[0];
			}
		}
		catch (InterruptedException e) {
			throw new SearchException( e );
		}

	}

	public void delete(
			Class<?> entityClass,
			List<Class<?>> inIndexOf,
			Object id,
			EntityProvider entityProvider,
			Transaction tx) {
		this.indexWrapper.delete( entityClass, inIndexOf, id, entityProvider, tx );
	}

	public void update(Object entity, Transaction tx) {
		this.indexWrapper.update( entity, tx );
	}

	public void index(Object entity, Transaction tx) {
		this.indexWrapper.update( entity, tx );
	}

	public void close() {
		this.exec.shutdown();
	}

	public interface IndexWrapper {

		void delete(
				Class<?> entityClass,
				List<Class<?>> inIndexOf,
				Object id,
				EntityProvider entityProvider,
				Transaction tx);

		void update(Object entity, Transaction tx);

		void index(Object entity, Transaction tx);

	}

	private class DefaultIndexWrapper implements IndexWrapper {

		private final ExtendedSearchIntegrator searchIntegrator;

		public DefaultIndexWrapper(ExtendedSearchIntegrator searchIntegrator) {
			this.searchIntegrator = searchIntegrator;
		}

		@Override
		public void delete(
				Class<?> entityClass,
				List<Class<?>> inIndexOf,
				Object id,
				EntityProvider entityProvider,
				Transaction tx) {
			for ( Class<?> indexClass : inIndexOf ) {
				RehashedTypeMetadata metadata = IndexUpdater.this.metadataForIndexRoot.get( indexClass );
				List<String> fields = metadata.getIdFieldNamesForType().get( entityClass );
				for ( String field : fields ) {
					DocumentFieldMetadata metaDataForIdField = metadata.getDocumentFieldMetadataForIdFieldName().get(
							field
					);
					SingularTermDeletionQuery.Type idType = metadata.getSingularTermDeletionQueryTypeForIdFieldName()
							.get( entityClass );
					Object idValueForDeletion;
					if ( idType == SingularTermDeletionQuery.Type.STRING ) {
						FieldBridge fb = metaDataForIdField.getFieldBridge();
						if ( !(fb instanceof StringBridge) ) {
							throw new IllegalArgumentException( "no TwoWayStringBridge found for field: " + field );
						}
						idValueForDeletion = ((StringBridge) fb).objectToString( id );
					}
					else {
						idValueForDeletion = id;
					}
					if ( indexClass.equals( entityClass ) ) {
						this.searchIntegrator.getWorker().performWork(
								new Work(
										entityClass,
										(Serializable) id,
										WorkType.DELETE
								), tx
						);
					}
					else {
						HSQuery hsQuery = this.searchIntegrator
								.createHSQuery()
								.targetedEntities( Collections.singletonList( indexClass ) )
								.luceneQuery(
										this.searchIntegrator.buildQueryBuilder()
												.forEntity( indexClass )
												.get()
												.keyword()
												.onField( field )
												.matching( idValueForDeletion )
												.createQuery()
								);
						int count = hsQuery.queryResultSize();
						int processed = 0;
						// this was just contained somewhere
						// so we have to update the containing entity
						while ( processed < count ) {
							for ( EntityInfo entityInfo : hsQuery.firstResult( processed ).projection(
									ProjectionConstants.ID
							).maxResults( HSQUERY_BATCH )
									.queryEntityInfos() ) {
								Serializable originalId = (Serializable) entityInfo.getProjection()[0];
								Object original = entityProvider.get( indexClass, originalId );
								if ( original != null ) {
									this.update( original, tx );
								}
								else {
									// original is not available in the
									// database, but it will be deleted by its
									// own delete event
									// TODO: log this?
								}
							}
							processed += HSQUERY_BATCH;
						}
					}
				}
			}
		}

		@Override
		public void update(Object entity, Transaction tx) {
			if ( entity != null ) {
				this.searchIntegrator.getWorker().performWork( new Work( entity, WorkType.UPDATE ), tx );
			}
		}

		@Override
		public void index(Object entity, Transaction tx) {
			if ( entity != null ) {
				this.searchIntegrator.getWorker().performWork( new Work( entity, WorkType.INDEX ), tx );
			}
		}

	}

}
