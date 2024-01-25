/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.model.impl;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.model.path.PojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathDefinition;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathDefinitionProvider;

/**
 * A {@link PojoPathDefinitionProvider} using a simple string representation of dirty paths,
 * in the form "propertyA.propertyB.propertyC".
 * <p>
 * See {@link PojoModelPathPropertyNode#toPropertyString()}.
 */
public class StandalonePojoPathsDefinitionProvider implements PojoPathDefinitionProvider {

	@Override
	public List<String> preDefinedOrdinals() {
		return Collections.emptyList(); // No pre-defined ordinals
	}

	@Override
	public PojoPathDefinition interpretPath(PojoModelPathValueNode source) {
		Set<String> stringRepresentations = new LinkedHashSet<>();
		stringRepresentations.add( source.parent().toPropertyString() );
		return new PojoPathDefinition( stringRepresentations, Optional.empty() );
	}
}
