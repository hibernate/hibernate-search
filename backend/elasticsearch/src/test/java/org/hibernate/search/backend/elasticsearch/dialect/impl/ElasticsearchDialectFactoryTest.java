/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.dialect.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.backend.elasticsearch.ElasticsearchDistributionName;
import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.Elasticsearch56ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.Elasticsearch6ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.Elasticsearch7ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.Elasticsearch8ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.ElasticsearchModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch56ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch60ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch63ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch64ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch67ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch70ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch80ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch81ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.ElasticsearchProtocolDialect;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Rule;
import org.junit.Test;

public class ElasticsearchDialectFactoryTest {

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	private final ElasticsearchDialectFactory dialectFactory = new ElasticsearchDialectFactory();

	@Test
	public void elastic_0_90_12() {
		testUnsupported( ElasticsearchDistributionName.ELASTIC, "0.90.12" );
	}

	@Test
	public void elastic_1_0_0() {
		testUnsupported( ElasticsearchDistributionName.ELASTIC, "1.0.0" );
	}

	@Test
	public void elastic_2_0_0() {
		testUnsupported( ElasticsearchDistributionName.ELASTIC, "2.0.0" );
	}

	@Test
	public void elastic_2_4_4() {
		testUnsupported( ElasticsearchDistributionName.ELASTIC, "2.4.4" );
	}

	@Test
	public void elastic_5_0_0() {
		testUnsupported( ElasticsearchDistributionName.ELASTIC, "5.0.0" );
	}

	@Test
	public void elastic_5_2_0() {
		testUnsupported( ElasticsearchDistributionName.ELASTIC, "5.2.0" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3563")
	public void elastic_5() {
		testAmbiguous( ElasticsearchDistributionName.ELASTIC, "5" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3563")
	public void elastic_5_6() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "5.6", "5.6.12",
				Elasticsearch56ModelDialect.class, Elasticsearch56ProtocolDialect.class
		);
	}

	@Test
	public void elastic_5_6_12() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "5.6.12", "5.6.12",
				Elasticsearch56ModelDialect.class, Elasticsearch56ProtocolDialect.class
		);
	}

