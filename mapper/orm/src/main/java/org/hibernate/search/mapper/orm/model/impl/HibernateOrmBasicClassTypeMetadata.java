/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.util.Map;

class HibernateOrmBasicClassTypeMetadata {

	private final Map<String, HibernateOrmBasicClassPropertyMetadata> properties;

	HibernateOrmBasicClassTypeMetadata(Map<String, HibernateOrmBasicClassPropertyMetadata> properties) {
		this.properties = properties;
	}

	HibernateOrmBasicClassPropertyMetadata getClassPropertyMetadataOrNull(String propertyName) {
		return properties.get( propertyName );
	}

}
