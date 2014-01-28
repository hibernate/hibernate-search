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
package org.hibernate.search.engine.spi;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.engine.BoostStrategy;
import org.hibernate.search.engine.impl.DefaultBoostStrategy;
import org.hibernate.search.engine.impl.WorkPlan;
import org.hibernate.search.engine.metadata.impl.ContainedInMetadata;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;
import org.hibernate.search.engine.metadata.impl.PropertyMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.util.impl.ReflectionHelper;
import org.hibernate.search.util.impl.ScopedAnalyzer;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Abstract base class for the document builders.
 *
 * @author Hardy Ferentschik
 * @author Davide D'Alto
 * @author Sanne Grinovero
 */
public abstract class AbstractDocumentBuilder<T> {
	private static final Log log = LoggerFactory.make();

	private final XClass beanXClass;
	private final Class<?> beanClass;
	private final TypeMetadata typeMetadata;
	private final InstanceInitializer instanceInitializer;

	private boolean isRoot;
	private Set<Class<?>> mappedSubclasses = new HashSet<Class<?>>();

	protected EntityState entityState;

	/**
	 * Constructor.
	 *
	 * @param xClass The class for which to build a document builder
	 * @param typeMetadata metadata for the specified class
	 * @param reflectionManager Reflection manager to use for processing the annotations
	 * @param optimizationBlackList keeps track of types on which we need to disable collection events optimizations
	 * @param instanceInitializer a {@link org.hibernate.search.spi.InstanceInitializer} object.
	 */
	public AbstractDocumentBuilder(XClass xClass,
			TypeMetadata typeMetadata,
			ReflectionManager reflectionManager,
			Set<XClass> optimizationBlackList,
			InstanceInitializer instanceInitializer) {
		if ( xClass == null ) {
			throw new AssertionFailure( "Unable to build a DocumentBuilderContainedEntity with a null class" );
		}

		this.instanceInitializer = instanceInitializer;
		this.entityState = EntityState.CONTAINED_IN_ONLY;
		this.beanXClass = xClass;
		this.beanClass = reflectionManager.toClass( xClass );
		this.typeMetadata = typeMetadata;

		optimizationBlackList.addAll( typeMetadata.getOptimizationBlackList() );
	}

	public abstract void addWorkToQueue(Class<T> entityClass,
			T entity, Serializable id,
			boolean delete,
			boolean add,
			List<LuceneWork> queue,
			ConversionContext contextualBridge);

	/**
	 * In case of an indexed entity, return the value of it's identifier: what is marked as @Id or @DocumentId;
	 * in case the entity uses @ProvidedId, it's illegal to call this method.
	 *
	 * @param entity the instance for which to retrieve the id
	 *
	 * @return the value, or null if it's not an indexed entity
	 *
	 * @throws IllegalStateException when used with a @ProvidedId annotated entity
	 */
	public abstract Serializable getId(Object entity);

	public TypeMetadata getTypeMetadata() {
		return typeMetadata;
	}

	public boolean isRoot() {
		return isRoot;
	}

	public Class<?> getBeanClass() {
		return beanClass;
	}

	public XClass getBeanXClass() {
		return beanXClass;
	}

	public TypeMetadata getMetadata() {
		return typeMetadata;
	}

	public ScopedAnalyzer getAnalyzer() {
		return typeMetadata.getDefaultAnalyzer();
	}

	public EntityState getEntityState() {
		return entityState;
	}

	public Set<Class<?>> getMappedSubclasses() {
		return mappedSubclasses;
	}

	public void postInitialize(Set<Class<?>> indexedClasses) {
		//we initialize only once because we no longer have a reference to the reflectionManager
		//in theory
		Class<?> plainClass = beanClass;
		if ( entityState == EntityState.NON_INDEXABLE ) {
			throw new AssertionFailure( "A non indexed entity is post processed" );
		}
		Set<Class<?>> tempMappedSubclasses = new HashSet<Class<?>>();
		//together with the caller this creates a o(2), but I think it's still faster than create the up hierarchy for each class
		for ( Class<?> currentClass : indexedClasses ) {
			if ( plainClass != currentClass && plainClass.isAssignableFrom( currentClass ) ) {
				tempMappedSubclasses.add( currentClass );
			}
		}
		this.mappedSubclasses = Collections.unmodifiableSet( tempMappedSubclasses );
		Class<?> superClass = plainClass.getSuperclass();
		this.isRoot = true;
		while ( superClass != null ) {
			if ( indexedClasses.contains( superClass ) ) {
				this.isRoot = false;
				break;
			}
			superClass = superClass.getSuperclass();
		}
	}

