/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import java.util.Objects;
import java.util.stream.Stream;

import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;

public class StubTypeModel implements MappableTypeModel {

	private final String typeIdentifier;

	public StubTypeModel(String typeIdentifier) {
		this.typeIdentifier = typeIdentifier;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + typeIdentifier + "]";
	}

	public String asString() {
		return typeIdentifier;
	}

	@Override
	public String name() {
		return typeIdentifier;
	}

	@Override
	public boolean isAbstract() {
		return false;
	}

	@Override
	public boolean isSubTypeOf(MappableTypeModel other) {
		return false;
	}

	@Override
	public Stream<? extends MappableTypeModel> ascendingSuperTypes() {
		return Stream.of( this );
	}

	@Override
	public Stream<? extends MappableTypeModel> descendingSuperTypes() {
		return Stream.of( this );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		StubTypeModel that = (StubTypeModel) o;
		return Objects.equals( typeIdentifier, that.typeIdentifier );
	}

	@Override
	public int hashCode() {
		return Objects.hash( typeIdentifier );
	}
}
