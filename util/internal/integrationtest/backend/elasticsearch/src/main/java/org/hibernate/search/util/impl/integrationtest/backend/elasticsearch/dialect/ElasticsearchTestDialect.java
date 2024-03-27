/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;

import org.hibernate.search.backend.elasticsearch.ElasticsearchDistributionName;
import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchTestHostConnectionConfiguration;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

public class ElasticsearchTestDialect {

	private static ElasticsearchVersion actualVersion;

	private static final ElasticsearchTestDialect INSTANCE = new ElasticsearchTestDialect();
	private static final String LOCAL_DATE_DEFAULT_FORMAT = "uuuu-MM-dd";

	public static ElasticsearchTestDialect get() {
		return INSTANCE;
	}

	public static ElasticsearchVersion getActualVersion() {
		if ( actualVersion == null ) {
			ElasticsearchDistributionName distribution =
					ElasticsearchDistributionName.of( System.getProperty(
							"org.hibernate.search.integrationtest.backend.elasticsearch.distribution" ) );
			String versionString = "";

			if ( distribution != ElasticsearchDistributionName.AMAZON_OPENSEARCH_SERVERLESS ) {
				try ( TestElasticsearchClient client = TestElasticsearchClient.create() ) {
					client.open( new TestConfigurationProvider(), Optional.empty() );
					versionString = client.getActualVersion();
					if ( versionString != null ) {
						// If we got a snapshot version back from the actual cluster we want to drop that qualifier part of version.
						// Qualifiers are not allowed when comparing versions with a comparator from this class
						// (see this#compare(ElasticsearchVersion a, ElasticsearchVersion b, int defaultInt)).
						int dashIndex = versionString.indexOf( '-' );
						if ( dashIndex > -1 ) {
							versionString = versionString.substring( 0, dashIndex );
						}
					}
				}
				catch (IOException e) {
					throw new IllegalStateException(
							"Wasn't able to detect the actual version of " + distribution.toString() + "cluster", e );
				}
			}

			actualVersion = ElasticsearchVersion.of( distribution, versionString.isBlank() ? null : versionString );
		}
		return actualVersion;
	}

	public String getLocalDateDefaultMappingFormat() {
		return LOCAL_DATE_DEFAULT_FORMAT;
	}

	public String vectorFieldMapping(int dim, Class<?> elementType, OptionalInt m, OptionalInt efConstruction,
			Optional<String> similarity) {
		switch ( getActualVersion().distribution() ) {
			case ELASTIC:
				return "{"
						+ "  'type': 'dense_vector',"
						+ "  'element_type': '" + elementType.getName() + "',"
						+ "  'dims': " + dim
						+ similarity.map( s -> ",  'similarity': '" + s + "'" ).orElse( "" )
						+ ( ( m.isPresent() || efConstruction.isPresent() )
								? ( ",  'index_options': {"
										+ ( m.isPresent() ? "    'm': " + m.getAsInt() + "," : "" )
										+ ( efConstruction.isPresent()
												? "    'ef_construction': " + efConstruction.getAsInt() + ","
												: "" )
										+ "    'type': 'hnsw'"
										+ "  }" )
								: "" )
						+ "}";
			case OPENSEARCH:
			case AMAZON_OPENSEARCH_SERVERLESS:
				return "{"
						+ "  'type': 'knn_vector',"
						+ "  'dimension': " + dim + ","
						+ "  'data_type': '" + elementType.getName() + "',"
						+ "  'method': {"
						+ "    'name': 'hnsw',"
						+ similarity.map( s -> "    'space_type': '" + s + "'," ).orElse( "" )
						+ "    'engine': 'lucene'"
						+ ( ( m.isPresent() || efConstruction.isPresent() )
								? ",    'parameters': {"
										+ ( m.isPresent() ? "    'm': " + m.getAsInt() : "" )
										+ ( efConstruction.isPresent()
												? ( m.isPresent() ? "," : "" ) + "    'ef_construction': "
														+ efConstruction.getAsInt()
												: "" )
										+ "    }"
								: "" )
						+ "  }"
						+ "}";
			default:
				throw new IllegalStateException( "Unknown distribution" );
		}
	}

	public Map<String, String> vectorFieldNames() {
		Map<String, String> map = new HashMap<>();
		switch ( getActualVersion().distribution() ) {
			case ELASTIC:
				map.put( "dimension", "dims" );
				map.put( "element_type", "element_type" );
				break;
			case OPENSEARCH:
			case AMAZON_OPENSEARCH_SERVERLESS:
				map.put( "dimension", "dimension" );
				map.put( "element_type", "data_type" );
				break;
			default:
				throw new IllegalStateException( "Unknown distribution" );
		}
		return map;
	}

	public String elementType(Class<?> type) {
		switch ( getActualVersion().distribution() ) {
			case ELASTIC:
				if ( byte.class.equals( type ) ) {
					return "byte";
				}
				if ( float.class.equals( type ) ) {
					return "float";
				}
				throw new AssertionFailure( "Unknown element type" );
			case OPENSEARCH:
			case AMAZON_OPENSEARCH_SERVERLESS:
				if ( byte.class.equals( type ) ) {
					return "BYTE";
				}
				if ( float.class.equals( type ) ) {
					return null;
				}
				throw new AssertionFailure( "Unknown element type" );
			default:
				throw new IllegalStateException( "Unknown distribution" );
		}
	}