	/**
	 * If we have a work instance we have to check whether the instance to be indexed is contained in any other indexed entities.
	 *
	 * @param instance the instance to be indexed
	 * @param workPlan the current work plan
	 * @param currentDepth the current {@link org.hibernate.search.engine.spi.DepthValidator} object used to check the graph traversal
	 */
	public void appendContainedInWorkForInstance(Object instance, WorkPlan workPlan, DepthValidator currentDepth) {
		for ( ContainedInMetadata containedInMetadata : typeMetadata.getContainedInMetadata() ) {
			XMember member = containedInMetadata.getContainedInMember();
			Object unproxiedInstance = instanceInitializer.unproxy( instance );

			DepthValidator depth = updateDepth( unproxiedInstance, containedInMetadata, currentDepth );
			depth.increaseDepth();

			if ( depth.isMaxDepthReached() ) {
				return;
			}

			Object value = ReflectionHelper.getMemberValue( unproxiedInstance, member );

			if ( value == null ) {
				continue;
			}

			if ( member.isArray() ) {
				@SuppressWarnings("unchecked")
				T[] array = (T[]) value;
				for ( T arrayValue : array ) {
					processSingleContainedInInstance( workPlan, arrayValue, depth );
				}
			}
			else if ( member.isCollection() ) {
				Collection<T> collection = null;
				try {
					collection = getActualCollection( member, value );
					collection.size(); //load it
				}
				catch (Exception e) {
					if ( e.getClass().getName().contains( "org.hibernate.LazyInitializationException" ) ) {
						/* A deleted entity not having its collection initialized
						 * leads to a LIE because the collection is no longer attached to the session
						 *
						 * But that's ok as the collection update event has been processed before
						 * or the fk would have been cleared and thus triggering the cleaning
						 */
						collection = null;
					}
				}
				if ( collection != null ) {
					for ( T collectionValue : collection ) {
						processSingleContainedInInstance( workPlan, collectionValue, depth );
					}
				}
			}
			else {
				processSingleContainedInInstance( workPlan, value, depth );
			}
		}
	}

	protected InstanceInitializer getInstanceInitializer() {
		return instanceInitializer;
	}

	private DepthValidator updateDepth(Object instance, ContainedInMetadata containedInMetadata, DepthValidator currentDepth) {
		Integer maxDepth = null;
		if ( instance != null ) {
			maxDepth = containedInMetadata.getMaxDepth();
		}
		if ( maxDepth != null ) {
			if ( currentDepth == null ) {
				return new DepthValidator( maxDepth );
			}
			else {
				int depth = currentDepth.getDepth();
				if ( depth <= maxDepth ) {
					return currentDepth;
				}
				else {
					return new DepthValidator( maxDepth );
				}
			}
		}
		else {
			if ( currentDepth != null ) {
				return currentDepth;
			}
			else {
				return new DepthValidator( Integer.MAX_VALUE );
			}
		}
	}

	@Override
	public String toString() {
		return "DocumentBuilder for {" + beanClass.getName() + "}";
	}

	/**
	 * A {@code XMember } instance treats a map as a collection as well in which case the map values are returned as
	 * collection.
	 *
	 * @param member The member instance
	 * @param value The value
	 *
	 * @return The {@code value} cast to collection or in case of {@code value} being a map the map values as collection.
	 */
	@SuppressWarnings("unchecked")
	private <T> Collection<T> getActualCollection(XMember member, Object value) {
		Collection<T> collection;
		if ( Map.class.equals( member.getCollectionClass() ) ) {
			collection = ( (Map<?, T>) value ).values();
		}
		else {
			collection = (Collection<T>) value;
		}
		return collection;
	}

	private <T> void processSingleContainedInInstance(WorkPlan workplan, T value, DepthValidator depth) {
		workplan.recurseContainedIn( value, depth );
	}

