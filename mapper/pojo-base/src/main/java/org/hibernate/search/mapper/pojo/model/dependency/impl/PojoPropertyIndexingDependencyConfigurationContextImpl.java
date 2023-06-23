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

import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.AbstractPojoIndexingDependencyCollectorDirectValueNode;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorNode;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorTypeNode;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorBinder;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl.PojoTypeAdditionalMetadataProvider;
import org.hibernate.search.mapper.pojo.model.dependency.PojoOtherEntityIndexingDependencyConfigurationContext;
import org.hibernate.search.mapper.pojo.model.dependency.PojoPropertyIndexingDependencyConfigurationContext;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathOriginalTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoModelPathBinder;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;

public class PojoPropertyIndexingDependencyConfigurationContextImpl<P> extends AbstractPojoBridgedElementDependencyContext
		implements PojoPropertyIndexingDependencyConfigurationContext {

	private final BoundPojoModelPathPropertyNode<?, P> modelPath;
	private final Map<ContainerExtractorPath, ValueDependencyContext> valueDependencyContexts = new LinkedHashMap<>();

	public PojoPropertyIndexingDependencyConfigurationContextImpl(
			PojoBootstrapIntrospector introspector,
			ContainerExtractorBinder containerExtractorBinder,
			PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider,
			BoundPojoModelPathPropertyNode<?, P> modelPath) {
		super( introspector, containerExtractorBinder, typeAdditionalMetadataProvider );
		this.modelPath = modelPath;
	}

	@Override
	public PojoPropertyIndexingDependencyConfigurationContext use(ContainerExtractorPath extractorPathFromBridgedProperty,
			PojoModelPathValueNode pathFromExtractedBridgedPropertyValueToUsedValue) {
		valueDependencyContexts.computeIfAbsent( extractorPathFromBridgedProperty, ValueDependencyContext::new )
				.use( pathFromExtractedBridgedPropertyValueToUsedValue );
		return this;
	}

	@Override
	public PojoOtherEntityIndexingDependencyConfigurationContext fromOtherEntity(
			ContainerExtractorPath extractorPathFromBridgedProperty,
			Class<?> otherEntityType,
			PojoModelPathValueNode pathFromOtherEntityTypeToBridgedPropertyExtractedType) {
		return valueDependencyContexts.computeIfAbsent( extractorPathFromBridgedProperty, ValueDependencyContext::new )
				.addOtherEntityDependencyContext( otherEntityType, pathFromOtherEntityTypeToBridgedPropertyExtractedType );
	}

	@Override
	public void useRootOnly() {
		super.useRootOnly();
		// Declare the value passed to the bridge as a dependency
		ContainerExtractorPath noExtractorPath = ContainerExtractorPath.noExtractors();
		valueDependencyContexts.put( noExtractorPath, new ValueDependencyContext( noExtractorPath ) );
	}

	@Override
	public boolean hasNonRootDependency() {
		if ( valueDependencyContexts.isEmpty() ) {
			return false;
		}
		if ( valueDependencyContexts.size() > 1 ) {
			return true;
		}
		ValueDependencyContext noExtractorValue = valueDependencyContexts.get( ContainerExtractorPath.noExtractors() );
		return noExtractorValue == null // If true, the only value dependency was added by a call to use(...), not useRootOnly()
				|| noExtractorValue.hasExplicitDependency(); // If true, the only value dependency was populated with calls to use(...)
	}

	public void contributeDependencies(PojoIndexingDependencyCollectorPropertyNode<?, P> dependencyCollector) {
		for ( Map.Entry<ContainerExtractorPath, ValueDependencyContext> entry : valueDependencyContexts.entrySet() ) {
			ContainerExtractorPath extractorPathFromBridgedElement = entry.getKey();

			AbstractPojoIndexingDependencyCollectorDirectValueNode<?, ?> dependencyCollectorValueNode =
					dependencyCollector.value( extractorPathFromBridgedElement );

			// Always declare the extracted value as a dependency.
			dependencyCollectorValueNode.collectDependency();

			entry.getValue().contributeDependencies( dependencyCollectorValueNode );
		}
	}

	private class ValueDependencyContext {
		private final BoundPojoModelPathOriginalTypeNode<?> valueTypePath;
		private final List<PojoModelPathValueNode> usedPaths = new ArrayList<>();
		private final List<PojoOtherEntityIndexingDependencyConfigurationContextImpl<?>> otherEntityDependencyContexts =
				new ArrayList<>();

		private ValueDependencyContext(ContainerExtractorPath extractorPathFromBridgedProperty) {
			BoundPojoModelPathValueNode<?, ?, ?> valuePath =
					bindingPathWalker.value( modelPath, extractorPathFromBridgedProperty );
			valueTypePath = valuePath.type();
		}

		public void contributeDependencies(
				AbstractPojoIndexingDependencyCollectorDirectValueNode<?, ?> dependencyCollectorValueNode) {
			PojoIndexingDependencyCollectorTypeNode<?> dependencyCollectorTypeNode =
					dependencyCollectorValueNode.type();
			for ( PojoModelPathValueNode pathFromExtractedBridgedElementToUsedValue : usedPaths ) {
				PojoModelPathBinder.bind(
						dependencyCollectorTypeNode,
						pathFromExtractedBridgedElementToUsedValue,
						PojoIndexingDependencyCollectorNode.walker()
				);
			}
			for ( PojoOtherEntityIndexingDependencyConfigurationContextImpl<
					?> otherEntityDependencyContext : otherEntityDependencyContexts ) {
				otherEntityDependencyContext.contributeDependencies( dependencyCollectorTypeNode );
			}
		}

		private PojoOtherEntityIndexingDependencyConfigurationContextImpl<?> addOtherEntityDependencyContext(
				Class<?> otherEntityType, PojoModelPathValueNode pathFromOtherEntityTypeToBridgedPropertyExtractedType) {
			PojoOtherEntityIndexingDependencyConfigurationContextImpl<?> otherContext = createOtherEntityDependencyContext(
					valueTypePath.getTypeModel().rawType(),
					otherEntityType, pathFromOtherEntityTypeToBridgedPropertyExtractedType
			);

			// If we get here, the path is valid

			otherEntityDependencyContexts.add( otherContext );
			return otherContext;
		}

		public void use(PojoModelPathValueNode pathFromExtractedBridgedPropertyValueToUsedValue) {
			PojoModelPathBinder.bind(
					valueTypePath, pathFromExtractedBridgedPropertyValueToUsedValue, bindingPathWalker
			);

			// If we get here, the path is valid

			usedPaths.add( pathFromExtractedBridgedPropertyValueToUsedValue );
		}

		public boolean hasExplicitDependency() {
			return !usedPaths.isEmpty() || !otherEntityDependencyContexts.isEmpty();
		}
	}
}
