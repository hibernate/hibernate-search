package org.hibernate.search.test.bridge;

import org.hibernate.search.annotations.*;
import org.hibernate.search.bridge.TwoWayStringBridge;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class IncorrectObjectToString {
	@Id @GeneratedValue @Field( bridge = @FieldBridge(impl = ErrorOnGetBridge.class) )
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	private Long id;

	@Field
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	private String name;

	public static class ErrorOnGetBridge implements TwoWayStringBridge {

		public Object stringToObject(String stringValue) {
			return stringValue;
		}

		public String objectToString(Object object) {
			throw new RuntimeException("Failure");
		}
	}
}
