/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.nulls.impl;

import org.hibernate.search.bridge.spi.NullMarker;
import org.hibernate.search.elasticsearch.impl.AbstractMarkerField;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchNullMarkerField extends AbstractMarkerField implements NullMarker {

	private final NullMarker nullMarker;
	private final ElasticsearchNullMarkerIndexStrategy indexStrategy;

	public ElasticsearchNullMarkerField(String name, NullMarker nullMarker, ElasticsearchNullMarkerIndexStrategy indexStrategy) {
		super( name );
		this.nullMarker = nullMarker;
		this.indexStrategy = indexStrategy;
	}

	@Override
	public String nullRepresentedAsString() {
		return nullMarker.nullRepresentedAsString();
	}

	@Override
	public Object nullEncoded() {
		return nullMarker.nullEncoded();
	}

	public ElasticsearchNullMarkerIndexStrategy indexStrategy() {
		return indexStrategy;
	}

}
