/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.impl;

import java.util.Map;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;

public class PojoIndexedTypeAdditionalMetadata {
	private final Optional<String> backendName;
	private final Optional<String> indexName;
	private final Optional<RoutingBinder> routingBinder;
	private final Map<String, Object> routingBinderParams;

	public PojoIndexedTypeAdditionalMetadata(Optional<String> backendName, Optional<String> indexName,
			Optional<RoutingBinder> routingBinder, Map<String, Object> routingBinderParams) {
		this.backendName = backendName;
		this.indexName = indexName;
		this.routingBinder = routingBinder;
		this.routingBinderParams = routingBinderParams;
	}

	public Optional<String> backendName() {
		return backendName;
	}

	public Optional<String> indexName() {
		return indexName;
	}

	public Optional<RoutingBinder> routingBinder() {
		return routingBinder;
	}

	public Map<String, Object> routingBinderParams() {
		return routingBinderParams;
	}
}
