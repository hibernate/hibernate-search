/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.scope.model.impl;

import java.util.HashMap;
import java.util.Map;

public class LuceneScopedIndexObjectComponent<T> {

	private final Map<String, T> fieldComponents = new HashMap<>();

	public void addFieldComponent(String absoluteFieldPath, T component) {
		fieldComponents.put( absoluteFieldPath, component );
	}

	public Map<String, T> getFieldComponents() {
		return fieldComponents;
	}
}
