package org.hibernate.search.test.id.providedId;

import org.hibernate.search.annotations.*;
import org.hibernate.search.bridge.builtin.LongBridge;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import java.io.Serializable;


/**
 * @author Navin Surtani (<a href="mailto:nsurtani@redhat.com">nsurtani@redhat.com</a>)
 */
@Entity
@ProvidedId(bridge = @FieldBridge(impl = LongBridge.class))
@Indexed
public class ProvidedIdPerson implements Serializable {

	@Id
	@GeneratedValue
	private long id;
	
	@Field(index = Index.TOKENIZED, store = Store.YES)
	private String name;
	@Field(index = Index.TOKENIZED, store = Store.YES)
	private String blurb;
	@Field(index = Index.UN_TOKENIZED, store = Store.YES)
	private int age;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBlurb() {
		return blurb;
	}

	public void setBlurb(String blurb) {
		this.blurb = blurb;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

}
