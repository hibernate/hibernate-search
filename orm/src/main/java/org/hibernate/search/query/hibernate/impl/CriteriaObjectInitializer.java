/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.hibernate.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.Criteria;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.search.cfg.spi.IdUniquenessResolver;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.TimeoutManager;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.IndexedTypeSet;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.util.impl.ReflectionHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Initialize object using one or several criteria queries.
 *
 * @author Emmanuel Bernard
 * @author Gunnar Morling
 * @author Guillaume Smet
 */
public class CriteriaObjectInitializer implements ObjectInitializer {

	private static final Log log = LoggerFactory.make();
	private static final int MAX_IN_CLAUSE = 500;

	public static final CriteriaObjectInitializer INSTANCE = new CriteriaObjectInitializer();

	private CriteriaObjectInitializer() {
		// use INSTANCE instead of constructor
	}

	@Override
	public void initializeObjects(List<EntityInfo> entityInfos,
			LinkedHashMap<EntityInfoLoadKey, Object> idToObjectMap,
			ObjectInitializationContext objectInitializationContext) {
		// Do not call isTimeOut here as the caller might be the last biggie on the list.
		final int maxResults = entityInfos.size();
		if ( log.isTraceEnabled() ) {
			log.tracef( "Load %d objects using criteria queries", (Integer) maxResults );
		}

		if ( maxResults == 0 ) {
			return;
		}

		List<Criteria> criterias = buildUpCriteria( entityInfos, objectInitializationContext );

		for ( Criteria criteria : criterias ) {
			setCriteriaTimeout( criteria, objectInitializationContext.getTimeoutManager() );

			@SuppressWarnings("unchecked")
			List<Object> queryResultList = criteria.list();
			final ExtendedSearchIntegrator integrator = objectInitializationContext.getExtendedSearchIntegrator();
			final InstanceInitializer instanceInitializer = integrator.getInstanceInitializer();
			for ( Object o : queryResultList ) {
				Class<?> loadedType = instanceInitializer.getClass( o );
				Object unproxiedObject = instanceInitializer.unproxy( o );
				IndexedTypeIdentifier type = integrator.getIndexBindings().keyFromPojoType( loadedType );
				DocumentBuilderIndexedEntity documentBuilder = getDocumentBuilder( type, integrator );
				if ( documentBuilder == null ) {
					// the query result can contain entities which are not indexed. This can for example happen if
					// the targeted entity type is a superclass with indexed and un-indexed sub classes
					// entities which don't have a document builder can be ignored (HF)
					continue;
				}
				XMember idProperty = documentBuilder.getIdGetter();
				Object id = ReflectionHelper.getMemberValue( unproxiedObject, idProperty );
				EntityInfoLoadKey key = new EntityInfoLoadKey( loadedType, id );
				Object previousValue = idToObjectMap.put( key, unproxiedObject );
				if ( previousValue == null ) {
					throw new AssertionFailure( "An entity got loaded even though it was not part of the EntityInfo list" );
				}
			}
		}
	}

	private void setCriteriaTimeout(Criteria criteria, TimeoutManager timeoutManager) {
		// not best effort so fail fast
		if ( timeoutManager.getType() != TimeoutManager.Type.LIMIT ) {
			Long timeLeftInSecond = timeoutManager.getTimeoutLeftInSeconds();
			if ( timeLeftInSecond != null ) {
				if ( timeLeftInSecond == 0 ) {
					timeoutManager.reactOnQueryTimeoutExceptionWhileExtracting( null );
				}
				criteria.setTimeout( timeLeftInSecond.intValue() );
			}
		}
	}

