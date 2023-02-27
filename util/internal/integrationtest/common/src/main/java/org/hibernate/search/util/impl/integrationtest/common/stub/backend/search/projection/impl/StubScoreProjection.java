/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

class StubScoreProjection extends AbstractStubPassThroughProjection<Float> {
	static final StubScoreProjection INSTANCE = new StubScoreProjection();

	@Override
	protected String typeName() {
		return "score";
	}
}
