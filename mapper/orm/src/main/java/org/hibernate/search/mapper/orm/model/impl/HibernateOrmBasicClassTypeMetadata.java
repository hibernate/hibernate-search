/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
