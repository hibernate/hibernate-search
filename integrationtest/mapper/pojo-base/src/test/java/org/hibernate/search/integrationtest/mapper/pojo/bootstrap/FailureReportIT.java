/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.bootstrap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.StandalonePojoMappingSetupHelper;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.ExceptionMatcherBuilder;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Rule;
import org.junit.Test;

import org.apache.logging.log4j.Level;

public class FailureReportIT {

	private static final String FAILURE_LOG_INTRODUCTION = "Hibernate Search encountered a failure during bootstrap;"
			+ " continuing for now to list all problems,"
			+ " but the process will ultimately be aborted.\n"
			+ "Context: ";
	private static final String FAILURE_REPORT_INTRODUCTION = "HSEARCH000520: Hibernate Search encountered failures during bootstrap."
			+ " Failures:\n"
			+ "\n"
			+ "    Standalone POJO mapping: \n";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	/**
	 * Test mapping with failures in the same context
	 * and check that every failure is reported, and that failures are grouped into a single list.
	 */
	@Test
	public void multipleFailuresSameContext() {
		final String indexName = "indexName";
		@Indexed(index = indexName)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField(name = "failingField1")
			@GenericField(name = "failingField2")
			Integer myProperty;
		}
		String field1FailureMessage = "This is the failure message for field 1";
		String field2FailureMessage = "This is the failure message for field 2";

