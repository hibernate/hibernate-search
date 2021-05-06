/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.massindexing;

import static org.mockito.Mockito.verifyNoInteractions;

import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;

import org.junit.Rule;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

public class MassIndexingErrorCustomBackgroundFailureHandlerIT extends AbstractMassIndexingErrorIT {

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	@Mock
	private FailureHandler failureHandler;

	@Override
	protected FailureHandler getBackgroundFailureHandlerReference() {
		return failureHandler;
	}

	@Override
	protected MassIndexingFailureHandler getMassIndexingFailureHandler() {
		return null;
	}

	@Override
	protected void expectNoFailureHandling() {
		// No expected call
	}

	@Override
	protected void assertNoFailureHandling() {
		verifyNoInteractions( failureHandler );
	}
}
