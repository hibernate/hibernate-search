/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.alternative.alternativebinder;

//tag::include[]
public enum Language { // <1>

	ENGLISH( "en" ),
	FRENCH( "fr" ),
	GERMAN( "de" );

	public final String code;

	Language(String code) {
		this.code = code;
	}
}
//end::include[]
