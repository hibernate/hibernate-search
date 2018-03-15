/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.augmented.building.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.mapper.pojo.model.augmented.impl.PojoAugmentedPropertyModel;
import org.hibernate.search.mapper.pojo.model.augmented.impl.PojoAugmentedTypeModel;

class PojoAugmentedTypeModelBuilder implements PojoAugmentedModelCollectorTypeNode {
	private final Map<String, PojoAugmentedPropertyModelBuilder> propertyBuilders = new HashMap<>();

	@Override
	public PojoAugmentedModelCollectorPropertyNode property(String propertyName) {
		return propertyBuilders.computeIfAbsent( propertyName, ignored -> new PojoAugmentedPropertyModelBuilder() );
	}

	public PojoAugmentedTypeModel build() {
		Map<String, PojoAugmentedPropertyModel> properties = new HashMap<>();
		for ( Map.Entry<String, PojoAugmentedPropertyModelBuilder> entry : propertyBuilders.entrySet() ) {
			properties.put( entry.getKey(), entry.getValue().build() );

		}
		return new PojoAugmentedTypeModel( properties );
	}
}
