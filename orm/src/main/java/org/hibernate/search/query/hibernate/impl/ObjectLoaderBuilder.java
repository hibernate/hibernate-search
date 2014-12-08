/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.hibernate.impl;

import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.classloading.spi.ClassLoadingException;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.query.engine.spi.TimeoutManager;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class ObjectLoaderBuilder {
	private Criteria criteria;
	private List<Class<?>> targetedEntities;
	private SessionImplementor session;
	private ExtendedSearchIntegrator extendedIntegrator;
	private Set<Class<?>> indexedTargetedEntities;
	private TimeoutManager timeoutManager;
	private ObjectLookupMethod lookupMethod;
	private DatabaseRetrievalMethod retrievalMethod;
	private static final Log log = LoggerFactory.make();

	public ObjectLoaderBuilder criteria(Criteria criteria) {
		this.criteria = criteria;
		return this;
	}

	public ObjectLoaderBuilder targetedEntities(List<Class<?>> targetedEntities) {
		this.targetedEntities = targetedEntities;
		return this;
	}

	public ObjectLoaderBuilder lookupMethod(ObjectLookupMethod lookupMethod) {
		this.lookupMethod = lookupMethod;
		return this;
	}

	public ObjectLoaderBuilder retrievalMethod(DatabaseRetrievalMethod retrievalMethod) {
		this.retrievalMethod = retrievalMethod;
		return this;
	}

	public Loader buildLoader() {
		if ( criteria != null ) {
			return getCriteriaLoader();
		}
		else if ( targetedEntities.size() == 1 ) {
			return getSingleEntityLoader();
		}
		else {
			return getMultipleEntitiesLoader();
		}
	}

	private Loader getMultipleEntitiesLoader() {
		final MultiClassesQueryLoader multiClassesLoader = new MultiClassesQueryLoader();
		multiClassesLoader.init( (Session) session, extendedIntegrator, getObjectInitializer(), timeoutManager );
		multiClassesLoader.setEntityTypes( indexedTargetedEntities );
		return multiClassesLoader;
	}

	private Loader getSingleEntityLoader() {
		final QueryLoader queryLoader = new QueryLoader();
		queryLoader.init( (Session) session, extendedIntegrator, getObjectInitializer(), timeoutManager );
		queryLoader.setEntityType( targetedEntities.iterator().next() );
		return queryLoader;
	}

	private Loader getCriteriaLoader() {
		if ( targetedEntities.size() > 1 ) {
			throw new SearchException( "Cannot mix criteria and multiple entity types" );
		}
		Class entityType = targetedEntities.size() == 0 ? null : targetedEntities.iterator().next();
		if ( criteria instanceof CriteriaImpl ) {
			String targetEntity = ( (CriteriaImpl) criteria ).getEntityOrClassName();
			if ( entityType == null ) {
				ServiceManager serviceManager = extendedIntegrator.getServiceManager();
				try {
					ClassLoaderService classLoaderService = serviceManager.requestService( ClassLoaderService.class );
					entityType = classLoaderService.classForName( targetEntity );
				}
				catch (ClassLoadingException e) {
					throw new SearchException( "Unable to load entity class from criteria: " + targetEntity, e );
				}
				finally {
					serviceManager.releaseService( ClassLoaderService.class );
				}
			}
			else {
				if ( !entityType.getName().equals( targetEntity ) ) {
					throw new SearchException( "Criteria query entity should match query entity" );
				}
			}
		}
		QueryLoader queryLoader = new QueryLoader();
		queryLoader.init( (Session) session, extendedIntegrator, getObjectInitializer(), timeoutManager );
		queryLoader.setEntityType( entityType );
		queryLoader.setCriteria( criteria );
		return queryLoader;
	}

	public ObjectLoaderBuilder session(SessionImplementor session) {
		this.session = session;
		return this;
	}

	public ObjectLoaderBuilder searchFactory(ExtendedSearchIntegrator extendedIntegrator) {
		this.extendedIntegrator = extendedIntegrator;
		return this;
	}

	public ObjectLoaderBuilder indexedTargetedEntities(Set<Class<?>> indexedTargetedEntities) {
		this.indexedTargetedEntities = indexedTargetedEntities;
		return this;
	}

	public ObjectLoaderBuilder timeoutManager(TimeoutManager timeoutManager) {
		this.timeoutManager = timeoutManager;
		return this;
	}

	private ObjectInitializer getObjectInitializer() {
		log.tracef(
				"ObjectsInitializer: Use lookup method %s and database retrieval method %s",
				lookupMethod,
				retrievalMethod
		);
		if ( criteria != null && retrievalMethod != DatabaseRetrievalMethod.QUERY ) {
			throw new SearchException( "Cannot mix custom criteria query and " + DatabaseRetrievalMethod.class.getSimpleName() + "." + retrievalMethod );
		}
		final ObjectInitializer initializer;
		if ( retrievalMethod == DatabaseRetrievalMethod.FIND_BY_ID ) {
			//return early as this method does naturally 2lc + session lookup
			return LookupObjectInitializer.INSTANCE;
		}
		else if ( retrievalMethod == DatabaseRetrievalMethod.QUERY ) {
			initializer = CriteriaObjectInitializer.INSTANCE;
		}
		else {
			throw new AssertionFailure( "Unknown " + DatabaseRetrievalMethod.class.getSimpleName() + "." + retrievalMethod );
		}
		if ( lookupMethod == ObjectLookupMethod.SKIP ) {
			return initializer;
		}
		else if ( lookupMethod == ObjectLookupMethod.PERSISTENCE_CONTEXT ) {
			return new PersistenceContextObjectInitializer( initializer );
		}
		else if ( lookupMethod == ObjectLookupMethod.SECOND_LEVEL_CACHE ) {
			//we want to check the PC first, that's cheaper
			return new PersistenceContextObjectInitializer( new SecondLevelCacheObjectInitializer( initializer ) );
		}
		else {
			throw new AssertionFailure( "Unknown " + ObjectLookupMethod.class.getSimpleName() + "." + lookupMethod );
		}
		//unreachable statement
	}
}
