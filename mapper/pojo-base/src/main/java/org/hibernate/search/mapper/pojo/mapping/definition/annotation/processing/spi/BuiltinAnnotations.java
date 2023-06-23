/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.spi;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.jandex.DotName;

public final class BuiltinAnnotations {

	private BuiltinAnnotations() {
	}

	public static final DotName ROOT_MAPPING =
			DotName.createSimple( "org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.RootMapping" );

	public static final List<DotName> ROOT_MAPPING_ANNOTATIONS = Collections.unmodifiableList( Arrays.asList(
			DotName.createSimple( "org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor" )
	) );

}
