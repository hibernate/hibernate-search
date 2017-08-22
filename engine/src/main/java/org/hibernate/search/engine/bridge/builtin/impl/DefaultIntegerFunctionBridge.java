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

public final class DefaultIntegerFunctionBridge implements FunctionBridge<Integer, Integer> {

	@Override
	public TypedFieldModelContext<Integer> bind(FieldModelContext context) {
		return context.fromInteger();
	}

	@Override
	public Integer toDocument(Integer propertyValue) {
		return propertyValue;
	}

}