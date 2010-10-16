package org.hibernate.search.test.bridge;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.TwoWayStringBridge;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class IncorrectGet {
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
		@Field( bridge = @FieldBridge(impl = ErrorOnGetBridge.class), store = Store.YES)
		public String getName() { return name; }
		public void setName(String name) { this.name = name; }
		private String name;
	}

	public static class ErrorOnGetBridge implements TwoWayStringBridge {

		public Object stringToObject(String stringValue) {
			throw new RuntimeException("Failure");
		}

		public String objectToString(Object object) {
			return object.toString();
		}
	}
}
