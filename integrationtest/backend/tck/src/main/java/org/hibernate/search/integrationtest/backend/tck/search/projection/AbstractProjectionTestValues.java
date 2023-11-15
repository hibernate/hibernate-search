/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;

/**
 * @param <F> The type of field values.
 * @param <P> The type of projected values.
 */
public abstract class AbstractProjectionTestValues<F, P> {

	protected final FieldTypeDescriptor<F, ?> fieldType;

	protected AbstractProjectionTestValues(FieldTypeDescriptor<F, ?> fieldType) {
		this.fieldType = fieldType;
	}

	public FieldTypeDescriptor<F, ?> fieldType() {
		return fieldType;
	}

	public abstract F fieldValue(int ordinal);

	public abstract P projectedValue(int ordinal);

	public List<P> projectedValues(int... ordinals) {
		return IntStream.of( ordinals ).mapToObj( this::projectedValue )
				.collect( Collectors.toList() );
	}

}
