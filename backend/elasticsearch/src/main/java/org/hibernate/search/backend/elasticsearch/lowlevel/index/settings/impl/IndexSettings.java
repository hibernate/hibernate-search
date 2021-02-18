/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.gson.impl.SerializeExtraProperties;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.annotations.JsonAdapter;

/**
 * Settings for an Elasticsearch index.
 */
@JsonAdapter(IndexSettingsJsonAdapterFactory.class)
public class IndexSettings {

	private Analysis analysis;

	@SerializeExtraProperties
	private Map<String, JsonElement> extraAttributes;

	public IndexSettings() {
	}

	public IndexSettings(Analysis analysis, Map<String, JsonElement> extraAttributes) {
		this.analysis = analysis;
		this.extraAttributes = extraAttributes;
	}

	public Analysis getAnalysis() {
		return analysis;
	}

	public void setAnalysis(Analysis analysis) {
		this.analysis = analysis;
	}

	public Map<String, JsonElement> getExtraAttributes() {
		return extraAttributes;
	}

	@Override
	public String toString() {
		return new GsonBuilder().setPrettyPrinting().create().toJson( this );
	}

	/**
	 * Merge these settings with other settings.
	 * Any conflict of definition will be solved in favour of the other settings.
	 * {@link #extraAttributes} will be always overridden.
	 *
	 * @param overridingIndexSettings The other index settings
	 */
	public void merge(IndexSettings overridingIndexSettings) {
		if ( overridingIndexSettings == null ) {
			// nothing to do
			return;
		}

		if ( analysis == null ) {
			analysis = overridingIndexSettings.analysis;
		}
		else {
			analysis.merge( overridingIndexSettings.analysis );
		}

		extraAttributes = overridingIndexSettings.extraAttributes;
	}

	/**
	 * Remove all entries from {@link #extraAttributes} that are present
	 * with the exact same values on {@code extraAttributesToRemove} parameter.
	 *
	 * @param extraAttributesToRemove Other index settings extra attributes
	 */
	public IndexSettings diff(Map<String, JsonElement> extraAttributesToRemove) {
		if ( extraAttributes == null || extraAttributes.isEmpty() ) {
			// nothing to do
			return this;
		}

		Set<String> keysToRemove = new HashSet<>();
		for ( Map.Entry<String, JsonElement> extraAttribute : extraAttributes.entrySet() ) {
			String key = extraAttribute.getKey();
			if ( !extraAttributesToRemove.containsKey( key ) ) {
				continue;
			}

			if ( Objects.equals( extraAttributesToRemove.get( key ), extraAttribute.getValue() ) ) {
				keysToRemove.add( key );
			}
		}

		if ( keysToRemove.isEmpty() ) {
			// nothing to do
			return this;
		}

		Map<String, JsonElement> newExtraAttributes = new HashMap<>( extraAttributes );
		for ( String key : keysToRemove ) {
			newExtraAttributes.remove( key );
		}
		return new IndexSettings( analysis, newExtraAttributes );
	}
}
