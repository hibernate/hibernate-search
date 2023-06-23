/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.scope.impl.PojoScopeIndexedTypeContext;
import org.hibernate.search.mapper.pojo.scope.impl.PojoScopeTypeContextProvider;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkTypeContext;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkTypeContextProvider;
import org.hibernate.search.util.common.data.spi.KeyValueProvider;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class PojoTypeManagerContainer
		implements AutoCloseable, PojoWorkTypeContextProvider, PojoScopeTypeContextProvider {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static Builder builder() {
		return new Builder();
	}

	private final KeyValueProvider<PojoRawTypeIdentifier<?>, AbstractPojoTypeManager<?, ?>> byExactType;
	private final KeyValueProvider<PojoRawTypeIdentifier<?>, PojoIndexedTypeManager<?, ?>> indexedByExactType;
	private final KeyValueProvider<String, AbstractPojoTypeManager<?, ?>> byEntityName;
	private final KeyValueProvider<String, PojoRawTypeIdentifier<?>> typeIdentifierByEntityName;
	private final KeyValueProvider<PojoRawTypeIdentifier<?>,
			Set<? extends AbstractPojoTypeManager<?, ?>>> allByNonInterfaceSuperType;

	private final Map<PojoRawTypeIdentifier<?>, Set<? extends PojoIndexedTypeManager<?, ?>>> indexedBySuperType;

	private final Collection<PojoIndexedTypeManager<?, ?>> allIndexed;
	private final Set<PojoRawTypeIdentifier<?>> allIndexedAndContainedTypes;
	private final Set<PojoRawTypeIdentifier<?>> allNonInterfaceSuperTypes;
	private final Set<Class<?>> allNonInterfaceSuperTypesClasses;

	private PojoTypeManagerContainer(Builder builder) {
		// Use a LinkedHashMap for deterministic iteration in the "all" set
		Map<PojoRawTypeIdentifier<?>, AbstractPojoTypeManager<?, ?>> byExactTypeContent = new LinkedHashMap<>();
		Map<PojoRawTypeIdentifier<?>, PojoIndexedTypeManager<?, ?>> indexedByExactTypeContent = new LinkedHashMap<>();
		Map<String, AbstractPojoTypeManager<?, ?>> byEntityNameContent = new LinkedHashMap<>();
		Map<String, PojoRawTypeIdentifier<?>> typeIdentifierByEntityNameContent = new LinkedHashMap<>();
		Set<Class<?>> allNonInterfaceSuperTypesClassesContent = new HashSet<>();

		for ( PojoIndexedTypeManager<?, ?> typeManager : builder.indexed ) {
			PojoRawTypeIdentifier<?> typeIdentifier = typeManager.typeIdentifier;

			byExactTypeContent.put( typeIdentifier, typeManager );
			indexedByExactTypeContent.put( typeIdentifier, typeManager );

			byEntityNameContent.put( typeManager.entityName(), typeManager );
		}
		for ( PojoContainedTypeManager<?, ?> typeManager : builder.contained ) {
			PojoRawTypeIdentifier<?> typeIdentifier = typeManager.typeIdentifier;

			byExactTypeContent.put( typeIdentifier, typeManager );

			byEntityNameContent.put( typeManager.entityName(), typeManager );
		}
		for ( PojoRawTypeIdentifier<?> identifier : builder.allByNonInterfaceSuperType.keySet() ) {
			// we want to skip any interfaces since this set should only be used to report an error when we cannot
			// find a type by a class, and that should only be a case when we are looking up non-named types.
			if ( !identifier.javaClass().isInterface() ) {
				allNonInterfaceSuperTypesClassesContent.add( identifier.javaClass() );
			}
		}
		// only limit to the indexed/contained and their supertype entities, no need to keep the others.
		for ( Map.Entry<String, PojoRawTypeModel<?>> entry : builder.allEntitiesByName.entrySet() ) {
			PojoRawTypeIdentifier<?> typedIdentifier = entry.getValue().typeIdentifier();
			if ( builder.allByNonInterfaceSuperType.containsKey( typedIdentifier ) ) {
				typeIdentifierByEntityNameContent.put( entry.getKey(), typedIdentifier );
			}
		}

		this.byExactType = new KeyValueProvider<>( byExactTypeContent, log::unknownTypeIdentifierForMappedEntityType );
		this.indexedByExactType =
				new KeyValueProvider<>( indexedByExactTypeContent, log::unknownTypeIdentifierForIndexedEntityType );
		this.byEntityName = new KeyValueProvider<>( byEntityNameContent, log::unknownEntityNameForMappedEntityType );
		this.typeIdentifierByEntityName =
				new KeyValueProvider<>( typeIdentifierByEntityNameContent, log::unknownEntityNameForAnyEntityByName );

		builder.allByNonInterfaceSuperType.replaceAll( (k, v) -> Collections.unmodifiableSet( v ) );
		this.allByNonInterfaceSuperType =
				new KeyValueProvider<>( builder.allByNonInterfaceSuperType, log::unknownSupertypeTypeIdentifier );
		this.allNonInterfaceSuperTypes = Collections.unmodifiableSet( builder.allByNonInterfaceSuperType.keySet() );

		this.indexedBySuperType = new LinkedHashMap<>( builder.indexedBySuperType );
		indexedBySuperType.replaceAll( (k, v) -> Collections.unmodifiableSet( v ) );

		this.allIndexed = Collections.unmodifiableCollection( indexedByExactTypeContent.values() );
		this.allIndexedAndContainedTypes = Collections.unmodifiableSet( byExactTypeContent.keySet() );
		this.allNonInterfaceSuperTypesClasses = Collections.unmodifiableSet( allNonInterfaceSuperTypesClassesContent );
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( AbstractPojoTypeManager::close, allIndexed );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> AbstractPojoTypeManager<?, E> forExactType(PojoRawTypeIdentifier<E> typeIdentifier) {
		return (AbstractPojoTypeManager<?, E>) byExactType.getOrFail( typeIdentifier );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> PojoIndexedTypeManager<?, E> indexedForExactType(PojoRawTypeIdentifier<E> typeIdentifier) {
		return (PojoIndexedTypeManager<?, E>) indexedByExactType.getOrFail( typeIdentifier );
	}

	@Override
	public Set<PojoRawTypeIdentifier<?>> allIndexedSuperTypes() {
		return indexedBySuperType.keySet();
	}

	@Override
	public Set<PojoRawTypeIdentifier<?>> allNonInterfaceSuperTypes() {
		return allNonInterfaceSuperTypes;
	}

	@Override
	public Set<Class<?>> allNonInterfaceSuperTypesClasses() {
		return allNonInterfaceSuperTypesClasses;
	}

	@Override
	public Set<PojoRawTypeIdentifier<?>> allIndexedAndContainedTypes() {
		return allIndexedAndContainedTypes;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> Optional<? extends Set<? extends PojoScopeIndexedTypeContext<?, ? extends E>>> allIndexedForSuperType(
			PojoRawTypeIdentifier<E> typeIdentifier) {
		return Optional.ofNullable( (Set<PojoIndexedTypeManager<?, ? extends E>>) indexedBySuperType.get( typeIdentifier ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> Set<? extends PojoWorkTypeContext<?, ? extends E>> allByNonInterfaceSuperType(
			PojoRawTypeIdentifier<E> typeIdentifier) {
		return (Set<? extends PojoWorkTypeContext<?, ? extends E>>) allByNonInterfaceSuperType.getOrFail(
				typeIdentifier );
	}

	@Override
	public KeyValueProvider<String, ? extends PojoWorkTypeContext<?, ?>> byEntityName() {
		return byEntityName;
	}

	@Override
	public KeyValueProvider<String, PojoRawTypeIdentifier<?>> typeIdentifierByEntityName() {
		return typeIdentifierByEntityName;
	}

	Collection<PojoIndexedTypeManager<?, ?>> allIndexed() {
		return allIndexed;
	}

	public static class Builder {

		// Use a LinkedHashMap for deterministic iteration
		private final List<PojoIndexedTypeManager<?, ?>> indexed = new ArrayList<>();
		private final List<PojoContainedTypeManager<?, ?>> contained = new ArrayList<>();
		private final Map<PojoRawTypeIdentifier<?>, Set<PojoIndexedTypeManager<?, ?>>> indexedBySuperType =
				new LinkedHashMap<>();
		private final Map<PojoRawTypeIdentifier<?>, Set<AbstractPojoTypeManager<?, ?>>> allByNonInterfaceSuperType =
				new LinkedHashMap<>();
		private final Map<String, PojoRawTypeModel<?>> allEntitiesByName = new LinkedHashMap<>();

		private Builder() {
		}

		public <E> void addIndexed(PojoRawTypeModel<E> typeModel, PojoIndexedTypeManager<?, E> typeManager) {
			indexed.add( typeManager );
			typeModel.descendingSuperTypes()
					.map( PojoRawTypeModel::typeIdentifier )
					.forEach( clazz -> {
						indexedBySuperType.computeIfAbsent( clazz, ignored -> new LinkedHashSet<>() )
								.add( typeManager );
						if ( clazz.isNamed() || !clazz.javaClass().isInterface() ) {
							allByNonInterfaceSuperType.computeIfAbsent( clazz, ignored -> new LinkedHashSet<>() )
									.add( typeManager );
						}
					} );
		}

		public <E> void addContained(PojoRawTypeModel<E> typeModel, PojoContainedTypeManager<?, E> typeManager) {
			contained.add( typeManager );
			typeModel.descendingSuperTypes()
					.map( PojoRawTypeModel::typeIdentifier )
					.filter( clazz -> clazz.isNamed() || !clazz.javaClass().isInterface() )
					.forEach( clazz -> allByNonInterfaceSuperType.computeIfAbsent( clazz, ignored -> new LinkedHashSet<>() )
							.add( typeManager ) );
		}

		public void addEntity(String entityName, PojoRawTypeModel<?> entityType) {
			allEntitiesByName.put( entityName, entityType );
		}

		public void closeOnFailure() {
			try ( Closer<RuntimeException> closer = new Closer<>() ) {
				closer.pushAll( PojoIndexedTypeManager::close, indexed );
				closer.pushAll( PojoContainedTypeManager::close, contained );
			}
		}

		public PojoTypeManagerContainer build() {
			return new PojoTypeManagerContainer( this );
		}
	}

}
