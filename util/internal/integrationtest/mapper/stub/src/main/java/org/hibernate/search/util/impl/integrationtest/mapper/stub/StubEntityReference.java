/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import java.util.Objects;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;

public class StubEntityReference {

	public static EntityReferenceFactory<StubEntityReference> FACTORY = StubEntityReference::new;

	private final String typeName;

	private final Object id;

	public StubEntityReference(String typeName, Object id) {
		this.typeName = typeName;
		this.id = id;
	}

	public String getTypeName() {
		return typeName;
	}

	public Object getId() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if ( !( obj instanceof EntityReference ) ) {
			return false;
		}
		StubEntityReference other = (StubEntityReference) obj;
		return typeName.equals( other.typeName ) && Objects.equals( id, other.id );
	}

	@Override
	public int hashCode() {
		return Objects.hash( typeName, id );
	}

	@Override
	public String toString() {
		return typeName + "#" + id;
	}

}
