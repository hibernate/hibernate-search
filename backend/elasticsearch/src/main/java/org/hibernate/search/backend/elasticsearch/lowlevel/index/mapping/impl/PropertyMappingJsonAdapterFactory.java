/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

import com.google.gson.JsonElement;

public class PropertyMappingJsonAdapterFactory extends AbstractTypeMappingJsonAdapterFactory {

	@Override
	protected <T> void addFields(Builder<T> builder) {
		super.addFields( builder );
		builder.add( "type", String.class );
		builder.add( "index", Boolean.class );
		builder.add( "norms", Boolean.class );
		builder.add( "docValues", Boolean.class );
		builder.add( "store", Boolean.class );
		builder.add( "nullValue", JsonElement.class );
		builder.add( "analyzer", String.class );
		builder.add( "searchAnalyzer", String.class );
		builder.add( "normalizer", String.class );
		builder.add( "format", new FormatJsonAdapter() );
		builder.add( "scalingFactor", Double.class );
		builder.add( "termVector", String.class );
	}
}
