/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

import java.util.List;

import com.google.gson.reflect.TypeToken;

public class RootTypeMappingJsonAdapterFactory extends AbstractTypeMappingJsonAdapterFactory {

	private static final TypeToken<List<NamedDynamicTemplate>> DYNAMIC_TEMPLATES_TYPE_TOKEN =
			new TypeToken<List<NamedDynamicTemplate>>() {};

	@Override
	protected <T> void addFields(Builder<T> builder) {
		super.addFields( builder );
		builder.add( "routing", RoutingType.class );
		builder.add( "dynamicTemplates", DYNAMIC_TEMPLATES_TYPE_TOKEN );
	}
}