	/**
	 * Hibernate entities might be dirty (their state has changed), but none of these changes would effect
	 * the the index state. This method will return {@code true} if any of changed entity properties identified
	 * by their names ({@code dirtyPropertyNames}) will effect the index state.
	 *
	 * @param dirtyPropertyNames array of property names for the changed entity properties, {@code null} in case the
	 * changed properties cannot be specified.
	 *
	 * @return {@code true} if the entity changes will effect the index state, {@code false} otherwise
	 *
	 * @since 3.4
	 */
	public boolean isDirty(String[] dirtyPropertyNames) {
		if ( dirtyPropertyNames == null || dirtyPropertyNames.length == 0 ) {
			return true; // it appears some collection work has no oldState -> reindex
		}
		if ( !stateInspectionOptimizationsEnabled() ) {
			return true;
		}

		for ( String dirtyPropertyName : dirtyPropertyNames ) {
			PropertyMetadata propertyMetadata = typeMetadata.getPropertyMetadataForProperty( dirtyPropertyName );
			if ( propertyMetadata != null ) {
				// if there is a property metadata it means that there is at least one @Field.
				// Fields are either indexed or stored, so we need to re-index
				return true;
			}

			// consider IndexedEmbedded:
			for ( EmbeddedTypeMetadata embeddedTypeMetadata : typeMetadata.getEmbeddedTypeMetadata() ) {
				String name = embeddedTypeMetadata.getEmbeddedFieldName();
				if ( name.equals( dirtyPropertyName ) ) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * To be removed, see org.hibernate.search.engine.DocumentBuilderIndexedEntity.isIdMatchingJpaId()
	 *
	 * @return true if a providedId needs to be provided for indexing
	 */
	public boolean requiresProvidedId() {
		return false;
	}

	/**
	 * To be removed, see org.hibernate.search.engine.DocumentBuilderIndexedEntity.isIdMatchingJpaId()
	 *
	 * @return true if @DocumentId and @Id are found on the same property
	 */
	public boolean isIdMatchingJpaId() {
		return true;
	}

	/**
	 * Returns {@code true} if the collection event is going to affect the index state. In this case
	 * the indexing event can be ignored. {@code false} otherwise.
	 *
	 * @param collectionRoleName a {@link java.lang.String} object.
	 *
	 * @return {@code true} if an update to the collection identified by the given role name effects the index
	 * state, {@code false} otherwise.
	 */
	public boolean collectionChangeRequiresIndexUpdate(String collectionRoleName) {
		if ( collectionRoleName == null ) {
			// collection name will only be non null for PostCollectionUpdateEvents
			return true;
		}

		// don't check stateInspectionOptimizationsEnabled() as it might ignore depth limit:
		// it will disable optimization even if we have class bridges, but we're too deep
		// to be reachable. The evaluation of stateInspectionOptimizationsEnabled() was
		// actually stored in stateInspectionOptimizationsEnabled, but limiting to depth recursion.
		if ( !typeMetadata.areStateInspectionOptimizationsEnabled() ) {
			// if optimizations are not enabled we need to re-index
			return true;
		}

		return this.typeMetadata.containsCollectionRole( collectionRoleName );
	}


	/**
	 * @deprecated Use {@link #collectionChangeRequiresIndexUpdate(String)} instead
	 */
	public boolean isCollectionRoleExcluded(String collectionRole) {
		return !collectionChangeRequiresIndexUpdate( collectionRole );
	}

	/**
	 * Verifies entity level preconditions to know if it's safe to skip index updates based
	 * on specific field or collection updates.
	 *
	 * @return true if it seems safe to apply such optimizations
	 */
	boolean stateInspectionOptimizationsEnabled() {
		if ( !typeMetadata.areStateInspectionOptimizationsEnabled() ) {
			return false;
		}
		if ( typeMetadata.areClassBridgesUsed() ) {
			log.tracef(
					"State inspection optimization disabled as entity %s uses class bridges",
					this.beanClass.getName()
			);
			return false; // can't know what a class bridge is going to look at -> reindex // TODO nice new feature to have?
		}
		BoostStrategy boostStrategy = typeMetadata.getDynamicBoost();
		if ( boostStrategy != null && !( boostStrategy instanceof DefaultBoostStrategy ) ) {
			log.tracef(
					"State inspection optimization disabled as DynamicBoost is enabled on entity %s",
					this.beanClass.getName()
			);
			return false; // as with class bridge: might be affected by any field // TODO nice new feature to have?
		}
		return true;
	}

	/**
	 * Makes sure isCollectionRoleExcluded will always return false, so that
	 * collection update events are always processed.
	 *
	 * @see #isCollectionRoleExcluded(String)
	 */
	public void forceStateInspectionOptimizationsDisabled() {
		typeMetadata.disableStateInspectionOptimizations();
	}

	/**
	 * Closes any resource
	 */
	public void close() {
		typeMetadata.getDefaultAnalyzer().close();
	}
}
