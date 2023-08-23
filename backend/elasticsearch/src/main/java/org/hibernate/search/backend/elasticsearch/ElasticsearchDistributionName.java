/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public enum ElasticsearchDistributionName {

	/**
	 * The Elasticsearch distribution from Elastic.
	 * <p>
	 * See: <a href="https://www.elastic.co/elasticsearch/">https://www.elastic.co/elasticsearch/</a>.
	 */
	ELASTIC( "elastic", "elastic" ),
	/**
	 * The OpenSearch distribution from the OpenSearch organization.
	 * <p>
	 * When used through Amazon OpenSearch Service, requires extra dependencies for authentication;
	 * refer to the reference documentation.
	 * <p>
	 * See: <a href="https://www.opensearch.org/">https://www.opensearch.org/</a>.
	 */
	OPENSEARCH( "opensearch", "opensearch" ),
	/**
	 * Amazon OpenSearch Serverless.
	 * <p>
	 * Requires extra dependencies for authentication;
	 * refer to the reference documentation.
	 * <p>
	 * See: <a href="https://docs.aws.amazon.com/opensearch-service/latest/developerguide/serverless-overview.html">https://docs.aws.amazon.com/opensearch-service/latest/developerguide/serverless-overview.html</a>.
	 */
	@Incubating
	AMAZON_OPENSEARCH_SERVERLESS( "amazon-opensearch-serverless", null );

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static ElasticsearchDistributionName of(String value) {
		return ParseUtils.parseDiscreteValues(
				ElasticsearchDistributionName.values(),
				ElasticsearchDistributionName::externalRepresentation,
				log::invalidElasticsearchDistributionName,
				value
		);
	}

	static List<String> allowedExternalRepresentations() {
		return Arrays.stream( ElasticsearchDistributionName.values() )
				.map( ElasticsearchDistributionName::externalRepresentation )
				.collect( Collectors.toList() );
	}

	public static ElasticsearchDistributionName fromServerResponseRepresentation(String value) {
		return ParseUtils.parseDiscreteValues(
				ElasticsearchDistributionName.values(),
				ElasticsearchDistributionName::serverResponseRepresentation,
				log::invalidElasticsearchDistributionName,
				value
		);
	}

	static ElasticsearchDistributionName defaultValue() {
		return ElasticsearchDistributionName.ELASTIC;
	}

	private final String externalRepresentation;
	private final String serverResponseRepresentation;

	ElasticsearchDistributionName(String externalRepresentation, String serverResponseRepresentation) {
		this.externalRepresentation = externalRepresentation;
		this.serverResponseRepresentation = serverResponseRepresentation;
	}

	@Override
	public String toString() {
		return externalRepresentation;
	}

	/**
	 * @return The expected string representation in configuration properties.
	 */
	public String externalRepresentation() {
		return externalRepresentation;
	}

	String serverResponseRepresentation() {
		return serverResponseRepresentation;
	}

}
