/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.dialect.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

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
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith( Parameterized.class )
public class ElasticsearchDialectFactoryTest {

	private enum ExpectedOutcome {
		UNSUPPORTED,
		AMBIGUOUS,
		SUCCESS_WITH_WARNING,
		SUCCESS
	}

	@Parameterized.Parameters(name = "{0} {1}/{2} => {3}")
	public static List<Object[]> params() {
		return Arrays.asList(
				unsupported( ElasticsearchDistributionName.ELASTIC, "0.90.12" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "0.90.12" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "1.0.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "2.0.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "2.4.4" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "5.0.0" ),
				unsupported( ElasticsearchDistributionName.ELASTIC, "5.2.0" ),
				ambiguous( ElasticsearchDistributionName.ELASTIC, "5" ),
				success(
						ElasticsearchDistributionName.ELASTIC, "5.6", "5.6.12",
						Elasticsearch56ModelDialect.class, Elasticsearch56ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "5.6.12", "5.6.12",
						Elasticsearch56ModelDialect.class, Elasticsearch56ProtocolDialect.class
				),
				successWithWarning(
						ElasticsearchDistributionName.ELASTIC, "5.7.0", "5.7.0",
						Elasticsearch56ModelDialect.class, Elasticsearch56ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "6", "6.0.0",
						Elasticsearch6ModelDialect.class, Elasticsearch60ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "6", "6.3.0",
						Elasticsearch6ModelDialect.class, Elasticsearch63ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "6", "6.7.0",
						Elasticsearch6ModelDialect.class, Elasticsearch67ProtocolDialect.class
				),

				success(
						ElasticsearchDistributionName.ELASTIC, "6.0", "6.0.0",
						Elasticsearch6ModelDialect.class, Elasticsearch60ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "6.0.0", "6.0.0",
						Elasticsearch6ModelDialect.class, Elasticsearch60ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "6.3", "6.3.0",
						Elasticsearch6ModelDialect.class, Elasticsearch63ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "6.3.0", "6.3.0",
						Elasticsearch6ModelDialect.class, Elasticsearch63ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "6.4", "6.4.0",
						Elasticsearch6ModelDialect.class, Elasticsearch64ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "6.4.0", "6.4.0",
						Elasticsearch6ModelDialect.class, Elasticsearch64ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "6.6", "6.6.0",
						Elasticsearch6ModelDialect.class, Elasticsearch64ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "6.6.0", "6.6.0",
						Elasticsearch6ModelDialect.class, Elasticsearch64ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "6.7", "6.7.0",
						Elasticsearch6ModelDialect.class, Elasticsearch67ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "6.7.0", "6.7.0",
						Elasticsearch6ModelDialect.class, Elasticsearch67ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "6.8.0", "6.8.0",
						Elasticsearch6ModelDialect.class, Elasticsearch67ProtocolDialect.class
				),
				successWithWarning(
						ElasticsearchDistributionName.ELASTIC, "6.9.0", "6.9.0",
						Elasticsearch6ModelDialect.class, Elasticsearch67ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.0.0-beta1", "7.0.0-beta1",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7", "7.16.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.0", "7.0.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.0.0", "7.0.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.1", "7.1.1",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.1.1", "7.1.1",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.2", "7.2.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.2.0", "7.2.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.3", "7.3.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.3.0", "7.3.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.4", "7.4.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.4.0", "7.4.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.5", "7.5.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.5.0", "7.5.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.6", "7.6.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.6.0", "7.6.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.6.2", "7.6.2",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.7", "7.7.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.7.0", "7.7.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.8", "7.8.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.8.0", "7.8.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.9", "7.9.2",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.9.0", "7.9.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.ELASTIC, "7.9.2", "7.9.2",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
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
						ElasticsearchDistributionName.ELASTIC, "8", "8.6.0",
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
				successWithWarning(
						ElasticsearchDistributionName.ELASTIC, "8.7", "8.7.0",
						Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				successWithWarning(
						ElasticsearchDistributionName.ELASTIC, "8.7.0", "8.7.0",
						Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				successWithWarning(
						ElasticsearchDistributionName.ELASTIC, "9.0.0", "9.0.0",
						Elasticsearch8ModelDialect.class, Elasticsearch81ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "1", "1.2.1",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "1.0", "1.0.0-rc1",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "1.0.0-rc1", "1.0.0-rc1",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "1.0.0", "1.0.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "1.0.1", "1.0.1",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "1.2", "1.2.1",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "1.2.0", "1.2.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "1.2.1", "1.2.1",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "1.3", "1.3.1",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "1.3.0", "1.3.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "1.3.1", "1.3.1",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				successWithWarning(
						ElasticsearchDistributionName.OPENSEARCH, "1.4", "1.4.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				successWithWarning(
						ElasticsearchDistributionName.OPENSEARCH, "1.4.0", "1.4.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2", "2.3.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.0", "2.3.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.0.0", "2.0.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.1.0", "2.1.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.2.1", "2.2.1",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.3.0", "2.3.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.4.0", "2.4.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				success(
						ElasticsearchDistributionName.OPENSEARCH, "2.5.0", "2.5.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				successWithWarning(
						ElasticsearchDistributionName.OPENSEARCH, "2.6", "2.6.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				successWithWarning(
						ElasticsearchDistributionName.OPENSEARCH, "2.6.0", "2.6.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				successWithWarning(
						ElasticsearchDistributionName.OPENSEARCH, "3", "3.0.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				successWithWarning(
						ElasticsearchDistributionName.OPENSEARCH, "3.0", "3.0.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				),
				successWithWarning(
						ElasticsearchDistributionName.OPENSEARCH, "3.0.0", "3.0.0",
						Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
				)
		);
	}

	private static Object[] unsupported(ElasticsearchDistributionName distribution, String configuredVersionString) {
		return new Object[] {
				distribution, configuredVersionString, null, ExpectedOutcome.UNSUPPORTED, null, null
		};
	}

	private static Object[] ambiguous(ElasticsearchDistributionName distribution, String configuredVersionString) {
		return new Object[] {
				distribution, configuredVersionString, null, ExpectedOutcome.AMBIGUOUS, null, null
		};
	}

	private static Object[] successWithWarning(ElasticsearchDistributionName distribution,
			String configuredVersionString, String actualVersionString,
			Class<? extends ElasticsearchModelDialect> expectedModelDialectClass,
			Class<? extends ElasticsearchProtocolDialect> expectedProtocolDialectClass) {
		return new Object[] {
				distribution, configuredVersionString, actualVersionString,
				ExpectedOutcome.SUCCESS_WITH_WARNING, expectedModelDialectClass, expectedProtocolDialectClass
		};
	}

	private static Object[] success(ElasticsearchDistributionName distribution,
			String configuredVersionString, String actualVersionString,
			Class<? extends ElasticsearchModelDialect> expectedModelDialectClass,
			Class<? extends ElasticsearchProtocolDialect> expectedProtocolDialectClass) {
		return new Object[] {
				distribution, configuredVersionString, actualVersionString,
				ExpectedOutcome.SUCCESS, expectedModelDialectClass, expectedProtocolDialectClass
		};
	}

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	private final ElasticsearchDialectFactory dialectFactory = new ElasticsearchDialectFactory();

	@Parameterized.Parameter
	public ElasticsearchDistributionName distributionName;
	@Parameterized.Parameter(1)
	public String configuredVersionString;
	@Parameterized.Parameter(2)
	public String actualVersionString;
	@Parameterized.Parameter(3)
	public ExpectedOutcome expectedOutcome;
	@Parameterized.Parameter(4)
	public Class<? extends ElasticsearchModelDialect> expectedModelDialectClass;
	@Parameterized.Parameter(5)
	public Class<? extends ElasticsearchProtocolDialect> expectedProtocolDialectClass;

	@Test
	public void test() {
		switch ( expectedOutcome ) {
			case UNSUPPORTED:
				testUnsupported();
				break;
			case AMBIGUOUS:
				testAmbiguous();
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

	private void testAmbiguous() {
		assertThatThrownBy(
				() -> {
					dialectFactory.createModelDialect( ElasticsearchVersion.of( distributionName, configuredVersionString ) );
				},
				"Test ambiguous version " + configuredVersionString
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "HSEARCH400561" )
				.hasMessageContaining( "Ambiguous Elasticsearch version: '" + distributionName + ":" + configuredVersionString + "'." )
				.hasMessageContaining( "Please use a more precise version to remove the ambiguity" );
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
