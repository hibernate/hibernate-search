/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;

public abstract class AbstractFieldTypeDescriptor<F> {

	protected final Class<F> javaType;
	protected final String uniqueName;

	private final List<F> uniquelyMatchableValues = Collections.unmodifiableList( createUniquelyMatchableValues() );

	private final List<F> nonMatchingValues = Collections.unmodifiableList( createNonMatchingValues() );

	protected AbstractFieldTypeDescriptor(Class<F> javaType) {
		this( javaType, javaType.getSimpleName() );
	}

	protected AbstractFieldTypeDescriptor(Class<F> javaType, String uniqueName) {
		this.javaType = javaType;
		this.uniqueName = uniqueName;
	}

	@Override
	public String toString() {
		return getUniqueName();
	}

	public final Class<F> getJavaType() {
		return javaType;
	}

	public final String getUniqueName() {
		return uniqueName;
	}

	public IndexFieldTypeOptionsStep<?, F> configure(IndexFieldTypeFactory fieldContext) {
		return fieldContext.as( javaType );
	}

	/**
	 * @param indexed The value that was indexed.
	 * @return The value that will returned by the backend,
	 * which could be different due to normalization.
	 * In particular, date/time types with an offset/zone will be normalized to UTC and lose the offset/zone information.
	 */
	public F toExpectedDocValue(F indexed) {
		return indexed;
	}


	/**
	 * @return A set of values that can be uniquely matched using predicates.
	 * This excludes empty strings in particular.
	 * This also means distinct values for analyzed/normalized text cannot share the same token.
	 */
	public final List<F> getUniquelyMatchableValues() {
		return uniquelyMatchableValues;
	}

	protected abstract List<F> createUniquelyMatchableValues();

	public final List<F> getNonMatchingValues() {
		return nonMatchingValues;
	}

	protected abstract List<F> createNonMatchingValues();

	public abstract F valueFromInteger(int integer);

	public boolean isFieldSortSupported() {
		// Assume supported by default: this way, we'll get test failures if we forget to override this method.
		return true;
	}

	public abstract Optional<IndexNullAsMatchPredicateExpectactions<F>> getIndexNullAsMatchPredicateExpectations();
}