	public String vectorSimilarity(VectorSimilarity similarity) {
		switch ( getActualVersion().distribution() ) {
			case ELASTIC:
				switch ( similarity ) {
					case L2:
						return "l2_norm";
					case DOT_PRODUCT:
						return "dot_product";
					case COSINE:
						return "cosine";
					default:
						throw new IllegalStateException( "Unknown similarity" );
				}
			case OPENSEARCH:
			case AMAZON_OPENSEARCH_SERVERLESS:
				switch ( similarity ) {
					case L2:
						return "l2";
					case DOT_PRODUCT:
						return "l2";
					case COSINE:
						return "cosinesimil";
					default:
						throw new IllegalStateException( "Unknown similarity" );
				}
			default:
				throw new IllegalStateException( "Unknown distribution" );
		}
	}

	public boolean supportsExplicitPurge() {
		return isActualVersion(
				es -> true,
				os -> true,
				aoss -> false
		);
	}

	public static boolean isActualVersion(
			Predicate<ElasticsearchVersionCondition> elasticsearchPredicate,
			Predicate<ElasticsearchVersionCondition> openSearchPredicate
	) {
		return isActualVersion(
				elasticsearchPredicate,
				openSearchPredicate,
				null
		);
	}

	public static boolean isActualVersion(
			Predicate<ElasticsearchVersionCondition> elasticsearchPredicate,
			Predicate<ElasticsearchVersionCondition> openSearchPredicate,
			Predicate<ElasticsearchVersionCondition> amazonOpenSearchServerlessPredicate
	) {
		return isVersion(
				getActualVersion(),
				elasticsearchPredicate,
				openSearchPredicate,
				amazonOpenSearchServerlessPredicate
		);
	}

	static boolean isVersion(
			ElasticsearchVersion version,
			Predicate<ElasticsearchVersionCondition> elasticsearchPredicate,
			Predicate<ElasticsearchVersionCondition> openSearchPredicate,
			Predicate<ElasticsearchVersionCondition> amazonOpenSearchServerlessPredicate
	) {
		ElasticsearchVersionCondition condition = new ElasticsearchVersionCondition( version );

		switch ( version.distribution() ) {
			case ELASTIC:
				return elasticsearchPredicate.test( condition );
			case OPENSEARCH:
				return openSearchPredicate.test( condition );
			case AMAZON_OPENSEARCH_SERVERLESS:
				if ( amazonOpenSearchServerlessPredicate != null ) {
					return amazonOpenSearchServerlessPredicate.test( condition );
				}
				else {
					// The caller doesn't handle AOSS; let's behave as if we were on the latest OpenSearch version.
					condition = new ElasticsearchVersionCondition( ElasticsearchVersion.of( "opensearch:999.999" ) );
					return openSearchPredicate.test( condition );
				}
			default:
				throw new IllegalStateException( "Unknown distribution" );
		}
	}

	public static class ElasticsearchVersionCondition {
		private final ElasticsearchVersion actual;

		private ElasticsearchVersionCondition(ElasticsearchVersion actual) {
			this.actual = actual;
		}

		public boolean isAws() {
			return ElasticsearchTestHostConnectionConfiguration.get().isAws();
		}

		public boolean isMatching(String version) {
			ElasticsearchVersion v = ElasticsearchVersion.of( actual.distribution(), version );

			return v.matches( actual );
		}

		public boolean isAtMost(String version) {
			ElasticsearchVersion v = ElasticsearchVersion.of( actual.distribution(), version );

			return compare( actual, v, Integer.MAX_VALUE ) <= 0;
		}

		public boolean isLessThan(String version) {
			ElasticsearchVersion v = ElasticsearchVersion.of( actual.distribution(), version );

			return compare( actual, v, Integer.MAX_VALUE ) < 0;
		}

		public boolean isBetween(String minVersion, String maxVersion) {
			ElasticsearchVersion min = ElasticsearchVersion.of( actual.distribution(), minVersion );
			ElasticsearchVersion max = ElasticsearchVersion.of( actual.distribution(), maxVersion );

			return !( compare( max, actual, Integer.MAX_VALUE ) < 0 || compare( min, actual, Integer.MIN_VALUE ) > 0 );
		}

		public ElasticsearchVersion actual() {
			return actual;
		}

		private static int compare(ElasticsearchVersion a, ElasticsearchVersion b, int defaultInt) {
			if ( !a.distribution().equals( b.distribution() ) ) {
				throw new IllegalArgumentException( "Cannot compare different distributions" );
			}

			if ( a.qualifier().isPresent() || b.qualifier().isPresent() ) {
				throw new IllegalArgumentException( "Qualifiers are ignored for version ranges." );
			}

			return Comparator.comparing( (ElasticsearchVersion version) -> version.majorOptional().orElse( defaultInt ) )
					.thenComparing( version -> version.minor().orElse( defaultInt ) )
					.thenComparing( version -> version.micro().orElse( defaultInt ) )
					.compare( a, b );
		}
	}
}
