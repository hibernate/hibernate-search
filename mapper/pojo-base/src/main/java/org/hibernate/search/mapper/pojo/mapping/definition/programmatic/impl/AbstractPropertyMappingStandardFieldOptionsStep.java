/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStandardFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

abstract class AbstractPropertyMappingStandardFieldOptionsStep<S extends PropertyMappingStandardFieldOptionsStep<?>>
		extends AbstractPropertyMappingFieldOptionsStep<S>
		implements PropertyMappingStandardFieldOptionsStep<S> {

	AbstractPropertyMappingStandardFieldOptionsStep(PropertyMappingStep parent, String relativeFieldName,
			PojoCompositeFieldModelContributor.Contributor fieldTypeChecker) {
		super( parent, relativeFieldName, IndexFieldTypeFactory::as, fieldTypeChecker );
	}

	@Override
	public S projectable(Projectable projectable) {
		fieldModelContributor.add( c -> c.standardTypeOptionsStep().projectable( projectable ) );
		return thisAsS();
	}

	@Override
	public S searchable(Searchable searchable) {
		fieldModelContributor.add( c -> c.standardTypeOptionsStep().searchable( searchable ) );
		return thisAsS();
	}

}
