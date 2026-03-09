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
    private String id;

    private String resourceType = "Patient";

    private String gender;
    private String birthDate;

    @ElementCollection
    @CollectionTable(name = "patient_names", joinColumns = @JoinColumn(name = "patient_id"))
    private List<HumanName> names = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "patient_telecom", joinColumns = @JoinColumn(name = "patient_id"))
    private List<ContactPoint> telecom = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "patient_addresses", joinColumns = @JoinColumn(name = "patient_id"))
    private List<Address> addresses = new ArrayList<>();
}