/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.errorhandling;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Indexed;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
public class Foo {
	@Id
	@GeneratedValue
	private long id;

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "Foo{" );
		sb.append( "id=" ).append( id );
		sb.append( '}' );
		return sb.toString();
	}
}
