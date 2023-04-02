package com.flowershop.POJO;

import javax.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;

@NamedQuery(name = "User.findByEmailId", query = "select u from User u where u.email =: email")
@NamedQuery(name = "User.getAllUser", query = "select new com.flowershop.wrapper.UserWrapper(u.id, u.firstName, u.lastName, u.email, u.phoneNr, u.status) from User u where u.role = 'user'")
@NamedQuery(name = "User.getAllAdmin", query = "select u.email from User u where u.role = 'admin'")
@NamedQuery(name = "User.updateStatus", query = "update User u set u.status=:status where u.id=:id")

@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "firstName")
    private String firstName;

    @Column(name = "lastName")
    private String lastName;
    @Column(name = "email")
    private String email;

    @Column(name = "phoneNr")
    private String phoneNr;

    @Column(name = "password")
    private String password;

    @Column(name = "userStatus")
    private String status;

    @Column(name = "role")
    private String role;
}
