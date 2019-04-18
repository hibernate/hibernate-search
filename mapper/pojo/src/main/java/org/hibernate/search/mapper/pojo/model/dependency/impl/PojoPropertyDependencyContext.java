/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.dependency.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.mapper.pojo.dirtiness.building.impl.PojoIndexingDependencyCollectorNode;
import org.hibernate.search.mapper.pojo.dirtiness.building.impl.PojoIndexingDependencyCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.dirtiness.building.impl.PojoIndexingDependencyCollectorTypeNode;
import org.hibernate.search.mapper.pojo.dirtiness.building.impl.PojoIndexingDependencyCollectorValueNode;
import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorBinder;
import org.hibernate.search.mapper.pojo.model.dependency.PojoDependencyContext;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.binding.impl.PojoModelPathBinder;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;

public class PojoPropertyDependencyContext<P> implements PojoDependencyContext {

	private final BoundPojoModelPath.Walker bindingPathWalker;
	private final PojoPropertyModel<P> propertyModel;
	private final BoundPojoModelPathTypeNode<P> rootPath;
	private final List<BoundPojoModelPathValueNode<?, ?, ?>> usedPaths = new ArrayList<>();

	public PojoPropertyDependencyContext(
			ContainerExtractorBinder containerExtractorBinder,
			PojoPropertyModel<P> propertyModel) {
		this.bindingPathWalker = BoundPojoModelPath.walker( containerExtractorBinder );
		this.propertyModel = propertyModel;
		this.rootPath = BoundPojoModelPath.root( propertyModel.getTypeModel() );
	}

	@Override
	public PojoDependencyContext use(PojoModelPathValueNode pathFromBridgedElementToUsedValue) {
		BoundPojoModelPathValueNode<?, ?, ?> boundPath = PojoModelPathBinder.bind(
				rootPath, pathFromBridgedElementToUsedValue, bindingPathWalker
		);
		usedPaths.add( boundPath );
		return this;
	}

	public void contributeDependencies(PojoIndexingDependencyCollectorPropertyNode<?, P> dependencyCollector) {
		// Always declare the value passed to the bridge as a dependency.
		PojoIndexingDependencyCollectorValueNode<?, ?> dependencyCollectorValueNode = dependencyCollector.value(
				BoundContainerExtractorPath.noExtractors( propertyModel.getTypeModel() )
		);
		dependencyCollectorValueNode.collectDependency();

		PojoIndexingDependencyCollectorTypeNode<?> dependencyCollectorTypeNode =
				dependencyCollectorValueNode.type();
		for ( BoundPojoModelPathValueNode<?, ?, ?> usedPath : usedPaths ) {
			PojoModelPathBinder.bind(
					dependencyCollectorTypeNode,
					usedPath.toUnboundPath(),
					PojoIndexingDependencyCollectorNode.walker()
			);
		}
	}

}
