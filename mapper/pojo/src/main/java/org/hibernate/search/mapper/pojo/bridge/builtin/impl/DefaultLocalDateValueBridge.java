/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.time.LocalDate;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTypedContext;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBridgeBindingContext;

public final class DefaultLocalDateValueBridge implements ValueBridge<LocalDate, LocalDate> {

	@Override
	public IndexSchemaFieldTypedContext<LocalDate> bind(ValueBridgeBindingContext context) {
		return context.getIndexSchemaFieldContext().asLocalDate();
	}

	@Override
	public LocalDate toIndexedValue(LocalDate value) {
		return value;
	}

}