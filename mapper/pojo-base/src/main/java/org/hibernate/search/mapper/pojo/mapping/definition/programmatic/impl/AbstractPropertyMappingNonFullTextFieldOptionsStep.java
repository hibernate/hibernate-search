/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.function.Function;

import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingNonFullTextFieldOptionsStep;


abstract class AbstractPropertyMappingNonFullTextFieldOptionsStep<
				S extends PropertyMappingNonFullTextFieldOptionsStep<?>,
				C extends StandardIndexFieldTypeOptionsStep<?, ?>
		>
		extends AbstractPropertyMappingFieldOptionsStep<S, C>
		implements PropertyMappingNonFullTextFieldOptionsStep<S> {

	AbstractPropertyMappingNonFullTextFieldOptionsStep(PropertyMappingStep parent, String relativeFieldName,
			Function<StandardIndexFieldTypeOptionsStep<?, ?>, C> typeOptionsStepCaster) {
		super( parent, relativeFieldName, typeOptionsStepCaster );
	}

	@Override
	public S sortable(Sortable sortable) {
		fieldModelContributor.add( (c, b) -> c.sortable( sortable ) );
		return thisAsS();
	}

	@Override
	public S indexNullAs(String indexNullAs) {
		fieldModelContributor.add( (c, b) -> b.indexNullAs( indexNullAs ) );
		return thisAsS();
	}
}
