/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.building.impl;

import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

public final class DerivedDependencyWalkingInfo {
	public final PojoIndexingDependencyCollectorMonomorphicDirectValueNode<?, ?> node;
	public final PojoRawTypeModel<?> definingTypeModel;
	public final PojoModelPath derivedFromPath;

	public DerivedDependencyWalkingInfo(PojoIndexingDependencyCollectorMonomorphicDirectValueNode<?, ?> node,
			PojoModelPath derivedFromPath) {
		this.node = node;
		this.definingTypeModel = node.parentNode.parentNode().typeModel().rawType();
		this.derivedFromPath = derivedFromPath;
	}

	@Override
	public String toString() {
		return definingTypeModel.name() + "#" + derivedFromPath.toPathString();
	}
}
