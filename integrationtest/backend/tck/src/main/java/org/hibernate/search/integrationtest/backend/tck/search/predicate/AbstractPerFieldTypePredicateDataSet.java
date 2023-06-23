/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;

public abstract class AbstractPerFieldTypePredicateDataSet<F, V extends AbstractPredicateTestValues<F>>
		extends AbstractPredicateDataSet {

	protected final FieldTypeDescriptor<F> fieldType;
	protected final V values;

	protected AbstractPerFieldTypePredicateDataSet(V values) {
		super( values.fieldType().getUniqueName() );
		fieldType = values.fieldType();
		this.values = values;
	}

}
