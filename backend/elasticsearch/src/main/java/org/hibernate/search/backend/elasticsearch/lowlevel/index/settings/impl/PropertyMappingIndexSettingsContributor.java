/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl;

public class PropertyMappingIndexSettingsContributor {
	private Boolean knn;

	public void contribute(IndexSettings settings) {
		if ( Boolean.TRUE.equals( knn ) ) {
			settings.setKnn( true );
		}
	}

	public void addKnn(boolean knn) {
		if ( this.knn == null ) {
			this.knn = knn;
		}
		else {
			this.knn = this.knn || Boolean.TRUE.equals( knn );
		}
	}
}
