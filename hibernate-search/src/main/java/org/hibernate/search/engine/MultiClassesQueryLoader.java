/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 *  Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 *  indicated by the @author tags or express copyright attribution
 *  statements applied by the authors.  All third-party contributions are
 *  distributed under license by Red Hat, Inc.
 *
 *  This copyrighted material is made available to anyone wishing to use, modify,
 *  copy, or redistribute it subject to the terms and conditions of the GNU
 *  Lesser General Public License, as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this distribution; if not, write to:
 *  Free Software Foundation, Inc.
 *  51 Franklin Street, Fifth Floor
 *  Boston, MA  02110-1301  USA
 */
package org.hibernate.search.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.common.AssertionFailure;

/**
 * A loader which loads objects of multiple types.
 *
 * @author Emmanuel Bernard
 */
public class MultiClassesQueryLoader extends AbstractLoader {
	private Session session;
	private SearchFactoryImplementor searchFactoryImplementor;
	private List<RootEntityMetadata> entityMatadata;

	public void init(Session session, SearchFactoryImplementor searchFactoryImplementor) {
		super.init( session, searchFactoryImplementor );
		this.session = session;
		this.searchFactoryImplementor = searchFactoryImplementor;
	}

	public void setEntityTypes(Set<Class<?>> entityTypes) {
		List<Class<?>> safeEntityTypes = new ArrayList<Class<?>>();
		//TODO should we go find the root entity for a given class rather than just checking for it's root status?
		//     root entity could lead to quite inefficient queries in Hibernate when using table per class
		if ( entityTypes.size() == 0 ) {
			//support all classes
			for( Map.Entry<Class<?>, DocumentBuilderIndexedEntity<?>> entry : searchFactoryImplementor.getDocumentBuildersIndexedEntities().entrySet() ) {
				//get only root entities to limit queries
				if ( entry.getValue().isRoot() ) {
					safeEntityTypes.add( entry.getKey() );
				}
			}
		}
		else {
			safeEntityTypes.addAll(entityTypes);
		}
		entityMatadata = new ArrayList<RootEntityMetadata>( safeEntityTypes.size() );
		for (Class clazz :  safeEntityTypes) {
			entityMatadata.add( new RootEntityMetadata( clazz, searchFactoryImplementor, session ) );
		}
	}

	public Object executeLoad(EntityInfo entityInfo) {
		return ObjectLoaderHelper.load( entityInfo, session );
	}

	public List executeLoad(EntityInfo... entityInfos) {
		if ( entityInfos.length == 0 ) return Collections.EMPTY_LIST;
		if ( entityInfos.length == 1 ) {
			final Object entity = load( entityInfos[0] );
			if ( entity == null ) {
				return Collections.EMPTY_LIST;
			}
			else {
				final List<Object> list = new ArrayList<Object>( 1 );
				list.add( entity );
				return list;
			}
		}

		//split EntityInfo per root entity
		Map<RootEntityMetadata, List<EntityInfo>> entityinfoBuckets =
				new HashMap<RootEntityMetadata, List<EntityInfo>>( entityMatadata.size());
		for (EntityInfo entityInfo : entityInfos) {
			boolean found = false;
			for (RootEntityMetadata rootEntityInfo : entityMatadata) {
				if ( rootEntityInfo.rootEntity == entityInfo.clazz || rootEntityInfo.mappedSubclasses.contains( entityInfo.clazz ) ) {
					List<EntityInfo> bucket = entityinfoBuckets.get( rootEntityInfo );
					if ( bucket == null ) {
						bucket = new ArrayList<EntityInfo>();
						entityinfoBuckets.put( rootEntityInfo, bucket );
					}
					bucket.add( entityInfo );
					found = true;
					break; //we stop looping for the right bucket
				}
			}
			if (!found) throw new AssertionFailure( "Could not find root entity for " + entityInfo.clazz );
		}

		//initialize objects by bucket
		for ( Map.Entry<RootEntityMetadata, List<EntityInfo>> entry : entityinfoBuckets.entrySet() ) {
			final RootEntityMetadata key = entry.getKey();
			final List<EntityInfo> value = entry.getValue();
			final EntityInfo[] bucketEntityInfos = value.toArray( new EntityInfo[value.size()] );

			ObjectLoaderHelper.initializeObjects( bucketEntityInfos,
					key.criteria, key.rootEntity, searchFactoryImplementor);
		}
		return ObjectLoaderHelper.returnAlreadyLoadedObjectsInCorrectOrder( entityInfos, session );
	}

	private static class RootEntityMetadata {
		public final Class<?> rootEntity;
		public final Set<Class<?>> mappedSubclasses;
		private final Criteria criteria;

		RootEntityMetadata(Class<?> rootEntity, SearchFactoryImplementor searchFactoryImplementor, Session session) {
			this.rootEntity = rootEntity;
			DocumentBuilderIndexedEntity<?> provider = searchFactoryImplementor.getDocumentBuilderIndexedEntity( rootEntity );
			if ( provider == null) throw new AssertionFailure("Provider not found for class: " + rootEntity);
			this.mappedSubclasses = provider.getMappedSubclasses();
			this.criteria = session.createCriteria( rootEntity );
		}
	}
}
