/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.hibernate.impl;

import java.util.Collections;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.TimeoutManager;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class QueryLoader extends AbstractLoader {

	private Session session;
	private Class entityType;
	private SearchFactoryImplementor searchFactoryImplementor;
	private Criteria criteria;
	private boolean isExplicitCriteria;
	private TimeoutManager timeoutManager;
	private ObjectsInitializer objectsInitializer;
	private boolean sizeSafe = true;

	@Override
	public void init(Session session,
					SearchFactoryImplementor searchFactoryImplementor,
					ObjectsInitializer objectsInitializer,
					TimeoutManager timeoutManager) {
		super.init( session, searchFactoryImplementor );
		this.session = session;
		this.searchFactoryImplementor = searchFactoryImplementor;
		this.timeoutManager = timeoutManager;
		this.objectsInitializer = objectsInitializer;
	}

	@Override
	public boolean isSizeSafe() {
		return sizeSafe;
	}

	public void setEntityType(Class entityType) {
		this.entityType = entityType;
	}

	@Override
	public final Object executeLoad(EntityInfo entityInfo) {
		//if explicit criteria, make sure to use it to load the objects
		if ( isExplicitCriteria ) {
			load( new EntityInfo[] { entityInfo } );
		}
		final Object result = ObjectLoaderHelper.load( entityInfo, session );
		timeoutManager.isTimedOut();
		return result;
	}

	@Override
	public final List executeLoad(EntityInfo... entityInfos) {
		if ( entityInfos.length == 0 ) {
			return Collections.EMPTY_LIST;
		}
		if ( entityType == null ) {
			throw new AssertionFailure( "EntityType not defined" );
		}

		objectsInitializer.initializeObjects(
				entityInfos,
				criteria,
				entityType,
				searchFactoryImplementor,
				timeoutManager,
				session);
		return ObjectLoaderHelper.returnAlreadyLoadedObjectsInCorrectOrder( entityInfos, session );
	}

	public void setCriteria(Criteria criteria) {
		if ( criteria != null ) {
			isExplicitCriteria = true;
			sizeSafe = true;
			if ( criteria instanceof CriteriaImpl ) {
				CriteriaImpl impl = (CriteriaImpl) criteria;
				//restriction of subcriteria => suspect
				//TODO some subcriteria might be ok (outer joins)
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
