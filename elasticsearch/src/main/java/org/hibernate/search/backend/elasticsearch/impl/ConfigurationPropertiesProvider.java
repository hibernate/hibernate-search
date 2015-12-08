/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.util.Properties;

import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.spi.BuildContext;

/**
 * Work-around for getting *all* properties in the index manager.
 *
 * @author Gunnar Morling
 */
// TODO is there a better way?
public class ConfigurationPropertiesProvider implements Service, Startable {

	private Properties properties;

	@Override
	public void start(Properties properties, BuildContext context) {
		this.properties = properties;
	}

	public Properties getProperties() {
		return properties;
	}
}
