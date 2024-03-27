/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

public class IndexFieldTypeDefaultsProvider {

	private final Integer decimalScale;

	public IndexFieldTypeDefaultsProvider() {
		this( null );
	}

	public IndexFieldTypeDefaultsProvider(Integer decimalScale) {
		this.decimalScale = decimalScale;
	}

	public Integer decimalScale() {
		return decimalScale;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "IndexFieldTypeDefaultsProvider{" );
		sb.append( "decimalScale=" ).append( decimalScale );
		sb.append( '}' );
		return sb.toString();
	}
}
