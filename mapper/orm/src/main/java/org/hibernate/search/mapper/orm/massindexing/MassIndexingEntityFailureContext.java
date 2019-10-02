/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contextual information about a failure to load or index a entities during mass indexing.
 */
public class MassIndexingEntityFailureContext extends MassIndexingFailureContext {

	public static Builder builder() {
		return new Builder();
	}

	private final List<Object> entityReferences;

	private MassIndexingEntityFailureContext(Builder builder) {
		super( builder );
		this.entityReferences = builder.entityReferences == null
				? Collections.emptyList() : Collections.unmodifiableList( builder.entityReferences );
	}

	/**
	 * @return A list of references to entities that may not be indexed correctly as a result of the failure.
	 * Never {@code null}, but may be empty.
	 * Use {@link Object#toString()} to get a textual representation of each reference,
	 * or cast it to the mapper-specific {@code EntityReference} type.
	 */
	public List<Object> getEntityReferences() {
		return entityReferences;
	}

	public static class Builder extends MassIndexingFailureContext.Builder {

		private List<Object> entityReferences;

		private Builder() {
		}

		public void entityReference(Object entityReference) {
			if ( entityReferences == null ) {
				entityReferences = new ArrayList<>();
			}
			entityReferences.add( entityReference );
		}

		@Override
		public MassIndexingEntityFailureContext build() {
			return new MassIndexingEntityFailureContext( this );
		}
	}
}
