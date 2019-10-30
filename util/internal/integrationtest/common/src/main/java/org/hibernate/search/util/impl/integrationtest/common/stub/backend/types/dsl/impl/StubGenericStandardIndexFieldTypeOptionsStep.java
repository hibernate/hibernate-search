/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl;

class StubGenericStandardIndexFieldTypeOptionsStep<F>
		extends AbstractStubStandardIndexFieldTypeOptionsStep<StubGenericStandardIndexFieldTypeOptionsStep<F>, F> {

	StubGenericStandardIndexFieldTypeOptionsStep(Class<F> inputType) {
		super( inputType );
	}

	@Override
	StubGenericStandardIndexFieldTypeOptionsStep<F> thisAsS() {
		return this;
	}

}
