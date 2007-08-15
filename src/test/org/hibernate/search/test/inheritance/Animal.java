//$Id$
package org.hibernate.search.test.inheritance;

import org.hibernate.search.annotations.Keyword;
import org.hibernate.search.annotations.Text;
import org.hibernate.search.annotations.Indexed;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Animal {
    private Long id;
    private String name;

    @Id @GeneratedValue @Keyword(id=true) 
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Text
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
