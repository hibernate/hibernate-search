/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public final class SimpleEntityManagerFactoryBuilder {

	private String persistenceUnitName;
	private final Map<String, Object> properties = new HashMap<>();

	public SimpleEntityManagerFactoryBuilder persistenceUnit(String persistenceUnitName) {
		this.persistenceUnitName = persistenceUnitName;
		return this;
	}

	public SimpleEntityManagerFactoryBuilder setProperty(String key, Object value) {
		properties.put( key, value );
		return this;
	}

	public EntityManagerFactory build() {
		return Persistence.createEntityManagerFactory( persistenceUnitName, properties );
	}

}
