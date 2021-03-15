/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.massindexing;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubFailureHandler;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.junit.Rule;

public class MassIndexingFailureCustomBackgroundFailureHandlerIT extends AbstractMassIndexingFailureIT {

	@Rule
	public StaticCounters staticCounters = new StaticCounters();

	@Override
	protected String getBackgroundFailureHandlerReference() {
		return StubFailureHandler.class.getName();
	}

	@Override
	protected MassIndexingFailureHandler getMassIndexingFailureHandler() {
		return null;
	}

	@Override
	protected void assertBeforeSetup() {
		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_ENTITY_INDEXING_CONTEXT ) ).isEqualTo( 0 );
	}

	@Override
	protected void assertAfterSetup() {
		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_ENTITY_INDEXING_CONTEXT ) ).isEqualTo( 0 );
	}

	@Override
	protected void expectEntityIndexingFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString) {
		// We'll check in the assert*() method, see below.
	}

	@Override
	protected void assertEntityIndexingFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString) {
		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_ENTITY_INDEXING_CONTEXT ) ).isEqualTo( 1 );
	}

	@Override
	protected void expectEntityIdGetterFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString) {
		// We'll check in the assert*() method, see below.
	}

	@Override
	protected void assertEntityIdGetterFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString) {
		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_ENTITY_INDEXING_CONTEXT ) ).isEqualTo( 1 );
	}

	@Override
	protected void expectEntityNonIdGetterFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString) {
		// We'll check in the assert*() method, see below.
	}

	@Override
	protected void assertEntityNonIdGetterFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString) {
		// Same expectations as for the ID getter
		assertEntityIdGetterFailureHandling( entityName, entityReferenceAsString, exceptionMessage,
				failingOperationAsString );
	}

	@Override
	protected void expectMassIndexerOperationFailureHandling(
			Class<? extends Throwable> exceptionType, String exceptionMessage,
			String failingOperationAsString) {
		// We'll check in the assert*() method, see below.
	}

	@Override
	protected void assertMassIndexerOperationFailureHandling(
			Class<? extends Throwable> exceptionType, String exceptionMessage,
			String failingOperationAsString) {
		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 1 );
	}

	@Override
	protected void expectEntityIndexingAndMassIndexerOperationFailureHandling(String entityName,
			String entityReferenceAsString,
			String failingEntityIndexingExceptionMessage, String failingEntityIndexingOperationAsString,
			String failingMassIndexerOperationExceptionMessage, String failingMassIndexerOperationAsString) {
		// We'll check in the assert*() method, see below.
	}

	@Override
	protected void assertEntityIndexingAndMassIndexerOperationFailureHandling(String entityName,
			String entityReferenceAsString,
			String failingEntityIndexingExceptionMessage, String failingEntityIndexingOperationAsString,
			String failingMassIndexerOperationExceptionMessage, String failingMassIndexerOperationAsString) {
		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_ENTITY_INDEXING_CONTEXT ) ).isEqualTo( 1 );
	}
}
