package com.adyen.demo.Model;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class ContactPoint {

    private String system;  // phone, email
    private String value;

    public ContactPoint() {}
}