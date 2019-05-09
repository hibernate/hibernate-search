/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.spi;

import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;

public interface FieldModelContributor {

	/**
	 * Add a default decimal scale value if no decimal scale value has been set on it.
	 *
	 * @param defaultScale value to add
	 */
	void defaultDecimalScale(int defaultScale);

	void contribute(StandardIndexFieldTypeContext<?, ?> fieldTypeContext, FieldModelContributorIndirectContext bridgeContext);

}
