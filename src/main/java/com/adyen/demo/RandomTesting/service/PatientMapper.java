package com.adyen.demo.RandomTesting.service;

import com.adyen.demo.RandomTesting.entity.InternalPatient;
import com.adyen.demo.RandomTesting.entity.patientSubclass.PatientAddress;
import com.adyen.demo.RandomTesting.entity.patientSubclass.Name;
import com.adyen.demo.RandomTesting.entity.patientSubclass.PhoneNumber;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

import java.time.Period;

@Component
public class PatientMapper {

    public InternalPatient toPatientEntity(Patient fhir) {
        return toPatientEntity(fhir, new InternalPatient());
    }

    public InternalPatient toPatientEntity(Patient fhir, InternalPatient patient) {
        patient.getNames().clear();
        patient.getAddresses().clear();
        patient.getPhoneNumbers().clear();

        patient.setFhirId(fhir.getIdElement().getIdPart());

        if (!fhir.getName().isEmpty()) {
            patient.setName(fhir.getNameFirstRep().getNameAsSingleString());
        }

        if (fhir.hasBirthDate()) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(fhir.getBirthDate());
            java.time.LocalDate birthDate = java.time.LocalDate.of(
                    cal.get(java.util.Calendar.YEAR),
                    cal.get(java.util.Calendar.MONTH) + 1,
                    cal.get(java.util.Calendar.DAY_OF_MONTH));
            patient.setAge(calculateAge(birthDate));
        }

        fhir.getName().forEach(fhirName -> patient.getNames().add(mapName(fhirName)));
        fhir.getAddress().forEach(fhirAddress -> patient.getAddresses().add(mapAddress(fhirAddress)));
        fhir.getTelecom().forEach(telecom -> patient.getPhoneNumbers().add(mapPhone(telecom)));

        return patient;
    }

    private int calculateAge(java.time.LocalDate birthDate) {
        if (birthDate.isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Birth date cannot be in the future: " + birthDate);
        }
        return Period.between(birthDate, java.time.LocalDate.now()).getYears();
    }

    private Name mapName(HumanName fhirName) {
        Name name = new Name();
        name.setFamily(fhirName.getFamily());
        if (!fhirName.getGiven().isEmpty()) {
            String given = fhirName.getGiven().stream()
                    .map(pt -> pt.getValue())
                    .collect(java.util.stream.Collectors.joining(" "));
            name.setGiven(given);
        }
        name.setUse(fhirName.getUse() != null ? fhirName.getUse().toCode() : null);
        return name;
    }

    private PatientAddress mapAddress(Address fhirAddress) {
        PatientAddress address = new PatientAddress();
        if (!fhirAddress.getLine().isEmpty()) {
            address.setLine(fhirAddress.getLine().get(0).getValue());
        }
        address.setCity(fhirAddress.getCity());
        address.setPostalCode(fhirAddress.getPostalCode());
        address.setCountry(fhirAddress.getCountry());
        address.setUse(fhirAddress.getUse() != null ? fhirAddress.getUse().toCode() : null);
        return address;
    }

    private PhoneNumber mapPhone(ContactPoint telecom) {
        PhoneNumber phone = new PhoneNumber();
        phone.setSystem(telecom.getSystem() != null ? telecom.getSystem().toCode() : null);
        phone.setValue(telecom.getValue());
        phone.setUse(telecom.getUse() != null ? telecom.getUse().toCode() : null);
        return phone;
    }
}
