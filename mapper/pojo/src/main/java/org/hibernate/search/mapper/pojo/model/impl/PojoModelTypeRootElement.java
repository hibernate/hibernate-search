/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.impl;

import org.hibernate.search.mapper.pojo.dirtiness.building.impl.PojoIndexingDependencyCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.PojoModelElementAccessor;
import org.hibernate.search.mapper.pojo.model.PojoModelType;
import org.hibernate.search.mapper.pojo.model.augmented.building.impl.PojoAugmentedTypeModelProvider;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;

public class PojoModelTypeRootElement<T> extends AbstractPojoModelElement<T> implements PojoModelType {

	private final BoundPojoModelPathTypeNode<T> modelPath;

	public PojoModelTypeRootElement(BoundPojoModelPathTypeNode<T> modelPath,
			PojoAugmentedTypeModelProvider augmentedTypeModelProvider) {
		super( augmentedTypeModelProvider );
		this.modelPath = modelPath;
	}

	@Override
	public String toString() {
		return modelPath.getTypeModel().toString();
	}

	public void contributeDependencies(PojoIndexingDependencyCollectorTypeNode<T> dependencyCollector) {
		contributePropertyDependencies( dependencyCollector );
	}

	@Override
	PojoModelElementAccessor<T> doCreateAccessor() {
		return new PojoModelRootElementAccessor<>();
	}

	@Override
	BoundPojoModelPathTypeNode<T> getModelPathTypeNode() {
		return modelPath;
	}
}
