/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.configuration.properties.collector.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lists the config parameters that can be passed to this annotation processor via {@code -A.....}.
 */
public final class ConfigurationRules {

	private static final Set<String> IGNORED_CLASSES = Collections.emptySet();

	private static final Set<String> IGNORED_CONSTANTS = new HashSet<>( Arrays.asList(
			"org.hibernate.search.engine.cfg.BackendSettings#INDEXES",
			"org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings#TYPE_NAME",
			"org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings#TYPE_NAME",
			"org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings#SHARDS",
			"org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings#COORDINATION",
			"org.hibernate.search.mapper.orm.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings#COORDINATION_STRATEGY_NAME",
			"org.hibernate.search.engine.cfg.EngineSettings#BACKEND",
			"org.hibernate.search.engine.cfg.EngineSettings#BACKENDS",
			"org.hibernate.search.engine.cfg.BackendSettings#INDEXES"
	) );

	private ConfigurationRules() {
	}

	public static boolean isClassIgnored(String className) {
		return !className.endsWith( "Settings" )
				|| className.contains( ".impl." )
				|| className.contains( ".internal." )
				|| IGNORED_CLASSES.contains( className );
	}

	public static boolean isConstantIgnored(String className, String constantName, String constantValue) {
		return constantValue.endsWith( "." ) || IGNORED_CONSTANTS.contains( className + "#" + constantName );
	}

	public static List<String> prefixes(String className, String propertyKey) {
		if ( className.endsWith( "BackendSettings" ) ) {
			return Arrays.asList( "hibernate.search.backend.", "hibernate.search.backends.<backend-name>." );
		}
		else if ( className.endsWith( "IndexSettings" ) ) {
			return Arrays.asList(
					"hibernate.search.backend.",
					"hibernate.search.backend.indexes.<index-name>.",
					"hibernate.search.backends.<backend-name>.",
					"hibernate.search.backends.<backend-name>.indexes.<index-name>."
			);
		}
		else {
			return !propertyKey.startsWith( "hibernate.search." )
					? Collections.singletonList( "hibernate.search." )
					: Collections.emptyList();
		}
	}
}
