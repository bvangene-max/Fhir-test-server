package com.adyen.demo.RandomTesting.entity;

import com.adyen.demo.RandomTesting.entity.patientSubclass.PatientAddress;
import com.adyen.demo.RandomTesting.entity.patientSubclass.Name;
import com.adyen.demo.RandomTesting.entity.patientSubclass.PhoneNumber;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.ArrayList;

@Entity
@Getter
@Setter
public class InternalPatient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Version
    private long version;

    @Column(unique = true)
    private String fhirId;
    private String name;
    @Min(0)
    @Max(150)
    private int age;

    @ElementCollection
    @CollectionTable(name = "patient_names", joinColumns = @JoinColumn(name = "patient_id"))
    private List<Name> names = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "patient_addresses", joinColumns = @JoinColumn(name = "patient_id"))
    private List<PatientAddress> addresses = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "patient_phone_numbers", joinColumns = @JoinColumn(name = "patient_id"))
    private List<PhoneNumber> phoneNumbers = new ArrayList<>();

    public InternalPatient() {}
}