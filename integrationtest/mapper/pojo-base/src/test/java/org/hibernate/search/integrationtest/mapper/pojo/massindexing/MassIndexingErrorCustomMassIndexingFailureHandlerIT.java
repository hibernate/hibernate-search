/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.massindexing;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class MassIndexingErrorCustomMassIndexingFailureHandlerIT extends AbstractMassIndexingErrorIT {

	@Mock
	private MassIndexingFailureHandler failureHandler;

	@Override
	protected FailureHandler getBackgroundFailureHandlerReference() {
		return null;
	}

	@Override
	protected MassIndexingFailureHandler getMassIndexingFailureHandler() {
		return failureHandler;
	}

	@Override
	protected void expectNoFailureHandling() {
		// No expected call
	}

	@Override
	protected void assertNoFailureHandling() {
		verify( failureHandler ).failureFloodingThreshold();
		verifyNoMoreInteractions( failureHandler );
	}

}
