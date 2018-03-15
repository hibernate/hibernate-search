/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.augmented.building.impl;

import org.hibernate.search.mapper.pojo.extractor.impl.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.model.augmented.impl.PojoAugmentedValueModel;
import org.hibernate.search.mapper.pojo.model.augmented.impl.PojoAssociationPath;

class PojoAugmentedValueModelBuilder implements PojoAugmentedModelCollectorValueNode {
	private PojoAssociationPath inverseSidePath;

	@Override
	public void associationInverseSide(String inversePropertyName, ContainerValueExtractorPath inverseExtractorPath) {
		this.inverseSidePath = new PojoAssociationPath( inversePropertyName, inverseExtractorPath );
	}

	PojoAugmentedValueModel build() {
		return new PojoAugmentedValueModel( inverseSidePath );
	}
}
