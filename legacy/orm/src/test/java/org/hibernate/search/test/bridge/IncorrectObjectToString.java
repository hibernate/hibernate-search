/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.bridge;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.bridge.TwoWayStringBridge;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class IncorrectObjectToString {

	@Id
	@GeneratedValue
	@FieldBridge(impl = ErrorOnGetBridge.class)
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	private Long id;

	@Field
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	private String name;

	public static class ErrorOnGetBridge implements TwoWayStringBridge {

		@Override
		public Object stringToObject(String stringValue) {
			return stringValue;
		}

		@Override
		public String objectToString(Object object) {
			throw new RuntimeException( "Failure" );
		}
	}
}
