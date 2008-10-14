package example;

import org.hibernate.search.annotations.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/*
 *
 */
@Entity
public class Author {
    @Id
    @GeneratedValue
    private Integer id;
    private String name;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Field(index = Index.TOKENIZED, store = Store.YES)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