		backendMock.expectFailingField(
				indexName, "failingField1",
				() -> new SearchException( field1FailureMessage )
		);
		backendMock.expectFailingField(
				indexName, "failingField2",
				() -> new SearchException( field2FailureMessage )
		);

		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SearchException.class )
						.withMessage( field1FailureMessage ).build(),
				FAILURE_LOG_INTRODUCTION
						+ "Standalone POJO mapping, type '" + IndexedEntity.class.getName() + "', path '.myProperty'\n"
		);
		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SearchException.class )
						.withMessage( field2FailureMessage ).build(),
				FAILURE_LOG_INTRODUCTION
						+ "Standalone POJO mapping, type '" + IndexedEntity.class.getName() + "', path '.myProperty'\n"
		);

		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessage(
						FAILURE_REPORT_INTRODUCTION
								+ "        type '" + IndexedEntity.class.getName() + "': \n"
								+ "            path '.myProperty': \n"
								+ "                failures: \n"
								+ "                  - " + field1FailureMessage + "\n"
								+ "                  - " + field2FailureMessage
				);
	}

	/**
	 * Test mapping with failures in multiple properties of the same type
	 * and check that every failure is reported.
	 */
	@Test
	public void multipleFailuresMultipleProperties() {
		final String indexName = "indexName";
		@Indexed(index = indexName)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField(name = "failingField1")
			Integer myProperty1;
			@GenericField(name = "failingField2")
			Integer myProperty2;
		}
		String field1FailureMessage = "This is the failure message for field 1";
		String field2FailureMessage = "This is the failure message for field 2";

		backendMock.expectFailingField(
				indexName, "failingField1",
				() -> new SearchException( field1FailureMessage )
		);
		backendMock.expectFailingField(
				indexName, "failingField2",
				() -> new SearchException( field2FailureMessage )
		);

		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SearchException.class )
						.withMessage( field1FailureMessage ).build(),
				FAILURE_LOG_INTRODUCTION
						+ "Standalone POJO mapping, type '" + IndexedEntity.class.getName() + "', path '.myProperty1'\n"
		);
		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SearchException.class )
						.withMessage( field2FailureMessage ).build(),
				FAILURE_LOG_INTRODUCTION
						+ "Standalone POJO mapping, type '" + IndexedEntity.class.getName() + "', path '.myProperty2'\n"
		);

		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessage(
						FAILURE_REPORT_INTRODUCTION
								+ "        type '" + IndexedEntity.class.getName() + "': \n"
								+ "            path '.myProperty1': \n"
								+ "                failures: \n"
								+ "                  - " + field1FailureMessage + "\n"
								+ "            path '.myProperty2': \n"
								+ "                failures: \n"
								+ "                  - " + field2FailureMessage
				);
	}

	/**
	 * Test mapping with failures in multiple types
	 * and check that every failure is reported.
	 */
	@Test
	public void multipleFailuresMultipleTypes() {
		final String indexName1 = "indexName1";
		@Indexed(index = indexName1)
		class IndexedEntity1 {
			@DocumentId
			Integer id;
			@GenericField(name = "failingField1")
			Integer myProperty1;
		}
		final String indexName2 = "indexName2";
		@Indexed(index = indexName2)
		class IndexedEntity2 {
			@DocumentId
			Integer id;
			@GenericField(name = "failingField2")
			Integer myProperty2;
		}
		String field1FailureMessage = "This is the failure message for field 1";
		String field2FailureMessage = "This is the failure message for field 2";

		backendMock.expectFailingField(
				indexName1, "failingField1",
				() -> new SearchException( field1FailureMessage )
		);
		backendMock.expectFailingField(
				indexName2, "failingField2",
				() -> new SearchException( field2FailureMessage )
		);

		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SearchException.class )
						.withMessage( field1FailureMessage ).build(),
				FAILURE_LOG_INTRODUCTION
						+ "Standalone POJO mapping, type '" + IndexedEntity1.class.getName() + "', path '.myProperty1'\n"
		);
		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SearchException.class )
						.withMessage( field2FailureMessage ).build(),
				FAILURE_LOG_INTRODUCTION
						+ "Standalone POJO mapping, type '" + IndexedEntity2.class.getName() + "', path '.myProperty2'\n"
		);

		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity1.class, IndexedEntity2.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessage(
						FAILURE_REPORT_INTRODUCTION
								+ "        type '" + IndexedEntity1.class.getName() + "': \n"
								+ "            path '.myProperty1': \n"
								+ "                failures: \n"
								+ "                  - " + field1FailureMessage + "\n"
								+ "        type '" + IndexedEntity2.class.getName() + "': \n"
								+ "            path '.myProperty2': \n"
								+ "                failures: \n"
								+ "                  - " + field2FailureMessage
				);
	}

	@Test
	public void failuresFromBackend() {
		final String indexName = "indexName";
		@Indexed(index = indexName)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField(name = "failingField1")
			@GenericField(name = "failingField2")
			Integer myProperty;
		}
		String field1FailureMessage = "This is the failure message for field 1";
		String field2FailureMessage = "This is the failure message for field 2";

		backendMock.expectFailingField(
				indexName, "failingField1",
				() -> new SearchException(
						field1FailureMessage,
						EventContexts.fromIndexName( indexName )
								.append( EventContexts.fromIndexFieldAbsolutePath( "failingField1" ) )
				)
		);
		backendMock.expectFailingField(
				indexName, "failingField2", () -> new SearchException(
						field2FailureMessage,
						EventContexts.fromIndexName( indexName )
								.append( EventContexts.fromIndexFieldAbsolutePath( "failingField2" ) )
				)
		);

		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SearchException.class )
						.withMessage( field1FailureMessage ).build(),
				FAILURE_LOG_INTRODUCTION
						+ "Standalone POJO mapping, type '" + IndexedEntity.class.getName() + "', path '.myProperty',"
						+ " index '" + indexName + "', field 'failingField1'\n"
		);
		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SearchException.class )
						.withMessage( field2FailureMessage ).build(),
				FAILURE_LOG_INTRODUCTION
						+ "Standalone POJO mapping, type '" + IndexedEntity.class.getName() + "', path '.myProperty',"
						+ " index '" + indexName + "', field 'failingField2'\n"
		);

		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessage(
						FAILURE_REPORT_INTRODUCTION
								+ "        type '" + IndexedEntity.class.getName() + "': \n"
								+ "            path '.myProperty': \n"
								+ "                index '" + indexName + "': \n"
								+ "                    field 'failingField1': \n"
								+ "                        failures: \n"
								+ "                          - " + field1FailureMessage + "\n"
								+ "                    field 'failingField2': \n"
								+ "                        failures: \n"
								+ "                          - " + field2FailureMessage
				);
	}

}
