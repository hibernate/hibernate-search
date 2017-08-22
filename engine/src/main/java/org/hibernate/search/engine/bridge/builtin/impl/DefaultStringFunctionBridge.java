/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.bridge.builtin.impl;

import org.hibernate.search.engine.backend.document.model.spi.FieldModelContext;
import org.hibernate.search.engine.backend.document.model.spi.TypedFieldModelContext;
import org.hibernate.search.engine.bridge.spi.FunctionBridge;

public final class DefaultStringFunctionBridge implements FunctionBridge<String, String> {

	@Override
	public TypedFieldModelContext<String> bind(FieldModelContext context) {
		return context.fromString();
	}

	@Override
	public String toDocument(String propertyValue) {
		return propertyValue;
	}

}