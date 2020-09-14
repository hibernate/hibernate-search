/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.hibernate.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.TimeoutManager;
import org.hibernate.search.spi.IndexedTypeIdentifier;

/**
 * A loader which loads objects of multiple types.
 *
 * @author Emmanuel Bernard
 */
public class MultiClassesQueryLoader extends AbstractLoader {
	private SessionImplementor session;
	private ExtendedSearchIntegrator extendedIntegrator;
	private List<RootEntityMetadata> entityMetadata;
	private TimeoutManager timeoutManager;
	private ObjectInitializer objectInitializer;

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
		return true; //no user provided criteria
	}

	public void setEntityTypes(Set<Class<?>> entityTypes) {
		List<Class<?>> safeEntityTypes = new ArrayList<>();
		// TODO should we go find the root entity for a given class rather than just checking for it's root status?
		// root entity could lead to quite inefficient queries in Hibernate when using table per class
		if ( entityTypes.size() == 0 ) {
			//support all classes
			for ( Entry<IndexedTypeIdentifier, EntityIndexBinding> entry : extendedIntegrator.getIndexBindings().entrySet() ) {
				//get only root entities to limit queries
				if ( entry.getValue().getDocumentBuilder().isRoot() ) {
					safeEntityTypes.add( entry.getKey().getPojoType() );
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
	protected Object executeLoad(EntityInfo entityInfo) {
		final Object result = ObjectLoaderHelper.load( entityInfo, session );
		timeoutManager.isTimedOut();
		return result;
	}

	@Override
	protected List executeLoad(List<EntityInfo> entityInfos) {
		LinkedHashMap<EntityInfoLoadKey, Object> idToObjectMap = new LinkedHashMap<>( (int) ( entityInfos.size() / 0.75 ) + 1 );

		// split EntityInfo per root entity
		Map<RootEntityMetadata, List<EntityInfo>> entityInfoBuckets = new HashMap<>( entityMetadata.size() );
		for ( EntityInfo entityInfo : entityInfos ) {
			boolean found = false;
			final Class<?> clazz = entityInfo.getType().getPojoType();
			for ( RootEntityMetadata rootEntityInfo : entityMetadata ) {
				if ( rootEntityInfo.rootEntity == clazz || rootEntityInfo.mappedSubclasses.contains( clazz ) ) {
					List<EntityInfo> bucket = entityInfoBuckets.get( rootEntityInfo );
					if ( bucket == null ) {
						bucket = new ArrayList<>();
						entityInfoBuckets.put( rootEntityInfo, bucket );
					}
					bucket.add( entityInfo );
					found = true;
					idToObjectMap.put(
							new EntityInfoLoadKey( entityInfo.getType().getPojoType(), entityInfo.getId() ),
							ObjectInitializer.ENTITY_NOT_YET_INITIALIZED
					);
					break; //we stop looping for the right bucket
				}
			}
			if ( !found ) {
				throw new AssertionFailure( "Could not find root entity for " + clazz );
			}
		}

		// initialize objects by bucket
		for ( Map.Entry<RootEntityMetadata, List<EntityInfo>> entry : entityInfoBuckets.entrySet() ) {
			final RootEntityMetadata key = entry.getKey();
			final List<EntityInfo> value = entry.getValue();

			objectInitializer.initializeObjects(
					value, idToObjectMap, new ObjectInitializationContext(
							null,
							key.rootEntity,
							extendedIntegrator,
							timeoutManager,
							session
					)
			);
			timeoutManager.isTimedOut();
		}

		ArrayList<Object> result = new ArrayList<>( idToObjectMap.size() );
		for ( Object o : idToObjectMap.values() ) {
			// is the value is null, we had a hit in the Lucene index, but the underlying entity had already been
			// removed w/i ORM (HF)
			if ( o != ObjectInitializer.ENTITY_NOT_YET_INITIALIZED ) {
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
			EntityIndexBinding provider = extendedIntegrator.getIndexBindings().get( rootEntity );
			if ( provider == null ) {
				throw new AssertionFailure( "Provider not found for class: " + rootEntity );
			}
			this.mappedSubclasses = provider.getDocumentBuilder().getMappedSubclasses();
		}
	}
}
