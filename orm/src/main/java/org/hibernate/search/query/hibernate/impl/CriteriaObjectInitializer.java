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
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.cfg.spi.IdUniquenessResolver;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.TimeoutManager;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.util.impl.ReflectionHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Initialize object using one or several criteria queries.
 *
 * @author Emmanuel Bernard
 * @author Gunnar Morling
 */
public class CriteriaObjectInitializer implements ObjectInitializer {

	private static final Log log = LoggerFactory.make();
	private static final int MAX_IN_CLAUSE = 500;

	public static final CriteriaObjectInitializer INSTANCE = new CriteriaObjectInitializer();

	private CriteriaObjectInitializer() {
		// use INSTANCE instead of constructor
	}

	@Override
	public void initializeObjects(EntityInfo[] entityInfos,
			LinkedHashMap<EntityInfoLoadKey, Object> idToObjectMap,
			ObjectInitializationContext objectInitializationContext) {
		// Do not call isTimeOut here as the caller might be the last biggie on the list.
		final int maxResults = entityInfos.length;
		if ( log.isTraceEnabled() ) {
			log.tracef( "Load %d objects using criteria queries", maxResults );
		}

		if ( maxResults == 0 ) {
			return;
		}

		List<Criteria> criterias = buildUpCriteria( entityInfos, objectInitializationContext );

		for ( Criteria criteria : criterias ) {
			setCriteriaTimeout( criteria, objectInitializationContext.getTimeoutManager() );

			@SuppressWarnings("unchecked")
			List<Object> queryResultList = criteria.list();
			InstanceInitializer instanceInitializer = objectInitializationContext.getExtendedSearchIntegrator()
					.getInstanceInitializer();
			for ( Object o : queryResultList ) {
				Class<?> loadedType = instanceInitializer.getClass( o );
				Object unproxiedObject = instanceInitializer.unproxy( o );
				DocumentBuilderIndexedEntity documentBuilder = getDocumentBuilder(
						loadedType,
						objectInitializationContext.getExtendedSearchIntegrator()
						);
				if ( documentBuilder == null ) {
					// the query result can contain entities which are not indexed. This can for example happen if
					// the targeted entity type is a superclass with indexed and un-indexed sub classes
					// entities which don't have an document builder can be ignores (HF)
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
	private List<Criteria> buildUpCriteria(EntityInfo[] entityInfos, ObjectInitializationContext objectInitializationContext) {
		Map<Class<?>, List<EntityInfo>> infosByIdSpace = groupInfosByIdSpace( entityInfos, objectInitializationContext );

		// all entities from same id space -> single criteria
		if ( infosByIdSpace.size() == 1 ) {
			// no explicitly user specified criteria query, define one
			Criteria criteria = objectInitializationContext.getCriteria();

			if ( criteria == null ) {
				criteria = objectInitializationContext.getSession().createCriteria( infosByIdSpace.keySet().iterator().next() );
			}

			criteria.add( getIdListCriterion( infosByIdSpace.values().iterator().next(), objectInitializationContext ) );

			return Collections.singletonList( criteria );
		}
		// entities originate from different id spaces -> criteria per space
		else {
			// Cannot use external criteria for fetching entities from different spaces
			if ( objectInitializationContext.getCriteria() != null ) {
				log.givenCriteriaObjectCannotBeApplied();
			}

			List<Criteria> criterias = new ArrayList<>( infosByIdSpace.size() );

			for ( Entry<Class<?>, List<EntityInfo>> infosOfIdSpace : infosByIdSpace.entrySet() ) {
				Criteria criteria = objectInitializationContext.getSession().createCriteria( infosOfIdSpace.getKey() );
				criteria.add( getIdListCriterion( infosOfIdSpace.getValue(), objectInitializationContext ) );
				criterias.add( criteria );
			}

			return criterias;
		}
	}

	/**
	 * Returns a {@link Criterion} for fetching all the given entity infos. If needed, this criterion will contain a
	 * {@link Disjunction} for fetching the infos in chunks of {@link CriteriaObjectInitializer#MAX_IN_CLAUSE} elements.
	 */
	private Criterion getIdListCriterion(List<EntityInfo> entityInfos, ObjectInitializationContext objectInitializationContext) {
		DocumentBuilderIndexedEntity documentBuilder = getDocumentBuilder(
				entityInfos.iterator().next().getClazz(),
				objectInitializationContext.getExtendedSearchIntegrator()
		);
		String idName = documentBuilder.getIdentifierName();
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
	private Map<Class<?>, List<EntityInfo>> groupInfosByIdSpace(EntityInfo[] entityInfos, ObjectInitializationContext objectInitializationContext) {
		ServiceManager serviceManager = objectInitializationContext.getExtendedSearchIntegrator().getServiceManager();
		IdUniquenessResolver resolver = serviceManager.requestService( IdUniquenessResolver.class );
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) objectInitializationContext.getSession().getSessionFactory();

		try {
			Map<Class<?>, List<EntityInfo>> idSpaces = new HashMap<>();

			for ( EntityInfo entityInfo : entityInfos ) {
				addToIdSpace( idSpaces, entityInfo, resolver, sessionFactory );
			}

			return idSpaces;
		}
		finally {
			serviceManager.releaseService( IdUniquenessResolver.class );
		}
	}

	private Class<?> getRootEntityType(SessionFactoryImplementor sessionFactory, Class<?> entityType) {
		String entityName = sessionFactory.getClassMetadata( entityType ).getEntityName();
		String rootEntityName = sessionFactory.getEntityPersister( entityName ).getRootEntityName();

		return sessionFactory.getEntityPersister( rootEntityName ).getMappedClass();
	}

	private void addToIdSpace(Map<Class<?>, List<EntityInfo>> idSpaces, EntityInfo entityInfo, IdUniquenessResolver resolver, SessionFactoryImplementor sessionFactory) {
		// add to existing id space if possible
		for ( Entry<Class<?>, List<EntityInfo>> idSpace : idSpaces.entrySet() ) {
			if ( resolver.areIdsUniqueForClasses( entityInfo.getClazz(), idSpace.getKey() ) ) {
				idSpace.getValue().add( entityInfo );
				return;
			}
		}

		// otherwise create a new id space, using the root entity as key
		List<EntityInfo> idSpace = new ArrayList<>();
		idSpace.add( entityInfo );
		idSpaces.put( getRootEntityType( sessionFactory, entityInfo.getClazz() ), idSpace );
	}

	private DocumentBuilderIndexedEntity getDocumentBuilder(Class<?> entityType, ExtendedSearchIntegrator extendedIntegrator) {
		Set<Class<?>> indexedEntities = extendedIntegrator.getIndexedTypesPolymorphic( new Class<?>[] { entityType } );
		if ( indexedEntities.size() > 0 ) {
			return extendedIntegrator.getIndexBinding(
					indexedEntities.iterator().next()
			).getDocumentBuilder();
		}
		else {
			return null;
		}
	}
}
