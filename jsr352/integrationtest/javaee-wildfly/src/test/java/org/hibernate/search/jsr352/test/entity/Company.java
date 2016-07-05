package org.hibernate.search.jsr352.test.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

@Entity
@Indexed
public class Company implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @DocumentId
    private int id;

    @Field
    private String name;
    
    Company() {
        
    }
    
    public Company(String name) {
        this.name = name;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}
