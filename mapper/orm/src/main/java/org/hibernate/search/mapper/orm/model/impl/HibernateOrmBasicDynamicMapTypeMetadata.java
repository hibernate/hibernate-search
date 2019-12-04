/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.util.Map;
import java.util.Set;

class HibernateOrmBasicDynamicMapTypeMetadata {

	private final String superEntityName;
	private final Map<String, HibernateOrmBasicDynamicMapPropertyMetadata> properties;

	HibernateOrmBasicDynamicMapTypeMetadata(String superEntityName,
			Map<String, HibernateOrmBasicDynamicMapPropertyMetadata> properties) {
		this.superEntityName = superEntityName;
		this.properties = properties;
	}

	String getSuperEntityNameOrNull() {
		return superEntityName;
	}

	HibernateOrmBasicDynamicMapPropertyMetadata getDynamicMapPropertyMetadataOrNull(String propertyName) {
		return properties.get( propertyName );
	}

	Set<String> getPropertyNames() {
		return properties.keySet();
	}
}
