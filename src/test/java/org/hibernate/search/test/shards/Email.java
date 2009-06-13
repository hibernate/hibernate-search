package org.hibernate.search.test.shards;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FullTextFilterDef;
import org.hibernate.search.annotations.FullTextFilterDefs;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.filter.ShardSensitiveOnlyFilter;

@Entity
@Indexed(index="Email")
@FullTextFilterDef(name="shard", impl= ShardSensitiveOnlyFilter.class)
public class Email {
	
	@Id
	@DocumentId
	private Integer id;
	
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
	
	@Field
	private String body;

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}	

}
