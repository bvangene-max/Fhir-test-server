package com.adyen.demo.Model;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class Address {

    private String line;
    private String city;
    private String postalCode;
    private String country;

    public Address() {}

}
