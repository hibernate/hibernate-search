/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.spi;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeOptionsStep;

public interface FieldModelContributor {

	default <F> IndexFieldTypeOptionsStep<?, F> inferDefaultFieldType(IndexFieldTypeFactory factory, Class<F> clazz) {
		return factory.as( clazz );
	}

	void contribute(FieldModelContributorContext context);

}
