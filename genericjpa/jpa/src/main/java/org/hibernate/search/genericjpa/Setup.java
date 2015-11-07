/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa;

import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.EntityType;
import javax.transaction.TransactionManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.genericjpa.annotations.CustomUpdateEntityProvider;
import org.hibernate.search.genericjpa.annotations.InIndex;
import org.hibernate.search.genericjpa.db.events.eclipselink.impl.EclipseLinkSynchronizedUpdateSourceProvider;
import org.hibernate.search.genericjpa.db.events.hibernate.impl.HibernateSynchronizedUpdateSourceProvider;
import org.hibernate.search.genericjpa.db.events.triggers.TriggerSQLStringSource;
import org.hibernate.search.genericjpa.entity.EntityManagerEntityProvider;
import org.hibernate.search.genericjpa.exception.SearchException;
import org.hibernate.search.genericjpa.impl.JPASearchFactoryAdapter;
import org.hibernate.search.genericjpa.db.events.jpa.impl.SQLJPAAsyncUpdateSourceProvider;
import org.hibernate.search.genericjpa.impl.SearchFactoryRegistry;
import org.hibernate.search.genericjpa.impl.AsyncUpdateSourceProvider;
import org.hibernate.search.genericjpa.impl.SynchronizedUpdateSourceProvider;
import org.hibernate.search.genericjpa.transaction.TransactionManagerProvider;

import static org.hibernate.search.genericjpa.Constants.ADDITIONAL_INDEXED_TYPES_KEY;
import static org.hibernate.search.genericjpa.Constants.BATCH_SIZE_FOR_UPDATES_DEFAULT_VALUE;
import static org.hibernate.search.genericjpa.Constants.BATCH_SIZE_FOR_UPDATES_KEY;
import static org.hibernate.search.genericjpa.Constants.SEARCH_FACTORY_TYPE_DEFAULT_VALUE;
import static org.hibernate.search.genericjpa.Constants.SEARCH_FACTORY_TYPE_KEY;
import static org.hibernate.search.genericjpa.Constants.TRANSACTION_MANAGER_PROVIDER_DEFAULT_VALUE;
import static org.hibernate.search.genericjpa.Constants.TRANSACTION_MANAGER_PROVIDER_KEY;
import static org.hibernate.search.genericjpa.Constants.TRIGGER_CREATION_STRATEGY_CREATE;
import static org.hibernate.search.genericjpa.Constants.TRIGGER_CREATION_STRATEGY_DEFAULT_VALUE;
import static org.hibernate.search.genericjpa.Constants.TRIGGER_CREATION_STRATEGY_DROP_CREATE;
import static org.hibernate.search.genericjpa.Constants.TRIGGER_CREATION_STRATEGY_KEY;
import static org.hibernate.search.genericjpa.Constants.TRIGGER_SOURCE_KEY;
import static org.hibernate.search.genericjpa.Constants.UPDATE_DELAY_DEFAULT_VALUE;
import static org.hibernate.search.genericjpa.Constants.UPDATE_DELAY_KEY;
import static org.hibernate.search.genericjpa.Constants.USE_JTA_TRANSACTIONS_DEFAULT_VALUE;
import static org.hibernate.search.genericjpa.Constants.USE_JTA_TRANSACTIONS_KEY;

/**
 * @author Martin Braun
 */
public final class Setup {

	private static final Logger LOGGER = Logger.getLogger( Setup.class.getName() );

	private Setup() {
		// can't touch this!
	}

