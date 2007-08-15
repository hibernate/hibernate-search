//$Id$
package org.hibernate.search.test.inheritance;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Keyword;

import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Mammal extends Animal {
    private int mammalNbr;

    @Keyword
	public int getMammalNbr() {
        return mammalNbr;
    }

    public void setMammalNbr(int mammalNbr) {
        this.mammalNbr = mammalNbr;
    }
}
