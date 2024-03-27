/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.dialect.spi;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.dialect.impl.ElasticsearchDialectFactory;

/**
 * Utils for integrations that require advanced checks on Elasticsearch versions,
 * i.e. for two-phase bootstrap like in Quarkus.
 */
public final class ElasticsearchDialects {

	private ElasticsearchDialects() {
	}

	public static boolean isPreciseEnoughForBootstrap(ElasticsearchVersion version) {
		return ElasticsearchDialectFactory.isPreciseEnoughForModelDialect( version );
	}

	public static boolean isPreciseEnoughForStart(ElasticsearchVersion version) {
		return ElasticsearchDialectFactory.isPreciseEnoughForProtocolDialect( version );
	}

	public static boolean isVersionCheckImpossible(ElasticsearchVersion version) {
		return ElasticsearchDialectFactory.isVersionCheckImpossible( version );
	}

}
