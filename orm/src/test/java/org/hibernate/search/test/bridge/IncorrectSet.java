/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.bridge;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.bridge.builtin.EnumBridge;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class IncorrectSet {
	@Id @GeneratedValue
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	private Long id;

	@IndexedEmbedded
	@Embedded
	public SubIncorrect getSubIncorrect() { return subIncorrect; }
	public void setSubIncorrect(SubIncorrect subIncorrect) { this.subIncorrect = subIncorrect; }
	private SubIncorrect subIncorrect;

	public static class SubIncorrect {
		@Field( bridge = @FieldBridge(impl = EnumBridge.class) )
		public String getName() { return name; }
		public void setName(String name) { this.name = name; }
		private String name;
	}
}
