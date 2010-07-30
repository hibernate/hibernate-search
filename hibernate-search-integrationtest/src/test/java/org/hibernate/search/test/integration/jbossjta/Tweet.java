package org.hibernate.search.test.integration.jbossjta;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Tweet {
	public Tweet() {};

	public Tweet(String tweet) {
		this.text = tweet;
	}

	@Id
	@GeneratedValue(generator = "uuid")
	@GenericGenerator( name = "uuid", strategy = "uuid")
	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	private String id;

	@Column(name="tweet_text")
	@Field
	public String getText() { return text; };
	public void setText(String text) { this.text = text; }
	private String text;
}
