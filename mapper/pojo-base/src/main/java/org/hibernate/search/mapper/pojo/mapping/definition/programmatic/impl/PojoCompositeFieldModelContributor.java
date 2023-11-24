/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeOptionsStep;
import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributor;
import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributorContext;

class PojoCompositeFieldModelContributor implements FieldModelContributor {

	public interface DefaultInitiator {
		<F> IndexFieldTypeOptionsStep<?, F> initiate(IndexFieldTypeFactory factory, Class<F> clazz);
	}

	public interface Contributor {
		void contribute(FieldModelContributorContext context);
	}

	private final DefaultInitiator defaultInitiator;
	private final List<Contributor> delegates = new ArrayList<>();

	PojoCompositeFieldModelContributor(DefaultInitiator defaultInitiator) {
		this.defaultInitiator = defaultInitiator;
	}

	@Override
	public <F> IndexFieldTypeOptionsStep<?, F> inferDefaultFieldType(IndexFieldTypeFactory factory, Class<F> clazz) {
		return defaultInitiator.initiate( factory, clazz );
	}

	public void add(Contributor delegate) {
		delegates.add( delegate );
	}

	@Override
	public void contribute(FieldModelContributorContext context) {
		for ( Contributor delegate : delegates ) {
			delegate.contribute( context );
		}
	}

}
