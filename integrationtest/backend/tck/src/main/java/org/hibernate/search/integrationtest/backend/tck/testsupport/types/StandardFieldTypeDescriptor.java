/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;

public abstract class StandardFieldTypeDescriptor<F>
		extends FieldTypeDescriptor<F, StandardIndexFieldTypeOptionsStep<?, F>> {

	protected StandardFieldTypeDescriptor(Class<F> javaType) {
		super( javaType );
	}

	protected StandardFieldTypeDescriptor(Class<F> javaType, String uniqueName) {
		super( javaType, uniqueName );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, F> configure(IndexFieldTypeFactory fieldContext) {
		return fieldContext.as( javaType );
	}
}
