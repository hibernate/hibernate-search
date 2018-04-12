/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.impl;

import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.dirtiness.building.impl.PojoIndexingDependencyCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.dirtiness.building.impl.PojoIndexingDependencyCollectorValueNode;
import org.hibernate.search.mapper.pojo.model.PojoModelElementAccessor;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;
import org.hibernate.search.mapper.pojo.model.augmented.building.impl.PojoAugmentedTypeModelProvider;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;

public class PojoModelPropertyRootElement<P> extends AbstractPojoModelElement<P> implements PojoModelProperty {

	private final BoundPojoModelPathValueNode<?, P, P> modelPath;

	public PojoModelPropertyRootElement(BoundPojoModelPathPropertyNode<?, P> modelPath,
			PojoAugmentedTypeModelProvider augmentedTypeModelProvider) {
		super( augmentedTypeModelProvider );
		this.modelPath = modelPath.valueWithoutExtractors();
	}

	@Override
	public String toString() {
		return modelPath.getParent().getPropertyModel().toString();
	}

	@Override
	public <M> Stream<M> markers(Class<M> markerType) {
		return Stream.empty();
	}

	@Override
	public String getName() {
		return modelPath.getParent().getPropertyModel().getName();
	}

	public void contributeDependencies(PojoIndexingDependencyCollectorPropertyNode<?, P> dependencyCollector) {
		if ( hasAccessor() ) {
			PojoIndexingDependencyCollectorValueNode<P, P> collectorValueNode =
					dependencyCollector.value( modelPath.getBoundExtractorPath() );
			collectorValueNode.collectDependency();
			contributePropertyDependencies( collectorValueNode.type() );
		}
	}

	@Override
	PojoModelElementAccessor<P> doCreateAccessor() {
		return new PojoModelRootElementAccessor<>();
	}

	@Override
	BoundPojoModelPathTypeNode<P> getModelPathTypeNode() {
		return modelPath.type();
	}
}
