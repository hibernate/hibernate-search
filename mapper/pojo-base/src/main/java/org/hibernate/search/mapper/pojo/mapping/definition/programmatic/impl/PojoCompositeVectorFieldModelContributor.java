/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.function.Function;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeOptionsStep;

class PojoCompositeVectorFieldModelContributor extends PojoCompositeFieldModelContributor {

	private final int dimension;

	public PojoCompositeVectorFieldModelContributor(int dimension) {
		this.dimension = dimension;
	}

	@Override
	public <F> Function<Class<F>, IndexFieldTypeOptionsStep<?, F>> initialStepCreator(
			IndexFieldTypeFactory indexFieldTypeFactory) {
		return c -> indexFieldTypeFactory.asVector( dimension, c );
	}
}
