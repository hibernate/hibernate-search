/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.hibernate.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
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
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
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

		// no explicitly user specified criteria query, define one
		Criteria criteria = objectInitializationContext.getCriteria();
		if ( criteria == null ) {
			criteria = objectInitializationContext.getSession()
					.createCriteria( objectInitializationContext.getEntityType() );
		}
		buildUpCriteria( entityInfos, criteria, maxResults, objectInitializationContext );
		setCriteriaTimeout( criteria, objectInitializationContext.getTimeoutManager() );

		@SuppressWarnings("unchecked")
		List<Object> queryResultList = criteria.list();
		InstanceInitializer instanceInitializer = objectInitializationContext.getExtendedSearchintegrator()
				.getInstanceInitializer();
		for ( Object o : queryResultList ) {
			Class<?> loadedType = instanceInitializer.getClass( o );
			Object unproxiedObject = instanceInitializer.unproxy( o );
			DocumentBuilderIndexedEntity documentBuilder = getDocumentBuilder(
					loadedType,
					objectInitializationContext.getExtendedSearchintegrator()
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

	private void buildUpCriteria(EntityInfo[] entityInfos,
			Criteria criteria,
			int maxResults,
			ObjectInitializationContext objectInitializationContext) {
		DocumentBuilderIndexedEntity documentBuilder = getDocumentBuilder(
				objectInitializationContext.getEntityType(),
				objectInitializationContext.getExtendedSearchintegrator()
		);
		String idName = documentBuilder.getIdentifierName();
		Disjunction disjunction = Restrictions.disjunction();

		int loop = maxResults / MAX_IN_CLAUSE;
		boolean exact = maxResults % MAX_IN_CLAUSE == 0;
		if ( !exact ) {
			loop++;
		}
		for ( int index = 0; index < loop; index++ ) {
			int max = index * MAX_IN_CLAUSE + MAX_IN_CLAUSE <= maxResults ?
					index * MAX_IN_CLAUSE + MAX_IN_CLAUSE :
					maxResults;
			List<Serializable> ids = new ArrayList<>( max - index * MAX_IN_CLAUSE );
			for ( int entityInfoIndex = index * MAX_IN_CLAUSE; entityInfoIndex < max; entityInfoIndex++ ) {
				ids.add( entityInfos[entityInfoIndex].getId() );
			}
			disjunction.add( Restrictions.in( idName, ids ) );
		}
		criteria.add( disjunction );
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
