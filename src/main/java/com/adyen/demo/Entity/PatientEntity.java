package com.adyen.demo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "patients")
public class PatientEntity {

    @Id
    private String id;

    private String name;
    private String address;
    private String phoneNumber;

    public PatientEntity() {}

    public PatientEntity(String id, String name, String address, String phoneNumber) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phoneNumber = phoneNumber;
    }
}