/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.impl;

import java.util.Collection;
import java.util.Collections;

import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.AbstractPojoIndexingDependencyCollectorDirectValueNode;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.model.PojoElementAccessor;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl.PojoTypeAdditionalMetadataProvider;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;

/**
 * @param <P> The type of the property used as a root element.
 */
public class PojoModelPropertyRootElement<P> extends AbstractPojoModelCompositeElement<P> implements PojoModelProperty {

	private final BoundPojoModelPathValueNode<?, P, P> modelPath;

	public PojoModelPropertyRootElement(BoundPojoModelPathPropertyNode<?, P> modelPath,
			PojoBootstrapIntrospector introspector,
			PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider) {
		super( introspector, typeAdditionalMetadataProvider );
		this.modelPath = modelPath.valueWithoutExtractors();
	}

	@Override
	public String toString() {
		return modelPath.getParent().getPropertyModel().toString();
	}

	@Override
	public <M> Collection<M> markers(Class<M> markerType) {
		return Collections.emptyList();
	}

	@Override
	public String name() {
		return modelPath.getParent().getPropertyModel().name();
	}

	public void contributeDependencies(PojoIndexingDependencyCollectorPropertyNode<?, P> dependencyCollector) {
		if ( hasAccessor() ) {
			AbstractPojoIndexingDependencyCollectorDirectValueNode<P, P> collectorValueNode =
					dependencyCollector.value( modelPath.getBoundExtractorPath() );
			collectorValueNode.collectDependency();
			contributePropertyDependencies( collectorValueNode.type() );
		}
	}

	@Override
	PojoElementAccessor<P> doCreateAccessor() {
		return new PojoRootElementAccessor<>();
	}

	@Override
	BoundPojoModelPathTypeNode<P> getModelPathTypeNode() {
		return modelPath.type();
	}
}
