/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.model;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.VectorField;

public class SomeRandomTypeBinder implements PropertyBinder {

	@FullTextField
	IndexFieldReference<String> stringSomeRandomTypeBinder;

	@GenericField
	IndexFieldReference<Integer> integerSomeRandomTypeBinder;

	@KeywordField
	IndexFieldReference<String> keywordSomeRandomTypeBinder;

	@VectorField(dimension = 15)
	private IndexFieldReference<byte[]> bytesSomeRandomTypeBinder;

	@VectorField(dimension = 5)
	private IndexFieldReference<float[]> floatsSomeRandomTypeBinder;

	@Override
	public void bind(PropertyBindingContext context) {
		// we don't care much about binding here... only about the fields ^
	}
}
