/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.massindexing;

import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import org.hibernate.search.mapper.orm.massindexing.MassIndexingFailureHandler;

import org.easymock.EasyMock;

public class MassIndexingErrorCustomMassIndexingFailureHandlerIT extends AbstractMassIndexingErrorIT {

	private final MassIndexingFailureHandler failureHandler = EasyMock.createMock( MassIndexingFailureHandler.class );

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
		reset( failureHandler );
		// No expected call
		replay( failureHandler );
	}

	@Override
	protected void assertNoFailureHandling() {
		verify( failureHandler );
	}

}
