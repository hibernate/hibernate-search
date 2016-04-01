/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.db.events.jpa.impl;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.db.EventType;
import org.hibernate.search.db.events.impl.AsyncUpdateSource;
import org.hibernate.search.db.events.impl.AsyncUpdateSourceProvider;
import org.hibernate.search.db.events.impl.EventModelInfo;
import org.hibernate.search.db.events.triggers.TriggerSQLStringSource;
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
			EntityManagerFactoryWrapper emf,
			List<EventModelInfo> eventModelInfos) {
		this.setupTriggers( emf, eventModelInfos );
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
			List<EventModelInfo> eventModelInfos) {
		switch ( this.triggerCreateStrategy ) {
			case AsyncUpdateConstants.TRIGGER_CREATION_STRATEGY_DROP_CREATE:
				this.dropDDL( emf, eventModelInfos );
			case AsyncUpdateConstants.TRIGGER_CREATION_STRATEGY_CREATE_DROP:
				//we drop at shutdown with this
				//this has to be handled outside of this class
			case AsyncUpdateConstants.TRIGGER_CREATION_STRATEGY_CREATE:
				this.createDDL( emf, eventModelInfos );
			case AsyncUpdateConstants.TRIGGER_CREATION_STRATEGY_DONT_CREATE:
			default:
				return;
		}
	}

	public void dropDDL(EntityManagerFactoryWrapper emf, List<EventModelInfo> eventModelInfos) {
		//DROP EVERYTHING IN THE EXACTLY INVERSE ORDER WE CREATE IT
		for ( EventModelInfo info : eventModelInfos ) {

			for ( EventType eventType : EventType.values() ) {
				String[] triggerDropStrings = this.triggerSource.getTriggerDropCode( info, eventType );
				for ( String triggerDropString : triggerDropStrings ) {
					log.triggerCreationSQL( triggerDropString );
					this.doQueryOrLogException(
							emf,
							triggerDropString,
							true
					);
				}
			}

			for ( String unSetupCode : this.triggerSource.getSpecificUnSetupCode( info ) ) {
				log.triggerCreationSQL( unSetupCode );
				this.doQueryOrLogException( emf, unSetupCode, true );
			}

			for ( String str : triggerSource.getUpdateTableDropCode( info ) ) {
				log.triggerCreationSQL( str );
				this.doQueryOrLogException( emf, str, true );
			}

		}

		for ( String str : triggerSource.getUnSetupCode() ) {
			log.triggerCreationSQL( str );
			this.doQueryOrLogException( emf, str, true );
		}

		log.trace( "finished dropping triggers!" );
	}

	private void createDDL(EntityManagerFactoryWrapper emf, List<EventModelInfo> eventModelInfos) {
		for ( String str : triggerSource.getSetupCode() ) {
			log.triggerCreationSQL( str );
			this.doQueryOrLogException( emf, str, false );
		}

		for ( EventModelInfo info : eventModelInfos ) {
			for ( String str : triggerSource.getUpdateTableCreationCode( info ) ) {
				log.triggerCreationSQL( str );
				this.doQueryOrLogException( emf, str, false );
			}

			for ( String setupCode : this.triggerSource.getSpecificSetupCode( info ) ) {
				log.triggerCreationSQL( setupCode );
				this.doQueryOrLogException( emf, setupCode, false );
			}

			for ( EventType eventType : EventType.values() ) {
				String[] triggerCreationStrings = this.triggerSource.getTriggerCreationCode(
						info,
						eventType
				);
				for ( String triggerCreationString : triggerCreationStrings ) {
					log.triggerCreationSQL( triggerCreationString );
					this.doQueryOrLogException(
							emf,
							triggerCreationString,
							false
					);
				}
			}
		}

		log.trace( "finished setting up triggers!" );
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