	/**
	 * Returns a list with one or more {@link Criteria} objects for loading the given entity infos. The returned list
	 * will contain one criteria object for each id space used by the given infos. A single criteria will be returned in
	 * case all the entity infos originate from the same id space.
	 */
	private List<Criteria> buildUpCriteria(List<EntityInfo> entityInfos, ObjectInitializationContext objectInitializationContext) {
		Map<Class<?>, EntityInfoIdSpace> infosByIdSpace = groupInfosByIdSpace( entityInfos, objectInitializationContext );

		// all entities from same id space -> single criteria
		if ( infosByIdSpace.size() == 1 ) {
			EntityInfoIdSpace idSpace = infosByIdSpace.values().iterator().next();

			// no explicitly user specified criteria query, define one
			Criteria criteria = objectInitializationContext.getCriteria();

			if ( criteria == null ) {
				criteria = createCriteria( idSpace, objectInitializationContext );
			}

			criteria.add( getIdListCriterion( idSpace.getEntityInfos(), objectInitializationContext ) );

			return Collections.singletonList( criteria );
		}
		// entities originate from different id spaces -> criteria per space
		else {
			// Cannot use external criteria for fetching entities from different spaces
			if ( objectInitializationContext.getCriteria() != null ) {
				log.givenCriteriaObjectCannotBeApplied();
			}

			List<Criteria> criterias = new ArrayList<>( infosByIdSpace.size() );

			for ( Entry<Class<?>, EntityInfoIdSpace> infosOfIdSpace : infosByIdSpace.entrySet() ) {
				EntityInfoIdSpace idSpace = infosOfIdSpace.getValue();
				Criteria criteria = createCriteria( idSpace, objectInitializationContext );
				criteria.add( getIdListCriterion( idSpace.getEntityInfos(), objectInitializationContext ) );
				criterias.add( criteria );
			}

			return criterias;
		}
	}

	private Criteria createCriteria(EntityInfoIdSpace idSpace, ObjectInitializationContext objectInitializationContext) {
		// Legacy Hibernate Criteria is constructed directly to avoid it logging a warning
		// which is meant to suggest that end users need to move away from the legacy Criteria usage..
		// We can't avoid the Criteria now w/o breacking backwards compatibility
		// (Specifically, without removing "org.hibernate.search.FullTextQuery.setCriteriaQuery(Criteria)" )
		return new CriteriaImpl( idSpace.getMostSpecificEntityType().getName(), (SharedSessionContractImplementor) objectInitializationContext.getSession() );
	}

	/**
	 * Returns a {@link Criterion} for fetching all the given entity infos. If needed, this criterion will contain a
	 * {@link Disjunction} for fetching the infos in chunks of {@link CriteriaObjectInitializer#MAX_IN_CLAUSE} elements.
	 */
	private Criterion getIdListCriterion(List<EntityInfo> entityInfos, ObjectInitializationContext objectInitializationContext) {
		DocumentBuilderIndexedEntity documentBuilder = getDocumentBuilder(
				entityInfos.iterator().next().getType(),
				objectInitializationContext.getExtendedSearchIntegrator()
		);
		String idName = documentBuilder.getIdPropertyName();
		Disjunction disjunction = Restrictions.disjunction();

		int maxResults = entityInfos.size();
		int loop = maxResults / MAX_IN_CLAUSE;
		boolean exact = maxResults % MAX_IN_CLAUSE == 0;
		if ( !exact ) {
			loop++;
		}
		for ( int index = 0; index < loop; index++ ) {
			int max = Math.min( index * MAX_IN_CLAUSE + MAX_IN_CLAUSE, maxResults );

			List<Serializable> ids = new ArrayList<>( max - index * MAX_IN_CLAUSE );
			for ( int entityInfoIndex = index * MAX_IN_CLAUSE; entityInfoIndex < max; entityInfoIndex++ ) {
				ids.add( entityInfos.get( entityInfoIndex ).getId() );
			}
			disjunction.add( Restrictions.in( idName, ids ) );
		}

		return disjunction;
	}

	/**
	 * Groups the given entity infos by id spaces. An id space is a set of entity types which share the same id
	 * property, e.g. defined in a common super-entity.
	 *
	 * @return The given entity infos, keyed by the root entity type of id spaces
	 */
	private Map<Class<?>, EntityInfoIdSpace> groupInfosByIdSpace(List<EntityInfo> entityInfos, ObjectInitializationContext objectInitializationContext) {
		ServiceManager serviceManager = objectInitializationContext.getExtendedSearchIntegrator().getServiceManager();
		IdUniquenessResolver resolver = serviceManager.requestService( IdUniquenessResolver.class );
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) objectInitializationContext.getSession().getSessionFactory();

