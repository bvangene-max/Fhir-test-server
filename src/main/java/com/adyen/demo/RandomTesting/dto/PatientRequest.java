package com.adyen.demo.RandomTesting.dto;

import com.adyen.demo.RandomTesting.entity.patientSubclass.Name;
import com.adyen.demo.RandomTesting.entity.patientSubclass.PatientAddress;
import com.adyen.demo.RandomTesting.entity.patientSubclass.PhoneNumber;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PatientRequest {
    @NotBlank
    private String name;

    @NotNull
    @Min(0)
    @Max(150)
    private Integer age;

    private List<Name> names = new ArrayList<>();
    private List<PatientAddress> addresses = new ArrayList<>();
    private List<PhoneNumber> phoneNumbers = new ArrayList<>();
}
