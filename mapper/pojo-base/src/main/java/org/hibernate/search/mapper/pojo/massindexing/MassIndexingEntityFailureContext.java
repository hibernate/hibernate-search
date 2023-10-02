/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.engine.common.EntityReference;

/**
 * Contextual information about a failure to load or index a entities during mass indexing.
 */
public class MassIndexingEntityFailureContext extends MassIndexingFailureContext {

	public static Builder builder() {
		return new Builder();
	}

	private final List<EntityReference> failingEntityReferences;

	private MassIndexingEntityFailureContext(Builder builder) {
		super( builder );
		this.failingEntityReferences = builder.failingEntityReferences == null
				? Collections.emptyList()
				: Collections.unmodifiableList( builder.failingEntityReferences );
	}

	@Override
	public String toString() {
		return "MassIndexingEntityFailureContext{" +
				"failingEntityReferences=" + failingEntityReferences +
				", throwable=" + throwable +
				", failingOperation=" + failingOperation +
				'}';
	}

	/**
	 * @return A list of references to entities that may not be indexed correctly as a result of the failure.
	 * Never {@code null}, but may be empty.
	 * Use {@link Object#toString()} to get a textual representation of each reference,
	 * or cast it to the mapper-specific {@code EntityReference} type.
	 * @deprecated Use {@link #failingEntityReferences()} instead.
	 */
	// The cast is safe because Object is a supertype of EntityReference and the list is unmodifiable.
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Deprecated
	public List<Object> entityReferences() {
		return (List<Object>) (List) failingEntityReferences;
	}

	/**
	 * @return A list of references to entities that may not be indexed correctly as a result of the failure.
	 * Never {@code null}, but may be empty.
	 */
	public List<EntityReference> failingEntityReferences() {
		return failingEntityReferences;
	}

	public static class Builder extends MassIndexingFailureContext.Builder {

		private List<EntityReference> failingEntityReferences;

		private Builder() {
		}

		/**
		 * @param entityReference A reference to an entity related to the failing operation.
		 * @deprecated Use {@link #failingEntityReference(EntityReference)} instead.
		 */
		@Deprecated
		public void entityReference(Object entityReference) {
			// This may fail for callers that don't retrieve the reference the usual way,
			// but we consider that acceptable as this builder should only be used by integrators
			// (mapper implementors or backend implementors).
			failingEntityReference( (EntityReference) entityReference );
		}

		/**
		 * @param entityReference A reference to an entity related to the failing operation.
		 */
		public void failingEntityReference(EntityReference entityReference) {
			if ( failingEntityReferences == null ) {
				failingEntityReferences = new ArrayList<>();
			}
			failingEntityReferences.add( entityReference );
		}

		@Override
		public MassIndexingEntityFailureContext build() {
			return new MassIndexingEntityFailureContext( this );
		}
	}
}
