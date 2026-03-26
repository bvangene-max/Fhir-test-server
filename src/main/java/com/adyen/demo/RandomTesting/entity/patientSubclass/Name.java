package com.adyen.demo.RandomTesting.entity.patientSubclass;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class Name {
    private String family;
    private String given;
    private String use;
}
