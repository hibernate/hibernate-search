/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.bridge;

import org.hibernate.search.mapper.pojo.bridge.FunctionBridge;
import org.hibernate.search.integrationtest.showcase.library.model.ISBN;

public class ISBNBridge implements FunctionBridge<ISBN, String> {

	// TODO use a default normalizer that removes hyphens

	@Override
	public String toIndexedValue(ISBN propertyValue) {
		return propertyValue == null ? null : propertyValue.getStringValue();
	}

	@Override
	public Object fromIndexedValue(String fieldValue) {
		return fieldValue == null ? null : new ISBN( fieldValue );
	}
}
