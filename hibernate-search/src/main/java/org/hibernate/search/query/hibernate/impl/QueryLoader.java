/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.query.hibernate.impl;

import java.util.Collections;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.common.AssertionFailure;
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

	public final Object executeLoad(EntityInfo entityInfo) {
		//if explicit criteria, make sure to use it to load the objects
		if ( isExplicitCriteria ) {
			load( new EntityInfo[] { entityInfo } );
		}
		final Object result = ObjectLoaderHelper.load( entityInfo, session );
		timeoutManager.isTimedOut();
		return result;
	}

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
		if (criteria != null) {
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
