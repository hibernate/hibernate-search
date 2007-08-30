//$Id$
package org.hibernate.search.test.inheritance;

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
public class Mammal extends Animal {
    private int mammalNbr;

    @Field(index= Index.UN_TOKENIZED, store= Store.YES)
	public int getMammalNbr() {
        return mammalNbr;
    }

    public void setMammalNbr(int mammalNbr) {
        this.mammalNbr = mammalNbr;
    }
}
