package org.hibernate.search.test.bridge;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

import javax.persistence.*;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
@Table(name = "student")
public class Student {


    private Long id;
    private String name;
    private String grade;
    private Teacher teacher;


    @Id
    @GeneratedValue
    @Column(name = "student_id")
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

    @Column(name = "grade")
    @Field(index = Index.UN_TOKENIZED, store = Store.YES)
    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

}

