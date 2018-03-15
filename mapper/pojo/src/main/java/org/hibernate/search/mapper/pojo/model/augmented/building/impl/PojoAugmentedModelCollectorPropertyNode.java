/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.augmented.building.impl;

import org.hibernate.search.mapper.pojo.bridge.mapping.MarkerBuilder;

public interface PojoAugmentedModelCollectorPropertyNode extends PojoAugmentedModelCollector {

	void marker(MarkerBuilder definition);

}
