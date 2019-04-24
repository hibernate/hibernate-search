/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.dependency.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.mapper.pojo.dirtiness.building.impl.PojoIndexingDependencyCollectorNode;
import org.hibernate.search.mapper.pojo.dirtiness.building.impl.PojoIndexingDependencyCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.dirtiness.building.impl.PojoIndexingDependencyCollectorTypeNode;
import org.hibernate.search.mapper.pojo.dirtiness.building.impl.PojoIndexingDependencyCollectorValueNode;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorBinder;
import org.hibernate.search.mapper.pojo.model.dependency.PojoPropertyDependencyContext;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.binding.impl.PojoModelPathBinder;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;

public class PojoPropertyDependencyContextImpl<P> implements PojoPropertyDependencyContext {

	private final BoundPojoModelPath.Walker bindingPathWalker;
	private final BoundPojoModelPathPropertyNode<?, P> modelPath;
	private final Map<ContainerExtractorPath, List<PojoModelPathValueNode>> usedPaths = new LinkedHashMap<>();

	public PojoPropertyDependencyContextImpl(
			ContainerExtractorBinder containerExtractorBinder,
			BoundPojoModelPathPropertyNode<?, P> modelPath) {
		this.bindingPathWalker = BoundPojoModelPath.walker( containerExtractorBinder );
		this.modelPath = modelPath;
		// Always declare the value passed to the bridge as a dependency.
		usedPaths.put( ContainerExtractorPath.noExtractors(), new ArrayList<>() );
	}

	@Override
	public PojoPropertyDependencyContext use(ContainerExtractorPath extractorPathFromBridgedProperty,
			PojoModelPathValueNode pathFromExtractedBridgedPropertyValueToUsedValue) {
		BoundPojoModelPathValueNode<?, ?, ?> extractedValuePath = bindingPathWalker.value(
				modelPath, extractorPathFromBridgedProperty
		);
		PojoModelPathBinder.bind(
				extractedValuePath.type(), pathFromExtractedBridgedPropertyValueToUsedValue, bindingPathWalker
		);

		// If we get here, the path is valid

		usedPaths.computeIfAbsent( extractorPathFromBridgedProperty, ignored -> new ArrayList<>() )
				.add( pathFromExtractedBridgedPropertyValueToUsedValue );
		return this;
	}

	public void contributeDependencies(PojoIndexingDependencyCollectorPropertyNode<?, P> dependencyCollector) {
		for ( Map.Entry<ContainerExtractorPath, List<PojoModelPathValueNode>> entry : usedPaths.entrySet() ) {
			ContainerExtractorPath extractorPathFromBridgedElement = entry.getKey();

			PojoIndexingDependencyCollectorValueNode<?, ?> dependencyCollectorValueNode =
					dependencyCollector.value( extractorPathFromBridgedElement );

			// Always declare the extracted value as a dependency.
			dependencyCollectorValueNode.collectDependency();

			PojoIndexingDependencyCollectorTypeNode<?> dependencyCollectorTypeNode =
					dependencyCollectorValueNode.type();
			for ( PojoModelPathValueNode pathFromExtractedBridgedElementToUsedValue : entry.getValue() ) {
				PojoModelPathBinder.bind(
						dependencyCollectorTypeNode,
						pathFromExtractedBridgedElementToUsedValue,
						PojoIndexingDependencyCollectorNode.walker()
				);
			}
		}
	}
}
