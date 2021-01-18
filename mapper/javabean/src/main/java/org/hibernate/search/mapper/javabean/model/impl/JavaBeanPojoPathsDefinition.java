/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.model.impl;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hibernate.search.mapper.pojo.model.path.PojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathsDefinition;

/**
 * A factory for filters expecting a simple string representation of dirty paths,
 * in the form "propertyA.propertyB.propertyC".
 * <p>
 * See {@link PojoModelPathPropertyNode#toPropertyString()}.
 */
public class JavaBeanPojoPathsDefinition implements PojoPathsDefinition {

	@Override
	public List<String> preDefinedOrdinals() {
		return Collections.emptyList(); // No pre-defined ordinals
	}

	@Override
	public void interpretPaths(Set<String> target, Set<PojoModelPathValueNode> source) {
		for ( PojoModelPathValueNode path : source ) {
			target.add( path.parent().toPropertyString() );
		}
	}
}
