/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
