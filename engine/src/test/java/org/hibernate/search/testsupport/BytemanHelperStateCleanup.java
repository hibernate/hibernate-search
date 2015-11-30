/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport;

import org.junit.rules.ExternalResource;

/**
 * A JUnit rule to make sure all tests using the Byteman Helper
 * org.hibernate.search.testsupport.BytemanHelper
 * do properly cleanup the static shared state.
 */
public class BytemanHelperStateCleanup extends ExternalResource {

	protected void after() {
		BytemanHelper.resetEventStack();
		BytemanHelper.getAndResetInvocationCount();
	}

}
