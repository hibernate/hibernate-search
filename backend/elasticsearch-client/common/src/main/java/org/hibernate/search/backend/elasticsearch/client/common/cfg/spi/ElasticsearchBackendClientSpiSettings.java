/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.common.cfg.spi;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Implementation-related settings.
 */
@Incubating
public final class ElasticsearchBackendClientSpiSettings {

	private ElasticsearchBackendClientSpiSettings() {
	}

	public static final String CLIENT_FACTORY = "client_factory";

}
