/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.manualsource.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.manualsource.source.EntityKeyForLoad;
import org.hibernate.search.manualsource.source.ObjectInitializer;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.TimeoutManager;

/**
 * A loader which loads objects of multiple types.
 *
 * @author Emmanuel Bernard
 */
public class MultiClassesQueryLoader extends AbstractLoader {
	private static Object ENTITY_NOT_YET_INITIALIZED = new Object();

	private ExtendedSearchIntegrator extendedIntegrator;
	private List<RootEntityMetadata> entityMetadata;
	private TimeoutManager timeoutManager;
	private ObjectInitializer objectInitializer;
	private ObjectInitializer.Context objectInitializerContext;

	@Override
	public void init(WorkLoadImpl workLoad,
					ExtendedSearchIntegrator extendedSearchIntegrator,
					ObjectInitializer objectInitializer,
					TimeoutManager timeoutManager) {
		super.init( workLoad, extendedSearchIntegrator );
		this.extendedIntegrator = extendedSearchIntegrator;
		this.timeoutManager = timeoutManager;
		this.objectInitializer = objectInitializer;
		this.objectInitializerContext = new ObjectInitializerContext( workLoad.getEntitySourceContext(), timeoutManager );
	}

	@Override
	public boolean isSizeSafe() {
		return true; //no user provided criteria
	}

	public void setEntityTypes(Set<Class<?>> entityTypes) {
		List<Class<?>> safeEntityTypes = new ArrayList<>();
		// TODO should we go find the root entity for a given class rather than just checking for it's root status?
		// root entity could lead to quite inefficient queries in Hibernate when using table per class
		if ( entityTypes.size() == 0 ) {
			//support all classes
			for ( Map.Entry<Class<?>, EntityIndexBinding> entry : extendedIntegrator.getIndexBindings().entrySet() ) {
				//get only root entities to limit queries
				//TODO today inheritance not optimized, think about it later: all entities are considered root
				//TODO can some / all third-party source make use of that optimization?
				//TODO should we use the new contract to detect who is the superclass of who?
				if ( true || entry.getValue().getDocumentBuilder().isRoot() ) {
					safeEntityTypes.add( entry.getKey() );
				}
			}
		}
		else {
			safeEntityTypes.addAll( entityTypes );
		}
		entityMetadata = new ArrayList<>( safeEntityTypes.size() );
		for ( Class clazz : safeEntityTypes ) {
			entityMetadata.add( new RootEntityMetadata( clazz, extendedIntegrator ) );
		}
	}

	@Override
	public Object executeLoad(EntityInfo entityInfo) {
		// linkedhasmap as we want to return the results in the same order as the EntityInfo processing
		// here it's not important as we have a single entry
		LinkedHashMap<EntityKeyForLoad, Object> idsToObjects = new LinkedHashMap<>( 1 );
		EntityKeyForLoad entityKeyForLoad = new EntityKeyForLoad( entityInfo.getClazz(), entityInfo.getId() );
		objectInitializer.initializeObjects(
				Collections.singletonList( entityKeyForLoad ),
				idsToObjects,
				objectInitializerContext
		);
		// TODO what if that's null ?
		final Object result = idsToObjects.get( entityKeyForLoad );
		timeoutManager.isTimedOut();
		return result;
	}

	@Override
	public List executeLoad(EntityInfo... entityInfos) {
		if ( entityInfos.length == 0 ) {
			return Collections.EMPTY_LIST;
		}

		if ( entityInfos.length == 1 ) {
			final Object entity = load( entityInfos[0] );
			if ( entity == null ) {
				return Collections.EMPTY_LIST;
			}
			else {
				return Collections.singletonList( entity );
			}
		}

		// linkedhasmap as we want to return the results in the same order as the EntityInfo processing
		LinkedHashMap<EntityKeyForLoad, Object> idToObjectMap = new LinkedHashMap<>( (int) ( entityInfos.length / 0.75 ) + 1 );

		// split EntityInfo per root entity
		Map<RootEntityMetadata, List<EntityKeyForLoad>> entityInfoBuckets = new HashMap<>( entityMetadata.size() );
		for ( EntityInfo entityInfo : entityInfos ) {
			boolean found = false;
			final Class<?> clazz = entityInfo.getClazz();
			for ( RootEntityMetadata rootEntityInfo : entityMetadata ) {
				//TODO today inheritance not optimized, think about it later: ignore subclasses for now
				//TODO can some / all third-party source make use of that optimization?
				//TODO should we use the new contract to detect who is the superclass of who?
				if ( rootEntityInfo.rootEntity == clazz ) {
						// || rootEntityInfo.mappedSubclasses.contains( clazz ) ) {
					List<EntityKeyForLoad> bucket = entityInfoBuckets.get( rootEntityInfo );
					if ( bucket == null ) {
						bucket = new ArrayList<>();
						entityInfoBuckets.put( rootEntityInfo, bucket );
					}
					EntityKeyForLoad key = new EntityKeyForLoad( entityInfo.getClazz(), entityInfo.getId() );
					bucket.add( key );
					found = true;
					idToObjectMap.put(
							key,
							ENTITY_NOT_YET_INITIALIZED
					);
					break; //we stop looping for the right bucket
				}
			}
			if ( !found ) {
				throw new AssertionFailure( "Could not find root entity for " + clazz );
			}
		}

		// initialize objects by bucket
		for ( Map.Entry<RootEntityMetadata, List<EntityKeyForLoad>> entry : entityInfoBuckets.entrySet() ) {
			objectInitializer.initializeObjects(
					entry.getValue(), idToObjectMap, objectInitializerContext
			);
			timeoutManager.isTimedOut();
		}

		ArrayList<Object> result = new ArrayList<>( idToObjectMap.size() );
		for ( Object o : idToObjectMap.values() ) {
			// is the value is null, we had a hit in the Lucene index, but the underlying entity had already been
			// removed w/i ORM (HF)
			if ( o != ENTITY_NOT_YET_INITIALIZED ) {
				result.add( o );
			}
		}
		return result;
	}

	private static class RootEntityMetadata {
		public final Class<?> rootEntity;
		public final Set<Class<?>> mappedSubclasses;

		RootEntityMetadata(Class<?> rootEntity, ExtendedSearchIntegrator extendedIntegrator) {
			this.rootEntity = rootEntity;
			EntityIndexBinding provider = extendedIntegrator.getIndexBinding( rootEntity );
			if ( provider == null ) {
				throw new AssertionFailure("Provider not found for class: " + rootEntity);
			}
			this.mappedSubclasses = provider.getDocumentBuilder().getMappedSubclasses();
		}
	}
}
