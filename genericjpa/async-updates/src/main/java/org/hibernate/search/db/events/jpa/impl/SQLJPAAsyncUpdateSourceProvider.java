/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.db.events.jpa.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.db.EventType;
import org.hibernate.search.db.events.impl.AnnotationEventModelParser;
import org.hibernate.search.db.events.impl.AsyncUpdateSource;
import org.hibernate.search.db.events.impl.EventModelInfo;
import org.hibernate.search.db.events.impl.EventModelParser;
import org.hibernate.search.db.events.triggers.TriggerSQLStringSource;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.db.events.impl.AsyncUpdateSourceProvider;
import org.hibernate.search.db.util.impl.EntityManagerFactoryWrapper;
import org.hibernate.search.db.util.impl.EntityManagerWrapper;
import org.hibernate.search.db.util.impl.TransactionWrapper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Martin Braun
 */
public class SQLJPAAsyncUpdateSourceProvider implements AsyncUpdateSourceProvider {

	private static final Log log = LoggerFactory.make();

	private final TriggerSQLStringSource triggerSource;
	private final List<Class<?>> entityClasses;
	private final String triggerCreateStrategy;

	public SQLJPAAsyncUpdateSourceProvider(
			TriggerSQLStringSource triggerSource,
			List<Class<?>> entityClasses,
			String triggerCreateStrategy) {
		this.triggerSource = triggerSource;
		this.entityClasses = entityClasses;
		this.triggerCreateStrategy = triggerCreateStrategy;
	}

	@Override
	public AsyncUpdateSource getUpdateSource(
			long delay,
			TimeUnit timeUnit,
			int batchSizeForUpdates,
			Properties properties,
			EntityManagerFactoryWrapper emf) {
		EventModelParser eventModelParser = new AnnotationEventModelParser();
		List<EventModelInfo> eventModelInfos = eventModelParser.parse( new ArrayList<>( this.entityClasses ) );
		this.setupTriggers( emf, eventModelInfos, properties );
		JPAUpdateSource updateSource = new JPAUpdateSource(
				eventModelInfos,
				emf,
				delay,
				timeUnit,
				batchSizeForUpdates,
				Integer.parseInt(
						properties.getProperty(
								AsyncUpdateConstants.BATCH_SIZE_FOR_UPDATE_QUERIES_KEY,
								AsyncUpdateConstants.BATCH_SIZE_FOR_UPDATE_QUERIES_DEFAULT_VALUE
						)
				), this.triggerSource.getDelimitedIdentifierToken()
		);
		return updateSource;
	}

	private void setupTriggers(
			EntityManagerFactoryWrapper emf,
			List<EventModelInfo> eventModelInfos,
			Properties properties) {
		if ( AsyncUpdateConstants.TRIGGER_CREATION_STRATEGY_DONT_CREATE.equals( this.triggerCreateStrategy ) || (!AsyncUpdateConstants.TRIGGER_CREATION_STRATEGY_CREATE
				.equals( this.triggerCreateStrategy ) && !AsyncUpdateConstants.TRIGGER_CREATION_STRATEGY_DROP_CREATE.equals(
				this.triggerCreateStrategy
		)) ) {
			return;
		}

		try {
			try {
				if ( AsyncUpdateConstants.TRIGGER_CREATION_STRATEGY_DROP_CREATE.equals( this.triggerCreateStrategy ) ) {
					//DROP EVERYTHING IN THE EXACTLY INVERSED ORDER WE CREATE IT
					for ( EventModelInfo info : eventModelInfos ) {

						for ( int eventType : EventType.values() ) {
							String[] triggerDropStrings = this.triggerSource.getTriggerDropCode( info, eventType );
							for ( String triggerDropString : triggerDropStrings ) {
								log.trace( triggerDropString );
								this.doQueryOrLogException(
										emf,
										triggerDropString,
										true
								);
							}
						}

						for ( String unSetupCode : this.triggerSource.getSpecificUnSetupCode( info ) ) {
							log.trace( unSetupCode );
							this.doQueryOrLogException( emf, unSetupCode, true );
						}

						for ( String str : triggerSource.getUpdateTableDropCode( info ) ) {
							log.trace( str );
							this.doQueryOrLogException( emf, str, true );
						}

					}

					for ( String str : triggerSource.getUnSetupCode() ) {
						log.trace( str );
						this.doQueryOrLogException( emf, str, true );
					}
				}

				//CREATE EVERYTHING
				try {
					for ( String str : triggerSource.getSetupCode() ) {
						log.trace( str );
						this.doQueryOrLogException( emf, str, false );
					}

					for ( EventModelInfo info : eventModelInfos ) {
						for ( String str : triggerSource.getUpdateTableCreationCode( info ) ) {
							log.trace( str );
							this.doQueryOrLogException( emf, str, false );
						}

						for ( String setupCode : this.triggerSource.getSpecificSetupCode( info ) ) {
							log.trace( setupCode );
							this.doQueryOrLogException( emf, setupCode, false );
						}

						for ( int eventType : EventType.values() ) {
							String[] triggerCreationStrings = this.triggerSource.getTriggerCreationCode(
									info,
									eventType
							);
							for ( String triggerCreationString : triggerCreationStrings ) {
								log.trace( triggerCreationString );
								this.doQueryOrLogException(
										emf,
										triggerCreationString,
										false
								);
							}
						}
					}
				}
				catch (Exception e) {
					throw new SearchException( e );
				}
				log.trace( "finished setting up triggers!" );
			}
			finally {

			}
		}
		catch (Exception e) {
			throw new SearchException( e );
		}
	}

	private void doQueryOrLogException(
			EntityManagerFactoryWrapper emf,
			String query,
			boolean canFail) {
		try {
			//we use a new EntityManager here everytime, because
			//if we get an error during trigger creation
			//(which is allowed, since we don't have logic
			//to check with IF EXISTS on every database)
			//the EntityManager can be in a RollbackOnly state
			//which we dont want
			EntityManagerWrapper em = emf.createEntityManager();
			try {
				TransactionWrapper tx = em.getTransaction();
				tx.setIgnoreExceptionsForJTATransaction( true );
				tx.begin();

				em.createNativeQuery( query ).executeUpdate();

				tx.commitIgnoreExceptions();
			}
			finally {
				em.close();
			}
		}
		catch (Exception e) {
			if ( canFail ) {
				log.exceptionOccurred(
						"Exception occured during setup of triggers (most of the time, this is okay)",
						e
				);
			}
		}
	}

}
