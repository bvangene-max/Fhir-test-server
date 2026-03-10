package com.adyen.demo;

import com.adyen.demo.Model.Address;
import com.adyen.demo.Model.ContactPoint;
import com.adyen.demo.Model.HumanName;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String resourceType = "Patient";

    private String gender;
    private String birthDate;

    //is this the best thing to use here?
    @ElementCollection
    @CollectionTable(name = "patient_names", joinColumns = @JoinColumn(name = "patient_id"))
    private List<HumanName> name = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "patient_telecom", joinColumns = @JoinColumn(name = "patient_id"))
    private List<ContactPoint> telecom = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "patient_addresses", joinColumns = @JoinColumn(name = "patient_id"))
    private List<Address> address = new ArrayList<>();
}