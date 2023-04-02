package com.flowershop.wrapper;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserWrapper {

    private Integer id;

    private String firstName;

    private String lastName;

    private String email;

    private String phoneNr;

    private String status;

    public UserWrapper(Integer id, String firstName, String lastName, String email, String phoneNr, String status) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNr = phoneNr;
        this.status = status;
    }
}
