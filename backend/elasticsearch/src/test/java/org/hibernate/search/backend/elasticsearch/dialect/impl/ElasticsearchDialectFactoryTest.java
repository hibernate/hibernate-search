/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.dialect.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.ElasticsearchDistributionName;
import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.Elasticsearch7ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.Elasticsearch812ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.Elasticsearch814ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.Elasticsearch8ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.ElasticsearchModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.OpenSearch1ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.OpenSearch214ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.OpenSearch29ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.OpenSearch2ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch70ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch80ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch81ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.ElasticsearchProtocolDialect;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.test.extension.ExpectedLog4jLog;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ElasticsearchDialectFactoryTest {

	private enum ExpectedOutcome {
		UNSUPPORTED,
		SUCCESS_WITH_WARNING,
		SUCCESS
	}

	public static List<? extends Arguments> params() {
		return Arrays.asList(
				unsupported( ElasticsearchDistributionName.ELASTIC, "0.90.12" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "0.90.12" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "1.0.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "2.0.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "2.4.4" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "5.0.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "5.2.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "5.6.12" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "5.7.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "6.0.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "6.3.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "6.4.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "6.6.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "6.7.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "6.8.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "6.9.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "7.0.0-beta1" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "7.0.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "7.1.1" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "7.1.1" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "7.2.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "7.3.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "7.3.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "7.4.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "7.4.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "7.5.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "7.5.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "7.6.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "7.6.2" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "7.7.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "7.8.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "7.9.2" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "7.9.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "7.9.2" ),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.10", "7.10.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.10.0", "7.10.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.11", "7.11.1",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.11.0", "7.11.00",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.11.1", "7.11.1",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.12", "7.12.1",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.12.0", "7.12.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.12.1", "7.12.1",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.13", "7.13.2",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.13.0", "7.13.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.13.2", "7.13.2",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.16", "7.16.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.16.0", "7.16.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.17", "7.17.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.17.0", "7.17.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				successWithWarning(
						ElasticsearchDistributionName.ELASTIC, "7.18.0", "7.18.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8", "8.7.1",
						Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.0", "8.0.0",
						Elasticsearch8ModelDialect.class, Elasticsearch80ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.0.0", "8.0.0",
						Elasticsearch8ModelDialect.class, Elasticsearch80ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.1", "8.1.0",
						Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.1.0", "8.1.0",
						Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.2", "8.2.0",
						Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.2.0", "8.2.0",
						Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.3", "8.3.0",
						Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.3.0", "8.3.0",
						Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.4", "8.4.0",
						Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.4.0", "8.4.0",
						Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.5", "8.5.0",
						Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.5.0", "8.5.0",
						Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.6", "8.6.0",
						Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.6.0", "8.6.0",
						Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.7", "8.7.1",
						Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.7.0", "8.7.0",
						Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.8", "8.8.0",
						Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.8.0", "8.8.0",
						Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.9", "8.9.0",
						Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.9.0", "8.9.0",
						Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.10", "8.10.0",
						Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.10.0", "8.10.0",
						Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.11", "8.11.0",
						Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.11.0", "8.11.0",
						Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.12", "8.12.0",
						Elasticsearch812ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.12.0", "8.12.0",
						Elasticsearch812ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.13", "8.13.0",
						Elasticsearch812ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.13.0", "8.13.0",
						Elasticsearch812ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.14", "8.14.0",
						Elasticsearch814ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.14.0", "8.14.0",
						Elasticsearch814ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.15", "8.15.0",
						Elasticsearch814ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.15.0", "8.15.0",
						Elasticsearch814ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.16", "8.16.0",
						Elasticsearch814ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.16.0", "8.16.0",
						Elasticsearch814ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.17", "8.17.0",
						Elasticsearch814ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.17.0", "8.17.0",
						Elasticsearch814ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.17", "8.17.0",
						Elasticsearch814ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.18", "8.18.0",
						Elasticsearch814ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "8.18.0", "8.18.0",
						Elasticsearch814ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				successWithWarning(
						ElasticsearchDistributionName.ELASTIC, "8.19", "8.19.0",
						Elasticsearch814ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				successWithWarning(
						ElasticsearchDistributionName.ELASTIC, "8.19.0", "8.19.0",
						Elasticsearch814ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "9.0", "9.0.0",
						Elasticsearch814ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "9.0.0", "9.0.0",
						Elasticsearch814ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				successWithWarning(
						ElasticsearchDistributionName.ELASTIC, "9.1.0", "9.1.0",
						Elasticsearch814ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "1", "1.3.1",
						OpenSearch1ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				unsupported( ElasticsearchDistributionName.OPENSEARCH, "1.0" ),
				unsupported( ElasticsearchDistributionName.OPENSEARCH, "1.0.0-rc1" ),
				unsupported( ElasticsearchDistributionName.OPENSEARCH, "1.0.0" ),
				unsupported( ElasticsearchDistributionName.OPENSEARCH, "1.0.1" ),
				unsupported( ElasticsearchDistributionName.OPENSEARCH, "1.2" ),
				unsupported( ElasticsearchDistributionName.OPENSEARCH, "1.2.0" ),
				unsupported( ElasticsearchDistributionName.OPENSEARCH, "1.2.1" ),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "1.3", "1.3.1",
						OpenSearch1ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "1.3.0", "1.3.0",
						OpenSearch1ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "1.3.1", "1.3.1",
						OpenSearch1ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				successWithWarning(
						ElasticsearchDistributionName.OPENSEARCH, "1.4", "1.4.0",
						OpenSearch1ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				successWithWarning(
						ElasticsearchDistributionName.OPENSEARCH, "1.4.0", "1.4.0",
						OpenSearch1ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2", "2.3.0",
						OpenSearch2ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.0", "2.3.0",
						OpenSearch2ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.0.0", "2.0.0",
						OpenSearch2ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.1.0", "2.1.0",
						OpenSearch2ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.2.1", "2.2.1",
						OpenSearch2ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.3.0", "2.3.0",
						OpenSearch2ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.4.0", "2.4.0",
						OpenSearch2ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.5.0", "2.5.0",
						OpenSearch2ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.6.0", "2.6.0",
						OpenSearch2ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.7.0", "2.7.0",
						OpenSearch2ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.8", "2.8.0",
						OpenSearch2ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.8.0", "2.8.0",
						OpenSearch2ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.9", "2.9.0",
						OpenSearch29ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.9.0", "2.9.0",
						OpenSearch29ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.10", "2.10.0",
						OpenSearch29ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.10.0", "2.10.0",
						OpenSearch29ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.11", "2.11.0",
						OpenSearch29ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.11.0", "2.11.0",
						OpenSearch29ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.12", "2.12.0",
						OpenSearch29ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.12.0", "2.12.0",
						OpenSearch29ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.13", "2.13.0",
						OpenSearch29ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.13.0", "2.13.0",
						OpenSearch29ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.14", "2.14.0",
						OpenSearch214ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.14.0", "2.14.0",
						OpenSearch214ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.15", "2.15.0",
						OpenSearch214ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.15.0", "2.15.0",
						OpenSearch214ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.16", "2.16.0",
						OpenSearch214ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.16.0", "2.16.0",
						OpenSearch214ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.17", "2.17.0",
						OpenSearch214ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.17.0", "2.17.0",
						OpenSearch214ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.18", "2.18.0",
						OpenSearch214ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.18.0", "2.18.0",
						OpenSearch214ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.19", "2.19.0",
						OpenSearch214ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.19.0", "2.19.0",
						OpenSearch214ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				successWithWarning(
						ElasticsearchDistributionName.OPENSEARCH, "2.20", "2.20.0",
						OpenSearch214ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				successWithWarning(
						ElasticsearchDistributionName.OPENSEARCH, "2.20.0", "2.20.0",
						OpenSearch214ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "3.0", "3.0.0",
						OpenSearch214ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "3.0.0", "3.0.0",
						OpenSearch214ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "3.1", "3.1.0",
						OpenSearch214ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "3.1.0", "3.1.0",
						OpenSearch214ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				successWithWarning(
						ElasticsearchDistributionName.OPENSEARCH, "3.2", "3.2.0",
						OpenSearch214ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				successWithWarning(
						ElasticsearchDistributionName.OPENSEARCH, "3.2.0", "3.2.0",
						OpenSearch214ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				successWithWarning(
						ElasticsearchDistributionName.OPENSEARCH, "4", "4.0.0",
						OpenSearch214ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				successWithWarning(
						ElasticsearchDistributionName.OPENSEARCH, "4.0", "4.0.0",
						OpenSearch214ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				successWithWarning(
						ElasticsearchDistributionName.OPENSEARCH, "4.0.0", "4.0.0",
						OpenSearch214ModelDialect.class, Elasticsearch70ProtocolDialect.class
				)
		);
	}

	private static Arguments unsupported(ElasticsearchDistributionName distribution, String configuredVersionString) {
		return Arguments.of(
				distribution, configuredVersionString, null, ExpectedOutcome.UNSUPPORTED, null, null
		);
	}

	private static Arguments successWithWarning(ElasticsearchDistributionName distribution,
			String configuredVersionString, String actualVersionString,
			Class<? extends ElasticsearchModelDialect> expectedModelDialectClass,
			Class<? extends ElasticsearchProtocolDialect> expectedProtocolDialectClass) {
		return Arguments.of(
				distribution, configuredVersionString, actualVersionString,
				ExpectedOutcome.SUCCESS_WITH_WARNING, expectedModelDialectClass, expectedProtocolDialectClass
		);
	}

	private static Arguments success(ElasticsearchDistributionName distribution,
			String configuredVersionString, String actualVersionString,
			Class<? extends ElasticsearchModelDialect> expectedModelDialectClass,
			Class<? extends ElasticsearchProtocolDialect> expectedProtocolDialectClass) {
		return Arguments.of(
				distribution, configuredVersionString, actualVersionString,
				ExpectedOutcome.SUCCESS, expectedModelDialectClass, expectedProtocolDialectClass
		);
	}

	@RegisterExtension
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	private final ElasticsearchDialectFactory dialectFactory = new ElasticsearchDialectFactory();

	public ElasticsearchDistributionName distributionName;
	public String configuredVersionString;
	public String actualVersionString;
	public Class<? extends ElasticsearchModelDialect> expectedModelDialectClass;
	public Class<? extends ElasticsearchProtocolDialect> expectedProtocolDialectClass;

	@ParameterizedTest(name = "{0} {1}/{2} => {3}")
	@MethodSource("params")
	void test(ElasticsearchDistributionName distributionName,
			String configuredVersionString,
			String actualVersionString,
			ExpectedOutcome expectedOutcome,
			Class<? extends ElasticsearchModelDialect> expectedModelDialectClass,
			Class<? extends ElasticsearchProtocolDialect> expectedProtocolDialectClass
	) {
		this.distributionName = distributionName;
		this.configuredVersionString = configuredVersionString;
		this.actualVersionString = actualVersionString;
		this.expectedModelDialectClass = expectedModelDialectClass;
		this.expectedProtocolDialectClass = expectedProtocolDialectClass;

		switch ( expectedOutcome ) {
			case UNSUPPORTED:
				testUnsupported();
				break;
			case SUCCESS_WITH_WARNING:
				testSuccessWithWarning();
				break;
			case SUCCESS:
				testSuccess();
				break;
		}
	}

	private void testUnsupported() {
		assertThatThrownBy(
				() -> dialectFactory.createModelDialect(
						ElasticsearchVersion.of( distributionName, configuredVersionString ) ),
				"Test unsupported version " + configuredVersionString
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "HSEARCH400081" )
				.hasMessageContaining( "'" + distributionName.toString() + ":" + configuredVersionString + "'" );
	}

	private void testSuccessWithWarning() {
		ElasticsearchVersion parsedConfiguredVersion = ElasticsearchVersion.of( distributionName, configuredVersionString );
		ElasticsearchVersion parsedActualVersion = ElasticsearchVersion.of( distributionName, actualVersionString );

		logged.expectMessage( "HSEARCH400085", "'" + parsedActualVersion + "'" );

		ElasticsearchModelDialect modelDialect = dialectFactory.createModelDialect( parsedConfiguredVersion );
		assertThat( modelDialect ).isInstanceOf( expectedModelDialectClass );

		ElasticsearchProtocolDialect protocolDialect = dialectFactory.createProtocolDialect( parsedActualVersion );
		assertThat( protocolDialect ).isInstanceOf( expectedProtocolDialectClass );
	}

	private void testSuccess() {
		ElasticsearchVersion parsedConfiguredVersion = ElasticsearchVersion.of( distributionName, configuredVersionString );
		ElasticsearchVersion parsedActualVersion = ElasticsearchVersion.of( distributionName, actualVersionString );

		logged.expectMessage( "HSEARCH400085" ).never();

		ElasticsearchModelDialect modelDialect = dialectFactory.createModelDialect( parsedConfiguredVersion );
		assertThat( modelDialect ).isInstanceOf( expectedModelDialectClass );

		ElasticsearchProtocolDialect protocolDialect = dialectFactory.createProtocolDialect( parsedActualVersion );
		assertThat( protocolDialect ).isInstanceOf( expectedProtocolDialectClass );
	}

}
