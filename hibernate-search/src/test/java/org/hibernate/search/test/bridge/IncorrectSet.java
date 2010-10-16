package org.hibernate.search.test.bridge;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.bridge.builtin.ClassBridge;

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
		@Field( bridge = @FieldBridge(impl = ClassBridge.class) )
		public String getName() { return name; }
		public void setName(String name) { this.name = name; }
		private String name;
	}
}
