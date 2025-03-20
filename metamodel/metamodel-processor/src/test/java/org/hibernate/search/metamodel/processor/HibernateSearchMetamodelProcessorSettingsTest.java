/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;

import org.junit.jupiter.api.Test;

class HibernateSearchMetamodelProcessorSettingsTest {

	@Test
	void defaultVersionsUpToDate() {
		HibernateSearchMetamodelProcessorSettings.Configuration configuration =
				new HibernateSearchMetamodelProcessorSettings.Configuration( Map.of() );
		assertThat( configuration.luceneVersion() )
				.isEqualTo( LuceneBackendSettings.Defaults.LUCENE_VERSION.toString() );
		assertThat( configuration.elasticsearchVersion() )
				.isEqualTo( System.getProperty( "org.hibernate.search.integrationtest.backend.elasticsearch.version" ) );
	}
}
