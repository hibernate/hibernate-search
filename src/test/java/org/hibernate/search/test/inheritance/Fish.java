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
public class Fish extends Animal {

	private int numberOfDorsalFins;

	@Field(index = Index.UN_TOKENIZED, store = Store.YES)
	public int getNumberOfDorsalFins() {
		return numberOfDorsalFins;
	}

	public void setNumberOfDorsalFins(int numberOfDorsalFins) {
		this.numberOfDorsalFins = numberOfDorsalFins;
	}
}