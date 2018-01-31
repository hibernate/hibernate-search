/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.time.LocalDate;

import org.hibernate.search.engine.backend.document.model.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.IndexSchemaFieldTypedContext;
import org.hibernate.search.mapper.pojo.bridge.FunctionBridge;

public final class DefaultLocalDateFunctionBridge implements FunctionBridge<LocalDate, LocalDate> {

	@Override
	public IndexSchemaFieldTypedContext<LocalDate> bind(IndexSchemaFieldContext context) {
		return context.asLocalDate();
	}

	@Override
	public LocalDate toIndexedValue(LocalDate propertyValue) {
		return propertyValue;
	}

}