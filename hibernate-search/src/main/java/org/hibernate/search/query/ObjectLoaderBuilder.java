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

package org.hibernate.search.query;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.annotations.common.util.ReflectHelper;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.impl.CriteriaImpl;
import org.hibernate.search.SearchException;
import org.hibernate.search.engine.Loader;
import org.hibernate.search.engine.MultiClassesQueryLoader;
import org.hibernate.search.engine.QueryLoader;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.query.impl.CriteriaObjectsInitializer;
import org.hibernate.search.query.impl.LookupObjectsInitializer;
import org.hibernate.search.query.impl.ObjectsInitializer;
import org.hibernate.search.query.impl.PersistenceContextObjectsInitializer;
import org.hibernate.search.query.impl.SecondLevelCacheObjectsInitializer;
import org.hibernate.search.util.LoggerFactory;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;

/**
 * @author Emmanuel Bernard
 */
public class ObjectLoaderBuilder {
	private Criteria criteria;
	private List<Class<?>> targetedEntities;
	private SessionImplementor session;
	private SearchFactoryImplementor searchFactoryImplementor;
	private Set<Class<?>> indexedTargetedEntities;
	private TimeoutManager timeoutManager;
	private ObjectLookupMethod lookupMethod;
	private DatabaseRetrievalMethod retrievalMethod;
	private static final Logger log = LoggerFactory.make();

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
		multiClassesLoader.init( (Session) session, searchFactoryImplementor, getObjectInitializer(), timeoutManager );
		multiClassesLoader.setEntityTypes( indexedTargetedEntities );
		return multiClassesLoader;
	}

	private Loader getSingleEntityLoader() {
		final QueryLoader queryLoader = new QueryLoader();
		queryLoader.init( ( Session ) session, searchFactoryImplementor, getObjectInitializer(), timeoutManager );
		queryLoader.setEntityType( targetedEntities.iterator().next() );
		return queryLoader;
	}

	private Loader getCriteriaLoader() {
		if ( targetedEntities.size() > 1 ) {
			throw new SearchException( "Cannot mix criteria and multiple entity types" );
		}
		Class entityType = targetedEntities.size() == 0 ? null : targetedEntities.iterator().next();
		if ( criteria instanceof CriteriaImpl) {
			String targetEntity = ( ( CriteriaImpl ) criteria ).getEntityOrClassName();
			if ( entityType != null && !entityType.getName().equals( targetEntity ) ) {
				throw new SearchException( "Criteria query entity should match query entity" );
			}
			else {
				try {
					entityType = ReflectHelper.classForName( targetEntity );
				}
				catch ( ClassNotFoundException e ) {
					throw new SearchException( "Unable to load entity class from criteria: " + targetEntity, e );
				}
			}
		}
		QueryLoader queryLoader = new QueryLoader();
		queryLoader.init( ( Session ) session, searchFactoryImplementor, getObjectInitializer(), timeoutManager );
		queryLoader.setEntityType( entityType );
		queryLoader.setCriteria( criteria );
		return queryLoader;
	}

	public ObjectLoaderBuilder session(SessionImplementor session) {
		this.session = session;
		return this;
	}

	public ObjectLoaderBuilder searchFactory(SearchFactoryImplementor searchFactoryImplementor) {
		this.searchFactoryImplementor = searchFactoryImplementor;
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

	private ObjectsInitializer getObjectInitializer() {
		log.trace( "ObjectsInitializer: Use lookup method {} and database retrieval method {}", lookupMethod, retrievalMethod );
		if (criteria != null && retrievalMethod != DatabaseRetrievalMethod.QUERY) {
			throw new SearchException( "Cannot mix custom criteria query and " + DatabaseRetrievalMethod.class.getSimpleName() + "." + retrievalMethod );
		}
		//TODO should we cache these ObjectInitializer instances?
		ObjectsInitializer initializer;
		if ( retrievalMethod == DatabaseRetrievalMethod.FIND_BY_ID) {
			//return early as this method does naturally 2lc + session lookup
			return new LookupObjectsInitializer();
		}
		else if ( retrievalMethod == DatabaseRetrievalMethod.QUERY) {
			initializer = new CriteriaObjectsInitializer();
		}
		else {
			throw new AssertionFailure( "Unknown " + DatabaseRetrievalMethod.class.getSimpleName() + "." + retrievalMethod );
		}
		if (lookupMethod == ObjectLookupMethod.SKIP) {
			return initializer;
		}
		else if (lookupMethod == ObjectLookupMethod.SECOND_LEVEL_CACHE) {
			return new SecondLevelCacheObjectsInitializer( initializer );
		}
		else if (lookupMethod == ObjectLookupMethod.PERSISTENCE_CONTEXT) {
			return new PersistenceContextObjectsInitializer( initializer );
		}
		else if (lookupMethod == ObjectLookupMethod.PERSISTENCE_CONTEXT_AND_SECOND_LEVEL_CACHE ) {
			//we want to check the PC first, that's cheaper
			return new PersistenceContextObjectsInitializer( new SecondLevelCacheObjectsInitializer(initializer) );
		}
		else {
			throw new AssertionFailure( "Unknown " + ObjectLookupMethod.class.getSimpleName() + "." + lookupMethod );
		}
		//unreachable statement
	}
}
