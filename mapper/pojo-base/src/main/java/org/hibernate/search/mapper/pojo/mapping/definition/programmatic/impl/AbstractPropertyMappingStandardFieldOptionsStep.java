/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStandardFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;


abstract class AbstractPropertyMappingStandardFieldOptionsStep<S extends PropertyMappingStandardFieldOptionsStep<?>>
		extends AbstractPropertyMappingFieldOptionsStep<S>
		implements PropertyMappingStandardFieldOptionsStep<S> {

	AbstractPropertyMappingStandardFieldOptionsStep(PropertyMappingStep parent, String relativeFieldName,
			FieldModelContributor fieldTypeChecker) {
		super( parent, relativeFieldName, fieldTypeChecker );
	}

	@Override
	public S projectable(Projectable projectable) {
		fieldModelContributor.add( c -> c.getStandardTypeOptionsStep().projectable( projectable ) );
		return thisAsS();
	}

	@Override
	public S searchable(Searchable searchable) {
		fieldModelContributor.add( c -> c.getStandardTypeOptionsStep().searchable( searchable ) );
		return thisAsS();
	}

	@Override
	public S aggregable(Aggregable aggregable) {
		fieldModelContributor.add( c -> c.getStandardTypeOptionsStep().aggregable( aggregable ) );
		return thisAsS();
	}

}
