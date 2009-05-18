//$Id$
package org.hibernate.search.test.inheritance;

import javax.persistence.Entity;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
public class Eagle extends Bird {

	private WingType wingYype;

	@Field(index = Index.UN_TOKENIZED, store = Store.YES)
	public WingType getWingYype() {
		return wingYype;
	}

	public void setWingYype(WingType wingYype) {
		this.wingYype = wingYype;
	}

	public enum WingType {
		BROAD,
		LONG
	}
}