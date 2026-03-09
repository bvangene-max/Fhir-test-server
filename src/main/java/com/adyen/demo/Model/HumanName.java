package com.adyen.demo.Model;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class HumanName {

    private String family;
    private String given;

    public HumanName() {}
}
