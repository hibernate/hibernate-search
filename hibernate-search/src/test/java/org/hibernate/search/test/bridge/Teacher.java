package org.hibernate.search.test.bridge;

import org.hibernate.search.annotations.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
@Table(name = "teacher")
@ClassBridge(
        name = "amount_of_students",
        index = Index.UN_TOKENIZED,
        store = Store.YES,
        impl = StudentsSizeBridge.class
)
public class Teacher {

    private Long id;
    private String name;
    private List<Student> students;


    public Teacher() {
        students = new ArrayList<Student>();
    }

    @Id
    @GeneratedValue
    @Column(name = "teacher_id")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(name = "name")
    @Field(index = Index.TOKENIZED, store = Store.YES)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @OneToMany(mappedBy = "teacher")
    public List<Student> getStudents() {
        return students;
    }

    public void setStudents(List<Student> students) {
        this.students = students;
    }

}

