/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.cfg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.OptionalInt;

import org.hibernate.search.backend.elasticsearch.ElasticsearchDistributionName;
import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.util.common.SearchException;

import org.junit.Test;

import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.ObjectArrayAssert;

public class ElasticsearchVersionTest {

	@Test
	public void of_string() {
		assertComponents( ElasticsearchVersion.of( "7" ) )
				.containsExactly( ElasticsearchDistributionName.ELASTIC, 7, null, null, null );
		assertComponents( ElasticsearchVersion.of( "7.0" ) )
				.containsExactly( ElasticsearchDistributionName.ELASTIC, 7, 0, null, null );
		assertComponents( ElasticsearchVersion.of( "7.0.0" ) )
				.containsExactly( ElasticsearchDistributionName.ELASTIC, 7, 0, 0, null );
		assertComponents( ElasticsearchVersion.of( "7.0.1" ) )
				.containsExactly( ElasticsearchDistributionName.ELASTIC, 7, 0, 1, null );
		assertComponents( ElasticsearchVersion.of( "7.1" ) )
				.containsExactly( ElasticsearchDistributionName.ELASTIC, 7, 1, null, null );
		assertComponents( ElasticsearchVersion.of( "7.1.0" ) )
				.containsExactly( ElasticsearchDistributionName.ELASTIC, 7, 1, 0, null );
		assertComponents( ElasticsearchVersion.of( "7.1.2" ) )
				.containsExactly( ElasticsearchDistributionName.ELASTIC, 7, 1, 2, null );
		assertComponents( ElasticsearchVersion.of( "7.0.0-beta1" ) )
				.containsExactly( ElasticsearchDistributionName.ELASTIC, 7, 0, 0, "beta1" );

		assertComponents( ElasticsearchVersion.of( "elastic:7.0.0-beta1" ) )
				.containsExactly( ElasticsearchDistributionName.ELASTIC, 7, 0, 0, "beta1" );

		assertComponents( ElasticsearchVersion.of( "opensearch:1" ) )
				.containsExactly( ElasticsearchDistributionName.OPENSEARCH, 1, null, null, null );
		assertComponents( ElasticsearchVersion.of( "opensearch:1.0" ) )
				.containsExactly( ElasticsearchDistributionName.OPENSEARCH, 1, 0, null, null );
		assertComponents( ElasticsearchVersion.of( "opensearch:1.0.0" ) )
				.containsExactly( ElasticsearchDistributionName.OPENSEARCH, 1, 0, 0, null );
		assertComponents( ElasticsearchVersion.of( "opensearch:1.0.3" ) )
				.containsExactly( ElasticsearchDistributionName.OPENSEARCH, 1, 0, 3, null );
		assertComponents( ElasticsearchVersion.of( "opensearch:1.1" ) )
				.containsExactly( ElasticsearchDistributionName.OPENSEARCH, 1, 1, null, null );
		assertComponents( ElasticsearchVersion.of( "opensearch:1.1.0" ) )
				.containsExactly( ElasticsearchDistributionName.OPENSEARCH, 1, 1, 0, null );
		assertComponents( ElasticsearchVersion.of( "opensearch:1.1.4" ) )
				.containsExactly( ElasticsearchDistributionName.OPENSEARCH, 1, 1, 4, null );
		assertComponents( ElasticsearchVersion.of( "opensearch:1.0.0-rc1" ) )
				.containsExactly( ElasticsearchDistributionName.OPENSEARCH, 1, 0, 0, "rc1" );

		assertComponents( ElasticsearchVersion.of( "amazon-opensearch-serverless" ) )
				.containsExactly( ElasticsearchDistributionName.AMAZON_OPENSEARCH_SERVERLESS, null, null, null, null );
	}

	@Test
	public void of_string_forceLowercase() {
		assertComponents( ElasticsearchVersion.of( "7.0.0-Beta1" ) )
				.containsExactly( ElasticsearchDistributionName.ELASTIC, 7, 0, 0, "beta1" );
		assertComponents( ElasticsearchVersion.of( "ELASTIC:7.0.0-Beta1" ) )
				.containsExactly( ElasticsearchDistributionName.ELASTIC, 7, 0, 0, "beta1" );
		assertComponents( ElasticsearchVersion.of( "OpenSearch:1.0.0-RC1" ) )
				.containsExactly( ElasticsearchDistributionName.OPENSEARCH, 1, 0, 0, "rc1" );
		assertComponents( ElasticsearchVersion.of( "Amazon-OpenSearch-Serverless" ) )
				.containsExactly( ElasticsearchDistributionName.AMAZON_OPENSEARCH_SERVERLESS, null, null, null, null );
	}

