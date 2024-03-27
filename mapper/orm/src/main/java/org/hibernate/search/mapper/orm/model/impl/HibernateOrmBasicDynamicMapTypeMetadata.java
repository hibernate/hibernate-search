/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
