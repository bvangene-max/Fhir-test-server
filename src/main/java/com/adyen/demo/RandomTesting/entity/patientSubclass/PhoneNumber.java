package com.adyen.demo.RandomTesting.entity.patientSubclass;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class PhoneNumber {
    private String system;
    @Column(name = "phone_value")
    private String value;
    private String use;
}
