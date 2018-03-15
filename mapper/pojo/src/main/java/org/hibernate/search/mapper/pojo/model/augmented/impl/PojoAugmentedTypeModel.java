/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.augmented.impl;

import java.util.Map;

public class PojoAugmentedTypeModel {
	private final Map<String, PojoAugmentedPropertyModel> properties;

	public PojoAugmentedTypeModel(Map<String, PojoAugmentedPropertyModel> properties) {
		this.properties = properties;
	}

	public PojoAugmentedPropertyModel getProperty(String name) {
		return properties.getOrDefault( name, PojoAugmentedPropertyModel.EMPTY );
	}
}
