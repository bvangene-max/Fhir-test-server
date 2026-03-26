package com.adyen.demo.RandomTesting.service;

// Unit tests for PatientMapper collection field mapping:
// addresses, telecom (phone/email), multiple entries, null use fields,
// and the two-argument overload that updates an existing entity in place.

import com.adyen.demo.RandomTesting.entity.InternalPatient;
import com.adyen.demo.RandomTesting.entity.patientSubclass.Name;
import com.adyen.demo.RandomTesting.entity.patientSubclass.PatientAddress;
import com.adyen.demo.RandomTesting.entity.patientSubclass.PhoneNumber;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Address.AddressUse;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointUse;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PatientMapperSubCollectionTest {

    private final PatientMapper mapper = new PatientMapper();

    @Test
    void toPatientEntity_address_allFieldsMapped() {
        var fhirPatient = new Patient();
        fhirPatient.setId("Patient/addr");
        fhirPatient.addAddress(new Address()
                .addLine("123 Main")
                .setCity("Brussels")
                .setPostalCode("1000")
                .setCountry("BE")
                .setUse(AddressUse.HOME));

        InternalPatient entity = mapper.toPatientEntity(fhirPatient);

        assertThat(entity.getAddresses()).hasSize(1);
        PatientAddress address = entity.getAddresses().get(0);
        assertThat(address.getLine()).isEqualTo("123 Main");
        assertThat(address.getCity()).isEqualTo("Brussels");
        assertThat(address.getPostalCode()).isEqualTo("1000");
        assertThat(address.getCountry()).isEqualTo("BE");
        assertThat(address.getUse()).isEqualTo("home");
    }

    @Test
    void toPatientEntity_addressWithNoLines_lineIsNull() {
        var fhirPatient = new Patient();
        fhirPatient.setId("Patient/noline");
        fhirPatient.addAddress(new Address().setCity("Brussels").setUse(AddressUse.HOME));

        InternalPatient entity = mapper.toPatientEntity(fhirPatient);

        assertThat(entity.getAddresses().get(0).getLine()).isNull();
        assertThat(entity.getAddresses().get(0).getCity()).isEqualTo("Brussels");
    }

    @Test
    void toPatientEntity_nullAddressUse_useIsNull() {
        var fhirPatient = new Patient();
        fhirPatient.setId("Patient/no-addr-use");
        fhirPatient.addAddress(new Address().setCity("Brussels"));

        assertThat(mapper.toPatientEntity(fhirPatient).getAddresses().get(0).getUse()).isNull();
    }

    @Test
    void toPatientEntity_phoneTelecom_allFieldsMapped() {
        var fhirPatient = new Patient();
        fhirPatient.setId("Patient/phone");
        fhirPatient.addTelecom(new ContactPoint()
                .setSystem(ContactPointSystem.PHONE)
                .setValue("+321234567")
                .setUse(ContactPointUse.MOBILE));

        InternalPatient entity = mapper.toPatientEntity(fhirPatient);

        assertThat(entity.getPhoneNumbers()).hasSize(1);
        PhoneNumber phone = entity.getPhoneNumbers().get(0);
        assertThat(phone.getSystem()).isEqualTo("phone");
        assertThat(phone.getValue()).isEqualTo("+321234567");
        assertThat(phone.getUse()).isEqualTo("mobile");
    }

    @Test
    void toPatientEntity_emailTelecom_systemMappedCorrectly() {
        // The phoneNumbers collection stores all telecom entries, not just phone.
        // This verifies that the email system code is preserved as-is.
        var fhirPatient = new Patient();
        fhirPatient.setId("Patient/email");
        fhirPatient.addTelecom(new ContactPoint()
                .setSystem(ContactPoint.ContactPointSystem.EMAIL)
                .setValue("test@example.com")
                .setUse(ContactPoint.ContactPointUse.WORK));

        InternalPatient entity = mapper.toPatientEntity(fhirPatient);

        assertThat(entity.getPhoneNumbers().get(0).getSystem()).isEqualTo("email");
        assertThat(entity.getPhoneNumbers().get(0).getValue()).isEqualTo("test@example.com");
        assertThat(entity.getPhoneNumbers().get(0).getUse()).isEqualTo("work");
    }

    @Test
    void toPatientEntity_nullTelecomSystemAndUse_fieldsAreNull() {
        var fhirPatient = new Patient();
        fhirPatient.setId("Patient/no-system");
        fhirPatient.addTelecom(new ContactPoint().setValue("+320000000"));

        InternalPatient entity = mapper.toPatientEntity(fhirPatient);

        assertThat(entity.getPhoneNumbers().get(0).getSystem()).isNull();
        assertThat(entity.getPhoneNumbers().get(0).getUse()).isNull();
        assertThat(entity.getPhoneNumbers().get(0).getValue()).isEqualTo("+320000000");
    }

    @Test
    void toPatientEntity_multipleNamesAddressesAndPhones_allMapped() {
        var fhirPatient = new Patient();
        fhirPatient.setId("Patient/multi");
        fhirPatient.addName(new HumanName().setFamily("Smith").addGiven("John").setUse(HumanName.NameUse.OFFICIAL));
        fhirPatient.addName(new HumanName().setFamily("Smith").addGiven("Johnny").setUse(HumanName.NameUse.NICKNAME));
        fhirPatient.addAddress(new Address().addLine("1 Home St").setCity("Paris").setUse(AddressUse.HOME));
        fhirPatient.addAddress(new Address().addLine("2 Work Ave").setCity("Lyon").setUse(AddressUse.WORK));
        fhirPatient.addTelecom(new ContactPoint().setSystem(ContactPointSystem.PHONE).setValue("+33111").setUse(ContactPointUse.HOME));
        fhirPatient.addTelecom(new ContactPoint().setSystem(ContactPointSystem.PHONE).setValue("+33222").setUse(ContactPointUse.WORK));

        InternalPatient entity = mapper.toPatientEntity(fhirPatient);

        assertThat(entity.getNames()).hasSize(2);
        assertThat(entity.getAddresses()).hasSize(2);
        assertThat(entity.getPhoneNumbers()).hasSize(2);
    }

    @Test
    void toPatientEntity_withExistingEntity_clearsAndReplacesCollections() {
        // When re-syncing, the mapper must wipe the old collections before
        // populating from FHIR so stale entries don't accumulate.
        var existing = new InternalPatient();
        existing.getNames().add(new Name());
        existing.getAddresses().add(new PatientAddress());
        existing.getPhoneNumbers().add(new PhoneNumber());

        var fhirPatient = new Patient();
        fhirPatient.setId("Patient/upd");
        fhirPatient.addName(new HumanName().setFamily("New").addGiven("Name"));

        InternalPatient result = mapper.toPatientEntity(fhirPatient, existing);

        assertThat(result).isSameAs(existing);
        assertThat(result.getNames()).hasSize(1);
        assertThat(result.getNames().get(0).getFamily()).isEqualTo("New");
        assertThat(result.getAddresses()).isEmpty();
        assertThat(result.getPhoneNumbers()).isEmpty();
    }
}
