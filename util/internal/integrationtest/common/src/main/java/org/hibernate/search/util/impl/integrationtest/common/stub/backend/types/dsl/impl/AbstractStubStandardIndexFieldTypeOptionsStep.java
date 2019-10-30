/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl;

import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;

abstract class AbstractStubStandardIndexFieldTypeOptionsStep<S extends AbstractStubStandardIndexFieldTypeOptionsStep<?, F>, F>
		extends AbstractStubIndexFieldTypeOptionsStep<S, F>
		implements StandardIndexFieldTypeOptionsStep<S, F> {

	AbstractStubStandardIndexFieldTypeOptionsStep(Class<F> inputType) {
		super( inputType );
	}

	abstract S thisAsS();

	@Override
	public S projectable(Projectable projectable) {
		modifiers.add( b -> b.projectable( projectable ) );
		return thisAsS();
	}

	@Override
	public S sortable(Sortable sortable) {
		modifiers.add( b -> b.sortable( sortable ) );
		return thisAsS();
	}

	@Override
	public S indexNullAs(F indexNullAs) {
		modifiers.add( b -> b.indexNullAs( indexNullAs ) );
		return thisAsS();
	}

	@Override
	public S searchable(Searchable searchable) {
		modifiers.add( b -> b.searchable( searchable ) );
		return thisAsS();
	}

	@Override
	public S aggregable(Aggregable aggregable) {
		modifiers.add( b -> b.aggregable( aggregable ) );
		return thisAsS();
	}

}