	@Test
	public void elastic_5_7_0() {
		testSuccessWithWarning(
				ElasticsearchDistributionName.ELASTIC, "5.7.0", "5.7.0",
				Elasticsearch56ModelDialect.class, Elasticsearch56ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3563")
	public void elastic_6() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "6", "6.0.0",
				Elasticsearch6ModelDialect.class, Elasticsearch60ProtocolDialect.class
		);
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "6", "6.3.0",
				Elasticsearch6ModelDialect.class, Elasticsearch63ProtocolDialect.class
		);
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "6", "6.7.0",
				Elasticsearch6ModelDialect.class, Elasticsearch67ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3563")
	public void elastic_6_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "6.0", "6.0.0",
				Elasticsearch6ModelDialect.class, Elasticsearch60ProtocolDialect.class
		);
	}

	@Test
	public void elastic_6_0_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "6.0.0", "6.0.0",
				Elasticsearch6ModelDialect.class, Elasticsearch60ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3352")
	public void elastic_6_3() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "6.3", "6.3.0",
				Elasticsearch6ModelDialect.class, Elasticsearch63ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3352")
	public void elastic_6_3_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "6.3.0", "6.3.0",
				Elasticsearch6ModelDialect.class, Elasticsearch63ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3791")
	public void elastic_6_4() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "6.4", "6.4.0",
				Elasticsearch6ModelDialect.class, Elasticsearch64ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3791")
	public void elastic_6_4_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "6.4.0", "6.4.0",
				Elasticsearch6ModelDialect.class, Elasticsearch64ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3563")
	public void elastic_6_6() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "6.6", "6.6.0",
				Elasticsearch6ModelDialect.class, Elasticsearch64ProtocolDialect.class
		);
	}

	@Test
	public void elastic_6_6_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "6.6.0", "6.6.0",
				Elasticsearch6ModelDialect.class, Elasticsearch64ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3563")
	public void elastic_6_7() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "6.7", "6.7.0",
				Elasticsearch6ModelDialect.class, Elasticsearch67ProtocolDialect.class
		);
	}

	@Test
	public void elastic_6_7_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "6.7.0", "6.7.0",
				Elasticsearch6ModelDialect.class, Elasticsearch67ProtocolDialect.class
		);
	}

	@Test
	public void elastic_6_8_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "6.8.0", "6.8.0",
				Elasticsearch6ModelDialect.class, Elasticsearch67ProtocolDialect.class
		);
	}

	@Test
	public void elastic_6_9_0() {
		testSuccessWithWarning(
				ElasticsearchDistributionName.ELASTIC, "6.9.0", "6.9.0",
				Elasticsearch6ModelDialect.class, Elasticsearch67ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3490")
	public void elastic_7_0_0_beta1() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.0.0-beta1", "7.0.0-beta1",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3563")
	public void elastic_7() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7", "7.16.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3563")
	public void elastic_7_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.0", "7.0.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3490")
	public void elastic_7_0_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.0.0", "7.0.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3592")
	public void elastic_7_1() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.1", "7.1.1",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3592")
	public void elastic_7_1_1() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.1.1", "7.1.1",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3625")
	public void elastic_7_2() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.2", "7.2.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3625")
	public void elastic_7_2_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.2.0", "7.2.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3660")
	public void elastic_7_3() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.3", "7.3.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3660")
	public void elastic_7_3_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.3.0", "7.3.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3723")
	public void elastic_7_4() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.4", "7.4.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3723")
	public void elastic_7_4_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.4.0", "7.4.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3782")
	public void elastic_7_5() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.5", "7.5.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3782")
	public void elastic_7_5_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.5.0", "7.5.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3829")
	public void elastic_7_6() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.6", "7.6.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3829")
	public void elastic_7_6_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.6.0", "7.6.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3918")
	public void elastic_7_6_2() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.6.2", "7.6.2",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3918")
	public void elastic_7_7() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.7", "7.7.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3918")
	public void elastic_7_7_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.7.0", "7.7.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3956")
	public void elastic_7_8() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.8", "7.8.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3956")
	public void elastic_7_8_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.8.0", "7.8.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3984")
	public void elastic_7_9() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.9", "7.9.2",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3984")
	public void elastic_7_9_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.9.0", "7.9.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4040")
	public void elastic_7_9_2() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.9.2", "7.9.2",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4104")
	public void elastic_7_1_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.10", "7.10.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4104")
	public void elastic_7_10_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.10.0", "7.10.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4170")
	public void elastic_7_11() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.11", "7.11.1",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4170")
	public void elastic_7_11_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.11.0", "7.11.00",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4170")
	public void elastic_7_11_1() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.11.1", "7.11.1",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4201")
	public void elastic_7_12() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.12", "7.12.1",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4201")
	public void elastic_7_12_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.12.0", "7.12.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4201")
	public void elastic_7_12_1() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.12.1", "7.12.1",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4235")
	public void elastic_7_13() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.13", "7.13.2",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4235")
	public void elastic_7_13_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.13.0", "7.13.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4235")
	public void elastic_7_13_2() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.13.2", "7.13.2",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4340")
	public void elastic_7_16() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.16", "7.16.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4340")
	public void elastic_7_16_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.16.0", "7.16.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4473")
	public void elastic_7_17() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.17", "7.17.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4473")
	public void elastic_7_17_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "7.17.0", "7.17.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	public void elastic_7_18_0() {
		testSuccessWithWarning(
				ElasticsearchDistributionName.ELASTIC, "7.18.0", "7.18.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4475")
	public void elastic_8() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "8", "8.2.0",
				Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4475")
	public void elastic_8_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "8.0", "8.0.0",
				Elasticsearch8ModelDialect.class, Elasticsearch80ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4475")
	public void elastic_8_0_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "8.0.0", "8.0.0",
				Elasticsearch8ModelDialect.class, Elasticsearch80ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4505")
	public void elastic_8_1() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "8.1", "8.1.0",
				Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4505")
	public void elastic_8_1_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "8.1.0", "8.1.0",
				Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4559")
	public void elastic_8_2() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "8.2", "8.2.0",
				Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4559")
	public void elastic_8_2_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "8.2.0", "8.2.0",
				Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4623")
	public void elastic_8_3() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "8.3", "8.3.0",
				Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4623")
	public void elastic_8_3_0() {
		testSuccess(
				ElasticsearchDistributionName.ELASTIC, "8.3.0", "8.3.0",
				Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4623")
	public void elastic_8_4() {
		testSuccessWithWarning(
				ElasticsearchDistributionName.ELASTIC, "8.4", "8.4.0",
				Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4623")
	public void elastic_8_4_0() {
		testSuccessWithWarning(
				ElasticsearchDistributionName.ELASTIC, "8.4.0", "8.4.0",
				Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
		);
	}

	@Test
	public void elastic_9_0_0() {
		testSuccessWithWarning(
				ElasticsearchDistributionName.ELASTIC, "9.0.0", "9.0.0",
				Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4212")
	public void openSearch_1() {
		testSuccess(
				ElasticsearchDistributionName.OPENSEARCH, "1", "1.2.1",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4212")
	public void openSearch_1_0() {
		testSuccess(
				ElasticsearchDistributionName.OPENSEARCH, "1.0", "1.0.0-rc1",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4212")
	public void openSearch_1_0_0_rc1() {
		testSuccess(
				ElasticsearchDistributionName.OPENSEARCH, "1.0.0-rc1", "1.0.0-rc1",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4212")
	public void openSearch_1_0_0() {
		testSuccess(
				ElasticsearchDistributionName.OPENSEARCH, "1.0.0", "1.0.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4212")
	public void openSearch_1_0_1() {
		testSuccess(
				ElasticsearchDistributionName.OPENSEARCH, "1.0.1", "1.0.1",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4412")
	public void openSearch_1_2() {
		testSuccess(
				ElasticsearchDistributionName.OPENSEARCH, "1.2", "1.2.1",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4412")
	public void openSearch_1_2_0() {
		testSuccess(
				ElasticsearchDistributionName.OPENSEARCH, "1.2.0", "1.2.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4412")
	public void openSearch_1_2_1() {
		testSuccess(
				ElasticsearchDistributionName.OPENSEARCH, "1.2.1", "1.2.1",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4561")
	public void openSearch_1_3() {
		testSuccess(
				ElasticsearchDistributionName.OPENSEARCH, "1.3", "1.3.1",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4561")
	public void openSearch_1_3_0() {
		testSuccess(
				ElasticsearchDistributionName.OPENSEARCH, "1.3.0", "1.3.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4561")
	public void openSearch_1_3_1() {
		testSuccess(
				ElasticsearchDistributionName.OPENSEARCH, "1.3.1", "1.3.1",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4561")
	public void openSearch_1_4() {
		testSuccessWithWarning(
				ElasticsearchDistributionName.OPENSEARCH, "1.4", "1.4.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4561")
	public void openSearch_1_4_0() {
		testSuccessWithWarning(
				ElasticsearchDistributionName.OPENSEARCH, "1.4.0", "1.4.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4562")
	public void openSearch_2() {
		testSuccess(
				ElasticsearchDistributionName.OPENSEARCH, "2", "2.0.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4562")
	public void openSearch_2_0() {
		testSuccess(
				ElasticsearchDistributionName.OPENSEARCH, "2.0", "2.0.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4562")
	public void openSearch_2_0_0() {
		testSuccess(
				ElasticsearchDistributionName.OPENSEARCH, "2.0.0", "2.0.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4562")
	public void openSearch_3() {
		testSuccessWithWarning(
				ElasticsearchDistributionName.OPENSEARCH, "3", "3.0.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4562")
	public void openSearch_3_0() {
		testSuccessWithWarning(
				ElasticsearchDistributionName.OPENSEARCH, "3.0", "3.0.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4562")
	public void openSearch_3_0_0() {
		testSuccessWithWarning(
				ElasticsearchDistributionName.OPENSEARCH, "3.0.0", "3.0.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	private void testUnsupported(ElasticsearchDistributionName distributionName,
			String unsupportedVersionString) {
		assertThatThrownBy(
				() -> dialectFactory.createModelDialect(
						ElasticsearchVersion.of( distributionName, unsupportedVersionString ) ),
				"Test unsupported version " + unsupportedVersionString
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "HSEARCH400081" )
				.hasMessageContaining( "'" + distributionName.toString() + ":" + unsupportedVersionString + "'" );
	}

	private void testAmbiguous(ElasticsearchDistributionName distributionName, String versionString) {
		assertThatThrownBy(
				() -> {
					dialectFactory.createModelDialect( ElasticsearchVersion.of( distributionName, versionString ) );
				},
				"Test ambiguous version " + versionString
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "HSEARCH400561" )
				.hasMessageContaining( "Ambiguous Elasticsearch version: '" + distributionName + ":" + versionString + "'." )
				.hasMessageContaining( "Please use a more precise version to remove the ambiguity" );
	}

	private void testSuccessWithWarning(ElasticsearchDistributionName distributionName,
			String configuredVersionString, String actualVersionString,
			Class<? extends ElasticsearchModelDialect> expectedModelDialectClass,
			Class<? extends ElasticsearchProtocolDialect> expectedProtocolDialectClass) {
		ElasticsearchVersion parsedConfiguredVersion = ElasticsearchVersion.of( distributionName, configuredVersionString );
		ElasticsearchVersion parsedActualVersion = ElasticsearchVersion.of( distributionName, actualVersionString );

		logged.expectMessage( "HSEARCH400085", "'" + parsedActualVersion + "'" );

		ElasticsearchModelDialect modelDialect = dialectFactory.createModelDialect( parsedConfiguredVersion );
		assertThat( modelDialect ).isInstanceOf( expectedModelDialectClass );

		ElasticsearchProtocolDialect protocolDialect = dialectFactory.createProtocolDialect( parsedActualVersion );
		assertThat( protocolDialect ).isInstanceOf( expectedProtocolDialectClass );
	}

	private void testSuccess(ElasticsearchDistributionName distributionName,
			String configuredVersionString, String actualVersionString,
			Class<? extends ElasticsearchModelDialect> expectedModelDialectClass,
			Class<? extends ElasticsearchProtocolDialect> expectedProtocolDialectClass) {
		ElasticsearchVersion parsedConfiguredVersion = ElasticsearchVersion.of( distributionName, configuredVersionString );
		ElasticsearchVersion parsedActualVersion = ElasticsearchVersion.of( distributionName, actualVersionString );

		logged.expectMessage( "HSEARCH400085" ).never();

		ElasticsearchModelDialect modelDialect = dialectFactory.createModelDialect( parsedConfiguredVersion );
		assertThat( modelDialect ).isInstanceOf( expectedModelDialectClass );

		ElasticsearchProtocolDialect protocolDialect = dialectFactory.createProtocolDialect( parsedActualVersion );
		assertThat( protocolDialect ).isInstanceOf( expectedProtocolDialectClass );
	}

}
