/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingNonFullTextFieldOptionsStep;


abstract class AbstractPropertyMappingNonFullTextStandardFieldOptionsStep<
				S extends PropertyMappingNonFullTextFieldOptionsStep<?>
		>
		extends AbstractPropertyMappingStandardFieldOptionsStep<S>
		implements PropertyMappingNonFullTextFieldOptionsStep<S> {

	AbstractPropertyMappingNonFullTextStandardFieldOptionsStep(PropertyMappingStep parent, String relativeFieldName,
			FieldModelContributor fieldTypeChecker) {
		super( parent, relativeFieldName, fieldTypeChecker );
	}

	@Override
	public S sortable(Sortable sortable) {
		fieldModelContributor.add( c -> c.getStandardTypeOptionsStep().sortable( sortable ) );
		return thisAsS();
	}

	@Override
	public S aggregable(Aggregable aggregable) {
		fieldModelContributor.add( c -> c.getStandardTypeOptionsStep().aggregable( aggregable ) );
		return thisAsS();
	}

	@Override
	public S indexNullAs(String indexNullAs) {
		fieldModelContributor.add( c -> c.indexNullAs( indexNullAs ) );
		return thisAsS();
	}
}
