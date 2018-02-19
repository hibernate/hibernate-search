/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.bridge;

import org.hibernate.search.mapper.pojo.bridge.FunctionBridge;
import org.hibernate.search.integrationtest.showcase.library.model.LibraryService;

public class LibraryServiceBridge implements FunctionBridge<LibraryService, String> {
	@Override
	public String toIndexedValue(LibraryService propertyValue) {
		return propertyValue == null ? null : propertyValue.name();
	}

	@Override
	public Object fromIndexedValue(String fieldValue) {
		return fieldValue == null ? null : LibraryService.valueOf( fieldValue );
	}
}
