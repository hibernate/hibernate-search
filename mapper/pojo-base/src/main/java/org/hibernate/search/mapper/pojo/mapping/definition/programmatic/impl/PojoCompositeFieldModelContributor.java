/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributor;
import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributorContext;

class PojoCompositeFieldModelContributor implements FieldModelContributor {
	private final List<FieldModelContributor> delegates = new ArrayList<>();

	public void add(FieldModelContributor delegate) {
		delegates.add( delegate );
	}

	@Override
	public void contribute(FieldModelContributorContext context) {
		for ( FieldModelContributor delegate : delegates ) {
			delegate.contribute( context );
		}
	}

}
