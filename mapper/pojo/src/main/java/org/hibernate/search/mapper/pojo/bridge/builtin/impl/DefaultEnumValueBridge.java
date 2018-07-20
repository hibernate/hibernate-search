/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTypedContext;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.model.PojoModelValue;

public final class DefaultEnumValueBridge<T extends Enum<T>> implements ValueBridge<T, String> {

	private Class<T> enumType;

	@Override
	@SuppressWarnings("unchecked") // The bridge resolver performs the checks using reflection
	public IndexSchemaFieldTypedContext<String> bind(PojoModelValue<T> pojoModelValue,
			IndexSchemaFieldContext fieldContext) {
		this.enumType = (Class<T>) pojoModelValue.getRawType();
		return fieldContext.asString();
	}

	@Override
	public String toIndexedValue(T value) {
		return value == null ? null : value.name();
	}

	@Override
	public T fromIndexedValue(String indexedValue) {
		return indexedValue == null ? null : Enum.valueOf( enumType, indexedValue );
	}

}