		try {
			Map<Class<?>, EntityInfoIdSpace> idSpaces = new HashMap<>();

			for ( EntityInfo entityInfo : entityInfos ) {
				addToIdSpace( idSpaces, entityInfo, resolver, sessionFactory );
			}

			return idSpaces;
		}
		finally {
			serviceManager.releaseService( IdUniquenessResolver.class );
		}
	}

	private Class<?> getRootEntityType(SessionFactoryImplementor sessionFactory, IndexedTypeIdentifier indexedTypeIdentifier) {
		MetamodelImplementor metamodel = sessionFactory.getMetamodel();
		String entityName = indexedTypeIdentifier.getName();
		String rootEntityName = metamodel.entityPersister( entityName ).getRootEntityName();
		return metamodel.entityPersister( rootEntityName ).getMappedClass();
	}

	private void addToIdSpace(Map<Class<?>, EntityInfoIdSpace> idSpaces, EntityInfo entityInfo, IdUniquenessResolver resolver, SessionFactoryImplementor sessionFactory) {
		// add to existing id space if possible
		for ( Entry<Class<?>, EntityInfoIdSpace> idSpace : idSpaces.entrySet() ) {
			if ( resolver.areIdsUniqueForClasses( entityInfo.getType(), new PojoIndexedTypeIdentifier( idSpace.getKey() ) ) ) {
				idSpace.getValue().add( entityInfo );
				return;
			}
		}

		// otherwise create a new id space, using the root entity as key
		Class<?> rootType = getRootEntityType( sessionFactory, entityInfo.getType() );
		EntityInfoIdSpace idSpace = new EntityInfoIdSpace( rootType, entityInfo );
		idSpaces.put( getRootEntityType( sessionFactory, entityInfo.getType() ), idSpace );
	}

	private DocumentBuilderIndexedEntity getDocumentBuilder(IndexedTypeIdentifier entityType, ExtendedSearchIntegrator extendedIntegrator) {
		IndexedTypeSet indexedEntities = extendedIntegrator.getIndexedTypesPolymorphic( entityType.asTypeSet() );
		if ( indexedEntities.size() > 0 ) {
			return extendedIntegrator.getIndexBinding(
					indexedEntities.iterator().next()
			).getDocumentBuilder();
		}
		else {
			return null;
		}
	}

	/**
	 * Container used to store all the {@code EntityInfo}s for entities which share the same id space (typically all the
	 * subtypes of the same root type).
	 *
	 * Determines the most specific entity type we can use to build a Criteria to get the entities associated with these
	 * {@code EntityInfo}s.
	 */
	private static class EntityInfoIdSpace {
		private final Class<?> rootType;

		private Class<?> mostSpecificEntityType;

		private List<EntityInfo> entityInfos = new ArrayList<>();

		private EntityInfoIdSpace(Class<?> rootType, EntityInfo entityInfo) {
			this.rootType = rootType;
			this.entityInfos.add( entityInfo );
			this.mostSpecificEntityType = entityInfo.getType().getPojoType();
		}

		private void add(EntityInfo entityInfo) {
			entityInfos.add( entityInfo );
			mostSpecificEntityType = getMostSpecificCommonSuperClass( mostSpecificEntityType, entityInfo.getType().getPojoType() );
		}

		private Class<?> getMostSpecificCommonSuperClass(Class<?> class1, Class<?> class2) {
			if ( rootType.equals( class1 ) || rootType.equals( class2 ) ) {
				return rootType;
			}
			Class<?> superClass = class1;
			while ( !superClass.isAssignableFrom( class2 ) ) {
				superClass = superClass.getSuperclass();
			}
			return superClass;
		}

		private List<EntityInfo> getEntityInfos() {
			return entityInfos;
		}

		/**
		 * Returns the most specific possible type to build the criteria with. In case of a hierarchy using a join inheritance,
		 * it makes a huge difference if we are targeting only one subtype as we will avoid the joins on all the subtypes
		 * of the root entity type.
		 *
		 * @return the most specific entity type we can use for the Criteria
		 */
		private Class<?> getMostSpecificEntityType() {
			return mostSpecificEntityType;
		}

	}

}
