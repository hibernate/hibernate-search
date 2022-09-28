/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path.impl;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathDefinition;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathDefinitionProvider;

@SuppressWarnings("deprecation")
public final class PojoPathsDefinitionAdapter implements PojoPathDefinitionProvider {
	private final org.hibernate.search.mapper.pojo.model.path.spi.PojoPathsDefinition delegate;

	public PojoPathsDefinitionAdapter(org.hibernate.search.mapper.pojo.model.path.spi.PojoPathsDefinition delegate) {
		this.delegate = delegate;
	}

	@Override
	public List<String> preDefinedOrdinals() {
		return delegate.preDefinedOrdinals();
	}

	@Override
	public PojoPathDefinition interpretPath(PojoModelPathValueNode source) {
		Set<String> stringRepresentations = new LinkedHashSet<>();
		delegate.interpretPaths( stringRepresentations, Collections.singleton( source ) );
		return new PojoPathDefinition( stringRepresentations, Optional.empty() );
	}
}
