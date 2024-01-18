/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.AbstractConfiguredExtraPropertiesJsonAdapterFactory;

public class IndexSettingsJsonAdapterFactory extends AbstractConfiguredExtraPropertiesJsonAdapterFactory {

	@Override
	protected <T> void addFields(Builder<T> builder) {
		builder.add( "analysis", Analysis.class );
		builder.add( "maxResultWindow", Integer.class );
		builder.add( "knn", Boolean.class );
	}
}
