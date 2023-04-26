/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.stub;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubEntityReference;

public class StubEntity {
	public static EntityReference reference(DocumentReference ref) {
		return new StubEntityReference( StubEntity.class, ref.typeName(), ref.id() );
	}

	private final DocumentReference documentReference;

	public StubEntity(DocumentReference documentReference) {
		this.documentReference = documentReference;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "documentReference=" + documentReference
				+ "]";
	}
}
