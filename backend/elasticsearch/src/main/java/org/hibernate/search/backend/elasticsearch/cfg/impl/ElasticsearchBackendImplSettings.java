/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.cfg.impl;

/**
 * Implementation-related settings, used for testing only.
 */
@Deprecated(since = "8.2", forRemoval = true)
public final class ElasticsearchBackendImplSettings {

	private ElasticsearchBackendImplSettings() {
	}

	public static final String CLIENT_FACTORY = "client_factory";

}
