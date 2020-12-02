/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.gson.impl.SerializeExtraProperties;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.annotations.JsonAdapter;

/**
 * Settings for an Elasticsearch index.
 *
 */
@JsonAdapter(IndexSettingsJsonAdapterFactory.class)
public class IndexSettings {

	private Analysis analysis;

	@SerializeExtraProperties
	private Map<String, JsonElement> extraAttributes;

	public Analysis getAnalysis() {
		return analysis;
	}

	public void setAnalysis(Analysis analysis) {
		this.analysis = analysis;
	}

	public boolean isEmpty() {
		return analysis == null || analysis.isEmpty();
	}

	@Override
	public String toString() {
		return new GsonBuilder().setPrettyPrinting().create().toJson( this );
	}

}
