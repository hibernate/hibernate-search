/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

	public Integer getDecimalScale() {
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
