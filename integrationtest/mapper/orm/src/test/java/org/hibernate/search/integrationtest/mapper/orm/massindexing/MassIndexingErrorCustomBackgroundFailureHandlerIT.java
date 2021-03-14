/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.massindexing;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubFailureHandler;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.junit.Rule;

public class MassIndexingErrorCustomBackgroundFailureHandlerIT extends AbstractMassIndexingErrorIT {

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
	protected void expectNoFailureHandling() {
		// We'll check in the assert*() method, see below.
	}

	@Override
	protected void assertNoFailureHandling() {
		assertThat( staticCounters.get( StubFailureHandler.CREATE ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_ENTITY_INDEXING_CONTEXT ) ).isEqualTo( 0 );
	}
}
