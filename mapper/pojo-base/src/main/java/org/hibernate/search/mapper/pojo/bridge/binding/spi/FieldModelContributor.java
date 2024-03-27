/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
