package ch.bbzw.m324.grade_compass.subject;

import jakarta.persistence.*;

@Entity
@Table(name = "subject")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, unique = true)
    private String name;

    protected Subject(){}

    public Subject(String name){
        this.name = name;
    }

    public Long getId(){return id;}
    public String getName(){return name;}
    public void setName(String name){this.name = name;}
}
