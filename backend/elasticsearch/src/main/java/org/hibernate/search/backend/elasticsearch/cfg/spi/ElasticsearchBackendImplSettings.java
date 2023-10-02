/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.cfg.spi;

/**
 * Implementation-related settings, used for testing only.
 *
 * @deprecated Use {@link org.hibernate.search.backend.elasticsearch.cfg.impl.ElasticsearchBackendImplSettings} instead.
 */
@Deprecated
public final class ElasticsearchBackendImplSettings {

	private ElasticsearchBackendImplSettings() {
	}

	public static final String CLIENT_FACTORY = "client_factory";

}
