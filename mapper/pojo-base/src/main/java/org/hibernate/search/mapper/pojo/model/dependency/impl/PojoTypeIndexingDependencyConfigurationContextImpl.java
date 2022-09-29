/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.dependency.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorNode;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorTypeNode;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorBinder;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl.PojoTypeAdditionalMetadataProvider;
import org.hibernate.search.mapper.pojo.model.dependency.PojoOtherEntityIndexingDependencyConfigurationContext;
import org.hibernate.search.mapper.pojo.model.dependency.PojoTypeIndexingDependencyConfigurationContext;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoModelPathBinder;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

public class PojoTypeIndexingDependencyConfigurationContextImpl<T> extends AbstractPojoBridgedElementDependencyContext
		implements PojoTypeIndexingDependencyConfigurationContext {

	private final BoundPojoModelPathTypeNode<T> modelPath;
	private final List<BoundPojoModelPathValueNode<?, ?, ?>> usedPaths = new ArrayList<>();
	private final List<PojoOtherEntityIndexingDependencyConfigurationContextImpl<?>> otherEntityDependencyContexts = new ArrayList<>();

	public PojoTypeIndexingDependencyConfigurationContextImpl(
			PojoBootstrapIntrospector introspector,
			ContainerExtractorBinder containerExtractorBinder,
			PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider,
			PojoTypeModel<T> typeModel) {
		super( introspector, containerExtractorBinder, typeAdditionalMetadataProvider );
		this.modelPath = BoundPojoModelPath.root( typeModel );
	}

	@Override
	public boolean hasNonRootDependency() {
		return !usedPaths.isEmpty() || !otherEntityDependencyContexts.isEmpty();
	}

	@Override
	public PojoTypeIndexingDependencyConfigurationContext use(PojoModelPathValueNode pathFromBridgedTypeToUsedValue) {
		BoundPojoModelPathValueNode<?, ?, ?> boundPath = PojoModelPathBinder.bind(
				modelPath, pathFromBridgedTypeToUsedValue, bindingPathWalker
		);
		usedPaths.add( boundPath );
		return this;
	}

	@Override
	public PojoOtherEntityIndexingDependencyConfigurationContext fromOtherEntity(Class<?> otherEntityType,
			PojoModelPathValueNode pathFromOtherEntityTypeToBridgedType) {
		PojoOtherEntityIndexingDependencyConfigurationContextImpl<?> otherEntityDependencyContext = createOtherEntityDependencyContext(
				modelPath.getTypeModel().rawType(),
				otherEntityType, pathFromOtherEntityTypeToBridgedType
		);

		// If we get here, the path is valid

		otherEntityDependencyContexts.add( otherEntityDependencyContext );

		return otherEntityDependencyContext;
	}

	public void contributeDependencies(PojoIndexingDependencyCollectorTypeNode<T> dependencyCollector) {
		for ( BoundPojoModelPathValueNode<?, ?, ?> usedPath : usedPaths ) {
			PojoModelPathBinder.bind(
					dependencyCollector,
					usedPath.toUnboundPath(),
					PojoIndexingDependencyCollectorNode.walker()
			);
		}
		for ( PojoOtherEntityIndexingDependencyConfigurationContextImpl<?> otherEntityDependencyContext : otherEntityDependencyContexts ) {
			otherEntityDependencyContext.contributeDependencies( dependencyCollector );
		}
	}

}