	public static JPASearchFactoryController createSearchFactoryController(EntityManagerFactory emf) {
		return createSearchFactoryController( emf, emf.getProperties() );
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static JPASearchFactoryController createSearchFactoryController(EntityManagerFactory emf, Map properties) {
		try {
			LOGGER.info( "using hibernate-search properties: " + properties );
			LOGGER.info( "using EntityManagerFactory: " + emf );

			List<Class<?>> jpaRootTypes = new ArrayList<>();
			List<Class<?>> entities = new ArrayList<>();
			if ( emf != null ) {
				// hack... but OpenJPA wants this so it can enhance the classes.
				emf.createEntityManager().close();

				// get all the root types marked by an @InIndex and @Indexed (@Indexed isn't sufficient here!)

				emf.getMetamodel()
						.getEntities()
						.stream()
						.map( EntityType::getBindableJavaType )
						.forEach( entities::add );

				entities.stream().filter(
						(entityClass) -> entityClass.isAnnotationPresent( InIndex.class ) && entityClass.isAnnotationPresent(
								Indexed.class
						)
				).forEach(
						jpaRootTypes::add
				);
			}

			List<Class<?>> indexRootTypes = new ArrayList<>( jpaRootTypes );
			// user specified types are supported. even those that are no JPA entities!
			String additionalIndexedTypesValue = (String) properties.get( ADDITIONAL_INDEXED_TYPES_KEY );
			if ( additionalIndexedTypesValue != null ) {
				for ( String entityClassName : additionalIndexedTypesValue.split( "," ) ) {
					entityClassName = entityClassName.trim();
					LOGGER.info( "using additional indexed columnTypes: " + entityClassName );
					Class<?> entityClass = Class.forName( entityClassName );
					if ( !entityClass.isAnnotationPresent( InIndex.class ) || !entityClass.isAnnotationPresent( Indexed.class ) ) {
						throw new SearchException(
								"additional indexed columnTypes specified that doesn't host both @InIndex and @Indexed!"
						);
					}
					indexRootTypes.add( entityClass );
				}
			}

			// get the basic properties
			String name = SearchFactoryRegistry.getNameProperty( properties );
			if ( SearchFactoryRegistry.getSearchFactory( name ) != null ) {
				throw new SearchException( "there is already a searchfactory running for name: " + name + ". close it first!" );
			}

			boolean useJTATransactions = Boolean.parseBoolean(
					(String) properties.getOrDefault(
							USE_JTA_TRANSACTIONS_KEY,
							USE_JTA_TRANSACTIONS_DEFAULT_VALUE
					)
			);
			TransactionManager transactionManager = null;
			if ( useJTATransactions ) {
				LOGGER.info( "using JTA Transactions" );
				String transactionManagerClassName = (String) properties.getOrDefault(
						TRANSACTION_MANAGER_PROVIDER_KEY,
						TRANSACTION_MANAGER_PROVIDER_DEFAULT_VALUE
				);
				if ( transactionManagerClassName == null ) {
					throw new SearchException( TRANSACTION_MANAGER_PROVIDER_KEY + " must be specified when using JTA transactions!" );
				}
				//FIXME: can we do this differently?
				//we have to make sure that this method is called on the right thread/classloader, which is not ideal
				Class<TransactionManagerProvider> providerClass = (Class<TransactionManagerProvider>) Class.forName(
						transactionManagerClassName
				);
				transactionManager = providerClass.newInstance().get(
						Thread.currentThread().getContextClassLoader(),
						properties
				);
			}

			String type = (String) properties.getOrDefault(
					SEARCH_FACTORY_TYPE_KEY,
					SEARCH_FACTORY_TYPE_DEFAULT_VALUE
			);
			//what AsyncUpdateSource to be used
			SynchronizedUpdateSourceProvider synchronizedUpdateSourceProvider = (a, b, c, d, e, f, g) -> null;
			AsyncUpdateSourceProvider asyncUpdateSourceProvider = (a, b, c, d, e) -> null;
			if ( "sql".equals( type ) ) {
				if ( emf == null ) {
					throw new SearchException( "EntityManagerFactory must not be null when using " + SEARCH_FACTORY_TYPE_KEY + " of \"sql\"" );
				}
				String triggerSource = (String) properties.get( TRIGGER_SOURCE_KEY );
				Class<?> triggerSourceClass;
				if ( triggerSource == null || (triggerSourceClass = Class.forName( triggerSource )) == null ) {
					throw new SearchException(
							"class specified in " + TRIGGER_SOURCE_KEY + " could not be found: " + triggerSource
					);
				}
				String createTriggerStrategy = (String) properties.getOrDefault(
						TRIGGER_CREATION_STRATEGY_KEY,
						TRIGGER_CREATION_STRATEGY_DEFAULT_VALUE
				);
				if ( !TRIGGER_CREATION_STRATEGY_CREATE.equals( createTriggerStrategy ) && !TRIGGER_CREATION_STRATEGY_DROP_CREATE
						.equals( createTriggerStrategy ) ) {
					throw new SearchException( "unrecognized " + Constants.TRIGGER_CREATION_STRATEGY_KEY + " specified: " + createTriggerStrategy );
				}

				asyncUpdateSourceProvider = new SQLJPAAsyncUpdateSourceProvider(
						(TriggerSQLStringSource) triggerSourceClass.newInstance(),
						entities, createTriggerStrategy
				);
			}
			else if ( "manual-updates".equals( type ) ) {
				//do nothing
			}
			else if ( "eclipselink".equals( type ) ) {
				synchronizedUpdateSourceProvider = new EclipseLinkSynchronizedUpdateSourceProvider();
			}
			else if ( "hibernate".equals( type ) ) {
				synchronizedUpdateSourceProvider = new HibernateSynchronizedUpdateSourceProvider();
			}
			else {
				throw new SearchException( "unrecognized " + SEARCH_FACTORY_TYPE_KEY + ": " + type );
			}

			Integer batchSizeForUpdates = Integer
					.parseInt(
							(String) properties.getOrDefault(
									BATCH_SIZE_FOR_UPDATES_KEY,
									BATCH_SIZE_FOR_UPDATES_DEFAULT_VALUE
							)
					);
			Integer updateDelay = Integer.parseInt(
					(String) properties.getOrDefault(
							UPDATE_DELAY_KEY,
							UPDATE_DELAY_DEFAULT_VALUE
					)
			);

			//get all the CustomUpdateEntityProviders. this is not needed for manual-updates.
			//but to keep everything simple we do it here anyways
			Map<Class<?>, EntityManagerEntityProvider> customUpdateEntityProviders = indexRootTypes.stream()
					.filter( (clazz) -> clazz.isAnnotationPresent( CustomUpdateEntityProvider.class ) )
					.collect(
							Collectors.toMap(
									(clazz2) -> clazz2, (clazz3) -> {
										try {
											return (EntityManagerEntityProvider) clazz3.getAnnotation(
													CustomUpdateEntityProvider.class
											).impl().newInstance();
										}
										catch (Exception e) {
											throw new SearchException( e );
										}
									}
							)
					);

			JPASearchFactoryAdapter ret = new JPASearchFactoryAdapter();
			ret.setName( name )
					.setCustomUpdateEntityProviders( customUpdateEntityProviders )
					.setEmf( emf )
					.setUseJTATransaction( useJTATransactions )
					.setJpaRootTypes( jpaRootTypes )
					.setIndexRootTypes(
							indexRootTypes
					)
					.setProperties( properties )
					.setAsyncUpdateSourceProvider( asyncUpdateSourceProvider )
					.setSynchronizedUpdateSourceProvider( synchronizedUpdateSourceProvider )
					.setBatchSizeForUpdates(
							batchSizeForUpdates
					)
					.setUpdateDelay(
							updateDelay
					)
					.setTransactionManager( transactionManager );

			//initialize this
			ret.init();

			SearchFactoryRegistry.setup( name, ret );
			return ret;
		}

		catch (Exception e) {
			if ( !(e instanceof SearchException) ) {
				throw new SearchException( e );
			}
			else {
				throw (SearchException) e;
			}
		}
	}
}
