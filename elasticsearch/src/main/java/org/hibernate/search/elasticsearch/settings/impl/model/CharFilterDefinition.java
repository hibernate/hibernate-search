/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl.model;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.JsonAdapter;

/**
 * A definition of an Elasticsearch char filter, to be included in index settings.
 *
 * @author Yoann Rodiere
 */
/*
 * CAUTION: JSON serialization is controlled by a specific adapter, which must be
 * updated whenever fields of this class are added, renamed or removed.
 */
@JsonAdapter(AnalysisDefinitionJsonAdapterFactory.class)
public class CharFilterDefinition extends AnalysisDefinition {

	@Override
	public String toString() {
		return new GsonBuilder().setPrettyPrinting().create().toJson( this );
	}

}
