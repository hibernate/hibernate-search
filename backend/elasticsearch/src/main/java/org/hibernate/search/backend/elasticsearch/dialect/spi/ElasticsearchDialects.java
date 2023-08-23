/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
