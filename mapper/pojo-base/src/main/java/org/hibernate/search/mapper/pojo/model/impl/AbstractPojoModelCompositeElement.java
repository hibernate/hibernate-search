/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorTypeNode;
import org.hibernate.search.mapper.pojo.logging.impl.MappingLog;
import org.hibernate.search.mapper.pojo.model.PojoElementAccessor;
import org.hibernate.search.mapper.pojo.model.PojoModelCompositeElement;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl.PojoTypeAdditionalMetadataProvider;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoPropertyAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

/**
 * @param <V> The type of the element, i.e. the type of values returned by accessors to this element.
 */
public abstract class AbstractPojoModelCompositeElement<V> implements PojoModelCompositeElement {

	private final PojoBootstrapIntrospector introspector;
	private final PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider;
	// Use a LinkedHashMap for deterministic iteration
	private final Map<String, PojoModelNestedCompositeElement<V, ?>> properties = new LinkedHashMap<>();
	private PojoTypeAdditionalMetadata typeAdditionalMetadata;
	private boolean propertiesInitialized = false;

	private PojoElementAccessor<?> accessor;

	AbstractPojoModelCompositeElement(AbstractPojoModelCompositeElement<?> parent) {
		this.introspector = parent.introspector;
		this.typeAdditionalMetadataProvider = parent.typeAdditionalMetadataProvider;
	}

	AbstractPojoModelCompositeElement(PojoBootstrapIntrospector introspector,
			PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider) {
		this.introspector = introspector;
		this.typeAdditionalMetadataProvider = typeAdditionalMetadataProvider;
	}

	@Override
	@SuppressWarnings("unchecked") // The cast is checked using reflection
	public final <T> PojoElementAccessor<T> createAccessor(Class<T> requestedType) {
		if ( !isAssignableTo( requestedType ) ) {
			throw MappingLog.INSTANCE.incompatibleRequestedType( getModelPathTypeNode().toUnboundPath(), requestedType );
		}
		return (PojoElementAccessor<T>) createAccessor();
	}

	@Override
	public PojoElementAccessor<?> createAccessor() {
		if ( accessor == null ) {
			accessor = doCreateAccessor();
		}
		return accessor;
	}

	@Override
	public boolean isAssignableTo(Class<?> clazz) {
		return getTypeModel().rawType().isSubTypeOf( introspector.typeModel( clazz ) );
	}

	@Override
	public Class<?> rawType() {
		return getTypeModel().rawType().typeIdentifier().javaClass();
	}

	@Override
	public PojoModelNestedCompositeElement<?, ?> property(String relativeFieldName) {
		return properties.computeIfAbsent( relativeFieldName, name -> {
			BoundPojoModelPathTypeNode<V> modelPathTypeNode = getModelPathTypeNode();
			PojoPropertyAdditionalMetadata additionalMetadata =
					getTypeAdditionalMetadata().getPropertyAdditionalMetadata( name );
			return new PojoModelNestedCompositeElement<>(
					this,
					modelPathTypeNode.property( name ),
					additionalMetadata
			);
		} );
	}

	@Override
	public Collection<? extends PojoModelProperty> properties() {
		if ( !propertiesInitialized ) {
			// Populate all the known properties
			getTypeModel().rawType().ascendingSuperTypes()
					.flatMap( pojoRawTypeModel -> pojoRawTypeModel.declaredProperties().stream() )
					.map( PojoPropertyModel::name )
					.forEach( this::property );
			propertiesInitialized = true;
		}
		return Collections.unmodifiableCollection( properties.values() );
	}

	public boolean hasDependency() {
		return hasAccessor();
	}

	public boolean hasNonRootDependency() {
		for ( PojoModelNestedCompositeElement<V, ?> property : properties.values() ) {
			if ( property.hasAccessor() ) {
				return true;
			}
		}
		return false;
	}

	abstract PojoElementAccessor<V> doCreateAccessor();

	abstract BoundPojoModelPathTypeNode<V> getModelPathTypeNode();

	final boolean hasAccessor() {
		return accessor != null;
	}

	final void contributePropertyDependencies(PojoIndexingDependencyCollectorTypeNode<V> dependencyCollector) {
		for ( Map.Entry<String, PojoModelNestedCompositeElement<V, ?>> entry : properties.entrySet() ) {
			entry.getValue().contributeDependencies( dependencyCollector );
		}
	}

	private PojoTypeModel<V> getTypeModel() {
		return getModelPathTypeNode().getTypeModel();
	}

	private PojoTypeAdditionalMetadata getTypeAdditionalMetadata() {
		if ( typeAdditionalMetadata == null ) {
			typeAdditionalMetadata = typeAdditionalMetadataProvider.get( getModelPathTypeNode().getTypeModel().rawType() );
		}
		return typeAdditionalMetadata;
	}
}
