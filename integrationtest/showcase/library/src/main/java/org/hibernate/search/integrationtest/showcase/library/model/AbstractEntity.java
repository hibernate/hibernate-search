/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.model;

import java.util.Objects;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.Hibernate;

@MappedSuperclass
public abstract class AbstractEntity<I> {

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != Hibernate.getClass( o ) ) {
			return false;
		}
		AbstractEntity<?> other = (AbstractEntity<?>) o;
		return Objects.equals( getId(), other.getId() );
	}

	@Override
	public int hashCode() {
		return Objects.hash( getClass() );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( getId() )
				.append( "," )
				.append( getDescriptionForToString() )
				.append( "]" )
				.toString();
	}

	public abstract I getId();

	protected abstract String getDescriptionForToString();
}
