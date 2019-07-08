/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.directory.impl;

import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryProviderInitializationContext;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.util.common.reporting.EventContext;

public class DirectoryProviderInitializationContextImpl implements DirectoryProviderInitializationContext {

	private final EventContext eventContext;
	private final ConfigurationPropertySource configurationPropertySource;

	public DirectoryProviderInitializationContextImpl(EventContext eventContext,
			ConfigurationPropertySource configurationPropertySource) {
		this.eventContext = eventContext;
		this.configurationPropertySource = configurationPropertySource;
	}

	@Override
	public EventContext getEventContext() {
		return eventContext;
	}

	@Override
	public ConfigurationPropertySource getConfigurationPropertySource() {
		return configurationPropertySource;
	}

}
