/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test.deletebyquery;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

@Entity
@Indexed(index = "hockeyplayer")
public class HockeyPlayer {

	@Id
	@GeneratedValue
	public Long id;

	@Field
	public String name;

	@Field
	public boolean active;

	public String getName() {
		return name;
	}
}