	@Test
	public void of_string_invalid() {
		assertThatThrownBy( () -> ElasticsearchVersion.of( "7_0" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid Elasticsearch version",
						"'7_0'",
						"Expected format is 'x.y.z-qualifier' or '<distribution>:x.y.z-qualifier'" );
		assertThatThrownBy( () -> ElasticsearchVersion.of( "elastic7" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid Elasticsearch version",
						"'elastic7'",
						"Expected format is 'x.y.z-qualifier' or '<distribution>:x.y.z-qualifier' or just '<distribution>'" );
		assertThatThrownBy( () -> ElasticsearchVersion.of( "opensearch;7.0" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid Elasticsearch version",
						"'opensearch;7.0'",
						"Expected format is 'x.y.z-qualifier' or '<distribution>:x.y.z-qualifier' or just '<distribution>'" );
		assertThatThrownBy( () -> ElasticsearchVersion.of( "elasticsearch:7.0" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid Elasticsearch version",
						"'elasticsearch:7.0'",
						"Expected format is 'x.y.z-qualifier' or '<distribution>:x.y.z-qualifier' or just '<distribution>'",
						"where '<distribution>' is one of [elastic, opensearch, amazon-opensearch-serverless] (defaults to 'elastic')" );
		assertThatThrownBy( () -> ElasticsearchVersion.of( "elastic:" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid Elasticsearch version",
						"'elastic:'",
						"Expected format is 'x.y.z-qualifier' or '<distribution>:x.y.z-qualifier' or just '<distribution>'" );
		assertThatThrownBy( () -> ElasticsearchVersion.of( "opensearch:" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid Elasticsearch version",
						"'opensearch:'",
						"Expected format is 'x.y.z-qualifier' or '<distribution>:x.y.z-qualifier' or just '<distribution>'" );
		assertThatThrownBy( () -> ElasticsearchVersion.of( "amazon-opensearch-service:" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid Elasticsearch version",
						"'amazon-opensearch-service:'",
						"Expected format is 'x.y.z-qualifier' or '<distribution>:x.y.z-qualifier'" );
	}

	@Test
	public void of_distributionNameAndString() {
		assertComponents( ElasticsearchVersion.of( ElasticsearchDistributionName.ELASTIC, "7" ) )
				.containsExactly( ElasticsearchDistributionName.ELASTIC, 7, null, null, null );
		assertComponents( ElasticsearchVersion.of( ElasticsearchDistributionName.ELASTIC, "7.0" ) )
				.containsExactly( ElasticsearchDistributionName.ELASTIC, 7, 0, null, null );
		assertComponents( ElasticsearchVersion.of( ElasticsearchDistributionName.ELASTIC, "7.0.0" ) )
				.containsExactly( ElasticsearchDistributionName.ELASTIC, 7, 0, 0, null );
		assertComponents( ElasticsearchVersion.of( ElasticsearchDistributionName.ELASTIC, "7.0.1" ) )
				.containsExactly( ElasticsearchDistributionName.ELASTIC, 7, 0, 1, null );
		assertComponents( ElasticsearchVersion.of( ElasticsearchDistributionName.ELASTIC, "7.1" ) )
				.containsExactly( ElasticsearchDistributionName.ELASTIC, 7, 1, null, null );
		assertComponents( ElasticsearchVersion.of( ElasticsearchDistributionName.ELASTIC, "7.1.0" ) )
				.containsExactly( ElasticsearchDistributionName.ELASTIC, 7, 1, 0, null );
		assertComponents( ElasticsearchVersion.of( ElasticsearchDistributionName.ELASTIC, "7.1.2" ) )
				.containsExactly( ElasticsearchDistributionName.ELASTIC, 7, 1, 2, null );
		assertComponents( ElasticsearchVersion.of( ElasticsearchDistributionName.ELASTIC, "7.0.0-beta1" ) )
				.containsExactly( ElasticsearchDistributionName.ELASTIC, 7, 0, 0, "beta1" );

		assertComponents( ElasticsearchVersion.of( ElasticsearchDistributionName.OPENSEARCH, "1" ) )
				.containsExactly( ElasticsearchDistributionName.OPENSEARCH, 1, null, null, null );
		assertComponents( ElasticsearchVersion.of( ElasticsearchDistributionName.OPENSEARCH, "1.0" ) )
				.containsExactly( ElasticsearchDistributionName.OPENSEARCH, 1, 0, null, null );
		assertComponents( ElasticsearchVersion.of( ElasticsearchDistributionName.OPENSEARCH, "1.0.0" ) )
				.containsExactly( ElasticsearchDistributionName.OPENSEARCH, 1, 0, 0, null );
		assertComponents( ElasticsearchVersion.of( ElasticsearchDistributionName.OPENSEARCH, "1.0.3" ) )
				.containsExactly( ElasticsearchDistributionName.OPENSEARCH, 1, 0, 3, null );
		assertComponents( ElasticsearchVersion.of( ElasticsearchDistributionName.OPENSEARCH, "1.1" ) )
				.containsExactly( ElasticsearchDistributionName.OPENSEARCH, 1, 1, null, null );
		assertComponents( ElasticsearchVersion.of( ElasticsearchDistributionName.OPENSEARCH, "1.1.0" ) )
				.containsExactly( ElasticsearchDistributionName.OPENSEARCH, 1, 1, 0, null );
		assertComponents( ElasticsearchVersion.of( ElasticsearchDistributionName.OPENSEARCH, "1.1.4" ) )
				.containsExactly( ElasticsearchDistributionName.OPENSEARCH, 1, 1, 4, null );
		assertComponents( ElasticsearchVersion.of( ElasticsearchDistributionName.OPENSEARCH, "1.0.0-rc1" ) )
				.containsExactly( ElasticsearchDistributionName.OPENSEARCH, 1, 0, 0, "rc1" );

		assertComponents( ElasticsearchVersion.of( ElasticsearchDistributionName.AMAZON_OPENSEARCH_SERVERLESS, null ) )
				.containsExactly( ElasticsearchDistributionName.AMAZON_OPENSEARCH_SERVERLESS, null, null, null, null );
	}

	@Test
	public void of_distributionNameAndString_invalid() {
		assertThatThrownBy( () -> ElasticsearchVersion.of( ElasticsearchDistributionName.ELASTIC, "7_0" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid Elasticsearch version",
						"'7_0'",
						"Expected format is 'x.y.z-qualifier'" );
		assertThatThrownBy( () -> ElasticsearchVersion.of( ElasticsearchDistributionName.OPENSEARCH, "1_0" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid Elasticsearch version",
						"'1_0'",
						"Expected format is 'x.y.z-qualifier'" );
		assertThatThrownBy( () -> ElasticsearchVersion.of( ElasticsearchDistributionName.OPENSEARCH, "opensearch:1.0" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid Elasticsearch version",
						"'opensearch:1.0'",
						"Expected format is 'x.y.z-qualifier'" );
	}

	@Test
	public void exactMatch_elasticsearch() {
		assertMatch( "5.6.0", ElasticsearchDistributionName.ELASTIC, "5.6.0" ).isTrue();
		assertMatch( "5.6.1", ElasticsearchDistributionName.ELASTIC, "5.6.1" ).isTrue();
		assertMatch( "6.0.0", ElasticsearchDistributionName.ELASTIC, "6.0.0" ).isTrue();
		assertMatch( "6.0.1", ElasticsearchDistributionName.ELASTIC, "6.0.1" ).isTrue();
		assertMatch( "6.6.0", ElasticsearchDistributionName.ELASTIC, "6.6.0" ).isTrue();
		assertMatch( "6.6.2", ElasticsearchDistributionName.ELASTIC, "6.6.2" ).isTrue();
		assertMatch( "6.7.0", ElasticsearchDistributionName.ELASTIC, "6.7.0" ).isTrue();
		assertMatch( "6.7.1", ElasticsearchDistributionName.ELASTIC, "6.7.1" ).isTrue();
		assertMatch( "7.0.0", ElasticsearchDistributionName.ELASTIC, "7.0.0" ).isTrue();
		assertMatch( "7.0.1", ElasticsearchDistributionName.ELASTIC, "7.0.1" ).isTrue();
		assertMatch( "7.0.0-beta1", ElasticsearchDistributionName.ELASTIC, "7.0.0-beta1" ).isTrue();
	}

	@Test
	public void exactMatch_opensearch() {
		assertMatch( "opensearch:1.0.0-beta1", ElasticsearchDistributionName.OPENSEARCH, "1.0.0-beta1" ).isTrue();
		assertMatch( "opensearch:1.0.0-rc1", ElasticsearchDistributionName.OPENSEARCH, "1.0.0-rc1" ).isTrue();
		assertMatch( "opensearch:1.0.0-rc2", ElasticsearchDistributionName.OPENSEARCH, "1.0.0-rc2" ).isTrue();
		assertMatch( "opensearch:1.0.0", ElasticsearchDistributionName.OPENSEARCH, "1.0.0" ).isTrue();
		assertMatch( "opensearch:1.0.1", ElasticsearchDistributionName.OPENSEARCH, "1.0.1" ).isTrue();
		assertMatch( "opensearch:1.1.0", ElasticsearchDistributionName.OPENSEARCH, "1.1.0" ).isTrue();
		assertMatch( "opensearch:1.1.1", ElasticsearchDistributionName.OPENSEARCH, "1.1.1" ).isTrue();
		assertMatch( "opensearch:2.0.0", ElasticsearchDistributionName.OPENSEARCH, "2.0.0" ).isTrue();
		assertMatch( "opensearch:2.0.1", ElasticsearchDistributionName.OPENSEARCH, "2.0.1" ).isTrue();
		assertMatch( "opensearch:2.1.0", ElasticsearchDistributionName.OPENSEARCH, "2.1.0" ).isTrue();
		assertMatch( "opensearch:2.1.1", ElasticsearchDistributionName.OPENSEARCH, "2.1.1" ).isTrue();
	}

	@Test
	public void exactMatch_amazonOpensearchServerless() {
		assertMatch( "amazon-opensearch-serverless", ElasticsearchDistributionName.AMAZON_OPENSEARCH_SERVERLESS, null ).isTrue();
	}

	@Test
	public void lenientMatch_elasticsearch() {
		assertMatch( "5", ElasticsearchDistributionName.ELASTIC, "5.6.0" ).isTrue();
		assertMatch( "5", ElasticsearchDistributionName.ELASTIC, "5.6.1" ).isTrue();
		assertMatch( "5", ElasticsearchDistributionName.ELASTIC, "5.7.1" ).isTrue();
		assertMatch( "5.6", ElasticsearchDistributionName.ELASTIC, "5.6.0" ).isTrue();
		assertMatch( "5.6", ElasticsearchDistributionName.ELASTIC, "5.6.1" ).isTrue();
		assertMatch( "6", ElasticsearchDistributionName.ELASTIC, "6.0.0" ).isTrue();
		assertMatch( "6", ElasticsearchDistributionName.ELASTIC, "6.0.1" ).isTrue();
		assertMatch( "6", ElasticsearchDistributionName.ELASTIC, "6.6.0" ).isTrue();
		assertMatch( "6", ElasticsearchDistributionName.ELASTIC, "6.6.1" ).isTrue();
		assertMatch( "6", ElasticsearchDistributionName.ELASTIC, "6.7.0" ).isTrue();
		assertMatch( "6", ElasticsearchDistributionName.ELASTIC, "6.7.1" ).isTrue();
		assertMatch( "6.0", ElasticsearchDistributionName.ELASTIC, "6.0.0" ).isTrue();
		assertMatch( "6.0", ElasticsearchDistributionName.ELASTIC, "6.0.1" ).isTrue();
		assertMatch( "6.6", ElasticsearchDistributionName.ELASTIC, "6.6.0" ).isTrue();
		assertMatch( "6.6", ElasticsearchDistributionName.ELASTIC, "6.6.1" ).isTrue();
		assertMatch( "6.7", ElasticsearchDistributionName.ELASTIC, "6.7.0" ).isTrue();
		assertMatch( "6.7", ElasticsearchDistributionName.ELASTIC, "6.7.1" ).isTrue();
		assertMatch( "7", ElasticsearchDistributionName.ELASTIC, "7.0.0" ).isTrue();
		assertMatch( "7", ElasticsearchDistributionName.ELASTIC, "7.0.1" ).isTrue();
		assertMatch( "7", ElasticsearchDistributionName.ELASTIC, "7.0.0-beta1" ).isTrue();
		assertMatch( "7.0", ElasticsearchDistributionName.ELASTIC, "7.0.0" ).isTrue();
		assertMatch( "7.0", ElasticsearchDistributionName.ELASTIC, "7.0.1" ).isTrue();
		assertMatch( "7.0", ElasticsearchDistributionName.ELASTIC, "7.0.0-beta1" ).isTrue();

		assertMatch( "elastic:7", ElasticsearchDistributionName.ELASTIC, "7.0.0" ).isTrue();
		assertMatch( "elastic:7", ElasticsearchDistributionName.ELASTIC, "7.0.1" ).isTrue();
		assertMatch( "elastic:7", ElasticsearchDistributionName.ELASTIC, "7.0.0-beta1" ).isTrue();
		assertMatch( "elastic:7.0", ElasticsearchDistributionName.ELASTIC, "7.0.0" ).isTrue();
		assertMatch( "elastic:7.0", ElasticsearchDistributionName.ELASTIC, "7.0.1" ).isTrue();
		assertMatch( "elastic:7.0", ElasticsearchDistributionName.ELASTIC, "7.0.0-beta1" ).isTrue();
	}

	@Test
	public void lenientMatch_opensearch() {
		assertMatch( "opensearch:1", ElasticsearchDistributionName.OPENSEARCH, "1.0.0-rc1" ).isTrue();
		assertMatch( "opensearch:1", ElasticsearchDistributionName.OPENSEARCH, "1.0.0" ).isTrue();
		assertMatch( "opensearch:1", ElasticsearchDistributionName.OPENSEARCH, "1.0.1" ).isTrue();
		assertMatch( "opensearch:1", ElasticsearchDistributionName.OPENSEARCH, "1.1.0" ).isTrue();
		assertMatch( "opensearch:1", ElasticsearchDistributionName.OPENSEARCH, "1.1.1" ).isTrue();
		assertMatch( "opensearch:1", ElasticsearchDistributionName.OPENSEARCH, "1.2.0" ).isTrue();
		assertMatch( "opensearch:1", ElasticsearchDistributionName.OPENSEARCH, "1.2.1" ).isTrue();
		assertMatch( "opensearch:1.0", ElasticsearchDistributionName.OPENSEARCH, "1.0.0" ).isTrue();
		assertMatch( "opensearch:1.0", ElasticsearchDistributionName.OPENSEARCH, "1.0.1" ).isTrue();
		assertMatch( "opensearch:1.1", ElasticsearchDistributionName.OPENSEARCH, "1.1.0" ).isTrue();
		assertMatch( "opensearch:1.1", ElasticsearchDistributionName.OPENSEARCH, "1.1.1" ).isTrue();
		assertMatch( "opensearch:1.2", ElasticsearchDistributionName.OPENSEARCH, "1.2.0" ).isTrue();
		assertMatch( "opensearch:1.2", ElasticsearchDistributionName.OPENSEARCH, "1.2.1" ).isTrue();
		assertMatch( "opensearch:2", ElasticsearchDistributionName.OPENSEARCH, "2.0.0" ).isTrue();
		assertMatch( "opensearch:2", ElasticsearchDistributionName.OPENSEARCH, "2.0.1" ).isTrue();
		assertMatch( "opensearch:2", ElasticsearchDistributionName.OPENSEARCH, "2.0.0-rc1" ).isTrue();
		assertMatch( "opensearch:2.0", ElasticsearchDistributionName.OPENSEARCH, "2.0.0" ).isTrue();
		assertMatch( "opensearch:2.0", ElasticsearchDistributionName.OPENSEARCH, "2.0.1" ).isTrue();
		assertMatch( "opensearch:2.0", ElasticsearchDistributionName.OPENSEARCH, "2.0.0-rc1" ).isTrue();
	}

	@Test
	public void nonMatching_elastic() {
		assertMatch( "5.6.0", ElasticsearchDistributionName.ELASTIC, "5.5.0" ).isFalse();
		assertMatch( "5.6.0", ElasticsearchDistributionName.ELASTIC, "5.7.0" ).isFalse();
		assertMatch( "5.6.0", ElasticsearchDistributionName.ELASTIC, "6.0.0" ).isFalse();
		assertMatch( "5.6", ElasticsearchDistributionName.ELASTIC, "6.0.0" ).isFalse();
		assertMatch( "5", ElasticsearchDistributionName.ELASTIC, "6.0.0" ).isFalse();
		assertMatch( "6.0.0", ElasticsearchDistributionName.ELASTIC, "5.6.0" ).isFalse();
		assertMatch( "6.7.0", ElasticsearchDistributionName.ELASTIC, "6.6.0" ).isFalse();
		assertMatch( "6.6.0", ElasticsearchDistributionName.ELASTIC, "6.7.0" ).isFalse();
		assertMatch( "6.0.0", ElasticsearchDistributionName.ELASTIC, "7.0.0" ).isFalse();
		assertMatch( "6.6.0", ElasticsearchDistributionName.ELASTIC, "7.0.0" ).isFalse();
		assertMatch( "6.0", ElasticsearchDistributionName.ELASTIC, "5.6.0" ).isFalse();
		assertMatch( "6.0", ElasticsearchDistributionName.ELASTIC, "7.0.0" ).isFalse();
		assertMatch( "6.0", ElasticsearchDistributionName.ELASTIC, "6.5.0" ).isFalse();
		assertMatch( "6.0", ElasticsearchDistributionName.ELASTIC, "6.6.0" ).isFalse();
		assertMatch( "6.6", ElasticsearchDistributionName.ELASTIC, "5.6.0" ).isFalse();
		assertMatch( "6.6", ElasticsearchDistributionName.ELASTIC, "7.0.0" ).isFalse();
		assertMatch( "6.6", ElasticsearchDistributionName.ELASTIC, "6.0.0" ).isFalse();
		assertMatch( "6.6", ElasticsearchDistributionName.ELASTIC, "6.5.0" ).isFalse();
		assertMatch( "6", ElasticsearchDistributionName.ELASTIC, "5.6.0" ).isFalse();
		assertMatch( "6", ElasticsearchDistributionName.ELASTIC, "7.0.0" ).isFalse();
		assertMatch( "7.0.0", ElasticsearchDistributionName.ELASTIC, "6.6.0" ).isFalse();
		assertMatch( "7.0.0", ElasticsearchDistributionName.ELASTIC, "5.6.0" ).isFalse();
		assertMatch( "7.0.0", ElasticsearchDistributionName.ELASTIC, "7.0.1" ).isFalse();
		assertMatch( "7.0.0", ElasticsearchDistributionName.ELASTIC, "7.1.0" ).isFalse();
		assertMatch( "7.0", ElasticsearchDistributionName.ELASTIC, "5.6.0" ).isFalse();
		assertMatch( "7.0", ElasticsearchDistributionName.ELASTIC, "6.0.0" ).isFalse();
		assertMatch( "7.0", ElasticsearchDistributionName.ELASTIC, "6.5.0" ).isFalse();
		assertMatch( "7.0", ElasticsearchDistributionName.ELASTIC, "6.6.0" ).isFalse();
		assertMatch( "7.0", ElasticsearchDistributionName.ELASTIC, "7.1.0" ).isFalse();
		assertMatch( "7", ElasticsearchDistributionName.ELASTIC, "5.6.0" ).isFalse();
		assertMatch( "7", ElasticsearchDistributionName.ELASTIC, "6.6.0" ).isFalse();
		assertMatch( "7.0.0", ElasticsearchDistributionName.ELASTIC, "6.6.0" ).isFalse();
		assertMatch( "7.0.0-beta1", ElasticsearchDistributionName.ELASTIC, "5.6.0" ).isFalse();
		assertMatch( "7.0.0-beta1", ElasticsearchDistributionName.ELASTIC, "6.0.0" ).isFalse();
		assertMatch( "7.0.0-beta1", ElasticsearchDistributionName.ELASTIC, "7.0.0" ).isFalse();
	}

	@Test
	public void nonMatching_opensearch() {
		assertMatch( "opensearch:1.0.0", ElasticsearchDistributionName.OPENSEARCH, "1.1.0" ).isFalse();
		assertMatch( "opensearch:1.0", ElasticsearchDistributionName.OPENSEARCH, "1.1.0" ).isFalse();
		assertMatch( "opensearch:1.1.0", ElasticsearchDistributionName.OPENSEARCH, "1.0.0" ).isFalse();
		assertMatch( "opensearch:1.1", ElasticsearchDistributionName.OPENSEARCH, "1.0.0" ).isFalse();
		assertMatch( "opensearch:1.0.0", ElasticsearchDistributionName.OPENSEARCH, "2.0.0" ).isFalse();
		assertMatch( "opensearch:1.0", ElasticsearchDistributionName.OPENSEARCH, "2.0.0" ).isFalse();
		assertMatch( "opensearch:1", ElasticsearchDistributionName.OPENSEARCH, "2.0.0" ).isFalse();
	}

	@Test
	public void nonMatching_elasticAndOpensearch() {
		assertMatch( "7.0.0", ElasticsearchDistributionName.OPENSEARCH, "7.0.0" ).isFalse();
		assertMatch( "7.0", ElasticsearchDistributionName.OPENSEARCH, "7.0.0" ).isFalse();
		assertMatch( "7", ElasticsearchDistributionName.OPENSEARCH, "7.0.0" ).isFalse();
		assertMatch( "7.0.0", ElasticsearchDistributionName.OPENSEARCH, "1.1.0" ).isFalse();
		assertMatch( "7.0", ElasticsearchDistributionName.OPENSEARCH, "1.1.0" ).isFalse();
		assertMatch( "7", ElasticsearchDistributionName.OPENSEARCH, "1.1.0" ).isFalse();

		assertMatch( "elastic:7.0.0", ElasticsearchDistributionName.OPENSEARCH, "7.0.0" ).isFalse();
		assertMatch( "elastic:7.0", ElasticsearchDistributionName.OPENSEARCH, "7.0.0" ).isFalse();
		assertMatch( "elastic:7", ElasticsearchDistributionName.OPENSEARCH, "7.0.0" ).isFalse();
		assertMatch( "elastic:7.0.0", ElasticsearchDistributionName.OPENSEARCH, "1.1.0" ).isFalse();
		assertMatch( "elastic:7.0", ElasticsearchDistributionName.OPENSEARCH, "1.1.0" ).isFalse();
		assertMatch( "elastic:7", ElasticsearchDistributionName.OPENSEARCH, "1.1.0" ).isFalse();

		assertMatch( "opensearch:1.0.0", ElasticsearchDistributionName.ELASTIC, "1.1.0" ).isFalse();
		assertMatch( "opensearch:1.0", ElasticsearchDistributionName.ELASTIC, "1.1.0" ).isFalse();
		assertMatch( "opensearch:1", ElasticsearchDistributionName.ELASTIC, "1.1.0" ).isFalse();
		assertMatch( "opensearch:1.0.0", ElasticsearchDistributionName.ELASTIC, "7.0.0" ).isFalse();
		assertMatch( "opensearch:1.0", ElasticsearchDistributionName.ELASTIC, "7.0.0" ).isFalse();
		assertMatch( "opensearch:1", ElasticsearchDistributionName.ELASTIC, "7.0.0" ).isFalse();
	}

	private ObjectArrayAssert<Object> assertComponents(ElasticsearchVersion version) {
		return assertThat( new Object[] {
				version.distribution(),
				toNullable( version.majorOptional() ),
				toNullable( version.minor() ),
				toNullable( version.micro() ),
				version.qualifier().orElse( null ) } );
	}

	private Integer toNullable(OptionalInt optional) {
		return optional.isPresent() ? optional.getAsInt() : null;
	}

	private AbstractBooleanAssert<?> assertMatch(String configuredVersionString,
			ElasticsearchDistributionName actualDistribution, String actualVersionString) {
		ElasticsearchVersion configuredVersion = ElasticsearchVersion.of( configuredVersionString );
		ElasticsearchVersion actualVersion = ElasticsearchVersion.of( actualDistribution, actualVersionString );

		return assertThat( configuredVersion.matches( actualVersion ) )
				.as( "ESVersion(" + configuredVersionString + ").matches( ESVersion("
						+ actualDistribution + "," + actualVersionString + ") )" );
	}
}
