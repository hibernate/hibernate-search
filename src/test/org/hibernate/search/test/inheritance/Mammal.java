//$Id$
package org.hibernate.search.test.inheritance;

import java.io.Serializable;
import javax.persistence.Entity;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Mammal extends Animal implements Serializable {
    private boolean hasSweatGlands;

    @Field(index= Index.UN_TOKENIZED, store= Store.YES)
	public boolean isHasSweatGlands() {
		return hasSweatGlands;
	}

	public void setHasSweatGlands(boolean hasSweatGlands) {
		this.hasSweatGlands = hasSweatGlands;
	}
}
