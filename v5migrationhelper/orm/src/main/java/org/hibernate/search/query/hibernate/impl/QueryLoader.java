/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.hibernate.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.TimeoutManager;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class QueryLoader extends AbstractLoader {

	private SessionImplementor session;
	private Class entityType;
	private ExtendedSearchIntegrator extendedIntegrator;
	private Criteria criteria;
	private boolean isExplicitCriteria;
	private TimeoutManager timeoutManager;
	private ObjectInitializer objectInitializer;
	private boolean sizeSafe = true;

	@Override
	public void init(SessionImplementor session,
			ExtendedSearchIntegrator extendedIntegrator,
					ObjectInitializer objectInitializer,
					TimeoutManager timeoutManager) {
		super.init( session, extendedIntegrator );
		this.session = session;
		this.extendedIntegrator = extendedIntegrator;
		this.timeoutManager = timeoutManager;
		this.objectInitializer = objectInitializer;
	}

	@Override
	public boolean isSizeSafe() {
		return sizeSafe;
	}

	public void setEntityType(Class entityType) {
		this.entityType = entityType;
	}

	@Override
	protected final Object executeLoad(EntityInfo entityInfo) {
		//if explicit criteria, make sure to use it to load the objects
		if ( isExplicitCriteria ) {
			executeLoad( Collections.singletonList( entityInfo ) );
		}
		final Object result = ObjectLoaderHelper.load( entityInfo, session );
		timeoutManager.isTimedOut();
		return result;
	}

	@Override
	protected final List executeLoad(List<EntityInfo> entityInfos) {
		if ( entityType == null ) {
			throw new AssertionFailure( "EntityType not defined" );
		}

		LinkedHashMap<EntityInfoLoadKey, Object> idToObjectMap = new LinkedHashMap<>( (int) ( entityInfos.size() / 0.75 ) + 1 );
		for ( EntityInfo entityInfo : entityInfos ) {
			idToObjectMap.put(
					new EntityInfoLoadKey( entityInfo.getType().getPojoType(), entityInfo.getId() ),
					ObjectInitializer.ENTITY_NOT_YET_INITIALIZED
			);
		}

		objectInitializer.initializeObjects(
				entityInfos,
				idToObjectMap,
				new ObjectInitializationContext( criteria, entityType, extendedIntegrator, timeoutManager, session )
		);

		ArrayList<Object> result = new ArrayList<>( idToObjectMap.size() );
		for ( Object o : idToObjectMap.values() ) {
			if ( o != ObjectInitializer.ENTITY_NOT_YET_INITIALIZED ) {
				result.add( o );
			}
		}
		return result;
	}

	public void setCriteria(Criteria criteria) {
		if ( criteria != null ) {
			isExplicitCriteria = true;
			sizeSafe = true;
			if ( criteria instanceof CriteriaImpl ) {
				CriteriaImpl impl = (CriteriaImpl) criteria;
				//restriction of sub criteria => suspect
				//TODO some sub criteria might be ok (outer joins)
				sizeSafe = !impl.iterateExpressionEntries().hasNext() && !impl.iterateSubcriteria().hasNext();
			}
		}
		else {
			sizeSafe = true;
			isExplicitCriteria = false;
		}
		this.criteria = criteria;
	}
}
