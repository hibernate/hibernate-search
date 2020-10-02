/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.massindexing;

import static org.mockito.Mockito.verifyNoInteractions;

import org.hibernate.search.mapper.orm.massindexing.MassIndexingFailureHandler;

import org.mockito.Mock;

public class MassIndexingErrorCustomMassIndexingFailureHandlerIT extends AbstractMassIndexingErrorIT {

	@Mock
	private MassIndexingFailureHandler failureHandler;

	@Override
	protected String getBackgroundFailureHandlerReference() {
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
		verifyNoInteractions( failureHandler );
	}

}
