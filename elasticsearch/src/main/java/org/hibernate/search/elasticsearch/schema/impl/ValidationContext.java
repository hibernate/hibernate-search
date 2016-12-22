/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

final class ValidationContext {
	private final List<ValidationContextElement> elements;

	public ValidationContext(Collection<ValidationContextElement> elements) {
		super();
		this.elements = Collections.unmodifiableList( new ArrayList<>( elements ) );
	}

	public List<ValidationContextElement> getElements() {
		return elements;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj != null && getClass().equals( obj.getClass() ) ) {
			ValidationContext other = (ValidationContext) obj;
			return Objects.equals( elements, other.elements );
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Objects.hashCode( elements );
		return result;
	}
}