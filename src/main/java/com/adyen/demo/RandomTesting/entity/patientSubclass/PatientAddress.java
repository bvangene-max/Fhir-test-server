package com.adyen.demo.RandomTesting.entity.patientSubclass;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class PatientAddress {
    private String line;
    private String city;
    private String postalCode;
    private String country;
    private String use;
}
