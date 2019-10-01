/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.impl;

import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorTypeNode;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorValueNode;
import org.hibernate.search.mapper.pojo.model.PojoElementAccessor;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl.PojoTypeAdditionalMetadataProvider;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoPropertyAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

/**
 * @param <T> The type holding the property.
 * @param <P> The type of the property.
 */
class PojoModelNestedCompositeElement<T, P> extends AbstractPojoModelCompositeElement<P> implements PojoModelProperty {

	private final AbstractPojoModelCompositeElement<T> parent;
	private final BoundPojoModelPathValueNode<T, P, P> modelPath;
	private final PojoPropertyAdditionalMetadata propertyAdditionalMetadata;

	PojoModelNestedCompositeElement(AbstractPojoModelCompositeElement<T> parent, BoundPojoModelPathPropertyNode<T, P> modelPath,
			PojoPropertyAdditionalMetadata propertyAdditionalMetadata,
			PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider) {
		super( typeAdditionalMetadataProvider );
		this.parent = parent;
		this.modelPath = modelPath.valueWithoutExtractors();
		this.propertyAdditionalMetadata = propertyAdditionalMetadata;
	}

	@Override
	public <M> Stream<M> markers(Class<M> markerType) {
		return propertyAdditionalMetadata.getMarkers( markerType );
	}

	@Override
	public String getName() {
		return modelPath.getParent().getPropertyModel().getName();
	}

	public void contributeDependencies(PojoIndexingDependencyCollectorTypeNode<T> dependencyCollector) {
		if ( hasAccessor() ) {
			@SuppressWarnings( "unchecked" ) // We used the same property as in modelPath, on the same type. The result must have the same type.
			PojoIndexingDependencyCollectorPropertyNode<T, P> collectorPropertyNode =
					(PojoIndexingDependencyCollectorPropertyNode<T, P>) dependencyCollector.property( getName() );
			PojoIndexingDependencyCollectorValueNode<P, P> collectorValueNode =
					collectorPropertyNode.value( modelPath.getBoundExtractorPath() );
			collectorValueNode.collectDependency();
			contributePropertyDependencies( collectorValueNode.type() );
		}
	}

	@Override
	PojoElementAccessor<P> doCreateAccessor() {
		return new PojoPropertyElementAccessor<>( parent.createAccessor(), getHandle() );
	}

	@Override
	BoundPojoModelPathTypeNode<P> getModelPathTypeNode() {
		return modelPath.type();
	}

	ValueReadHandle<P> getHandle() {
		return modelPath.getParent().getValueReadHandle();
	}
}
