package com.adyen.demo.RandomTesting.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.Patient;
import com.adyen.demo.RandomTesting.entity.InternalPatient;
import com.adyen.demo.RandomTesting.entity.patientSubclass.PatientAddress;
import com.adyen.demo.RandomTesting.entity.patientSubclass.Name;
import com.adyen.demo.RandomTesting.entity.patientSubclass.PhoneNumber;
import com.adyen.demo.RandomTesting.repository.PatientRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PatientService {
    private final PatientRepository repository;
    private final IGenericClient fhirClient;
    private final PatientMapper patientMapper;

    public PatientService(PatientRepository repository,
                          IGenericClient fhirClient,
                          PatientMapper patientMapper) {
        this.repository = repository;
        this.fhirClient = fhirClient;
        this.patientMapper = patientMapper;
    }

    public List<InternalPatient> getAllPatients() {
        return repository.findAll();
    }

    public InternalPatient savePatient(InternalPatient p) {
        return repository.save(p);
    }

    public InternalPatient getPatientByID(long ID) {
        return repository.findById(ID).orElse(null);
    }

    public void deletePatientByID(long ID) {
        repository.deleteById(ID);
    }


    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 5)
    @Transactional
    public InternalPatient addName(long patientId, Name name) {
        InternalPatient patient = repository.findById(patientId).orElse(null);
        if (patient == null) return null;
        patient.getNames().add(name);
        return repository.save(patient);
    }

    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 5)
    @Transactional
    public InternalPatient removeName(long patientId, int index) {
        InternalPatient patient = repository.findById(patientId).orElse(null);
        if (patient == null) return null;
        patient.getNames().remove(index);
        return repository.save(patient);
    }

    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 5)
    @Transactional
    public InternalPatient addAddress(long patientId, PatientAddress address) {
        InternalPatient patient = repository.findById(patientId).orElse(null);
        if (patient == null) return null;
        patient.getAddresses().add(address);
        return repository.save(patient);
    }

    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 5)
    @Transactional
    public InternalPatient removeAddress(long patientId, int index) {
        InternalPatient patient = repository.findById(patientId).orElse(null);
        if (patient == null) return null;
        patient.getAddresses().remove(index);
        return repository.save(patient);
    }

    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 5)
    @Transactional
    public InternalPatient addPhoneNumber(long patientId, PhoneNumber phoneNumber) {
        InternalPatient patient = repository.findById(patientId).orElse(null);
        if (patient == null) return null;
        patient.getPhoneNumbers().add(phoneNumber);
        return repository.save(patient);
    }

    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 5)
    @Transactional
    public InternalPatient removePhoneNumber(long patientId, int index) {
        InternalPatient patient = repository.findById(patientId).orElse(null);
        if (patient == null) return null;
        patient.getPhoneNumbers().remove(index);
        return repository.save(patient);
    }

    @Transactional
    public InternalPatient syncPatientFromFhir(String fhirId) {
        Patient fhirPatient = fhirClient
                .read().resource(Patient.class)
                .withId(fhirId).execute();
        try {
            InternalPatient entity = repository.findByFhirId(fhirId)
                    .orElse(new InternalPatient());
            patientMapper.toPatientEntity(fhirPatient, entity);
            return repository.save(entity);
        } catch (DataIntegrityViolationException e) {
            // Another concurrent sync inserted the same fhirId — return the existing row
            return repository.findByFhirId(fhirId)
                    .orElseThrow(() -> e);
        }
    }
}
