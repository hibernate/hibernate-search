/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl.translation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

class SplitArrayParameterValueTransformer implements ParameterValueTransformer {
	private final String separatorPattern;
	private final ParameterValueTransformer itemTransformer;

	public SplitArrayParameterValueTransformer(String separatorPattern, ParameterValueTransformer itemTransformer) {
		super();
		this.separatorPattern = separatorPattern;
		this.itemTransformer = itemTransformer;
	}

	@Override
	public JsonElement transform(String parameterValue) {
		JsonArray array = new JsonArray();
		for ( String stringItem : parameterValue.split( separatorPattern ) ) {
			array.add( itemTransformer.transform( stringItem ) );
		}
		return array;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( separatorPattern )
				.append( "," )
				.append( itemTransformer )
				.append( "]" )
				.toString();
	}
}