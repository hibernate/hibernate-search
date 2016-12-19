/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl.model;

import java.util.List;

import com.google.gson.reflect.TypeToken;

/**
 * @author Yoann Rodiere
 */
public class AnalyzerDefinitionJsonAdapterFactory extends AnalysisDefinitionJsonAdapterFactory {

	private static final TypeToken<List<String>> STRING_LIST_TYPE_TOKEN =
			new TypeToken<List<String>>() {
			};

	@Override
	protected <T> void addFields(Builder<T> builder) {
		super.addFields( builder );
		builder.add( "tokenizer", String.class );
		builder.add( "tokenFilters", STRING_LIST_TYPE_TOKEN );
		builder.add( "charFilters", STRING_LIST_TYPE_TOKEN );
	}

}
