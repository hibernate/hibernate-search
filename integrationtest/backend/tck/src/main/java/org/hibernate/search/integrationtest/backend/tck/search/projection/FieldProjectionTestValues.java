/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;

public class FieldProjectionTestValues<F> extends AbstractProjectionTestValues<F, F> {
	protected FieldProjectionTestValues(FieldTypeDescriptor<F, ?> fieldType) {
		super( fieldType );
	}

	@Override
	public F fieldValue(int ordinal) {
		return fieldType.valueFromInteger( ordinal );
	}

	@Override
	public F projectedValue(int ordinal) {
		return fieldType.valueFromInteger( ordinal );
	}
}
