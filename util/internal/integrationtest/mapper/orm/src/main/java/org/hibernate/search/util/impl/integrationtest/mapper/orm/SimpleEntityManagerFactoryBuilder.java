/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
