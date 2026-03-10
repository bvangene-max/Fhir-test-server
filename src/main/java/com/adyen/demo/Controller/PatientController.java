package com.adyen.demo.Controller;

import com.adyen.demo.Patient;
import com.adyen.demo.PatientRepository;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/patients")
public class PatientController {

    private final PatientRepository repository;

    public PatientController(PatientRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Patient> getAllPatients() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Patient getPatientById(@PathVariable Long id) {
        return repository.findById(id).orElseThrow();
    }

    @PostMapping
    public Patient createPatient(@RequestBody Patient patient) {
        return repository.save(patient);
    }

    @PatchMapping("/{id}")
    public Patient patchPatient(@PathVariable Long id, @RequestBody Patient patient) {
        Patient patient_existing = getPatientById(id);
        if (patient.getGender() != null)
            patient_existing.setGender(patient.getGender());
        if (patient.getBirthDate() != null)
            patient_existing.setBirthDate(patient.getBirthDate());
        if (patient.getName() != null)
            patient_existing.setName(patient.getName());
        if (patient.getTelecom() != null)
            patient_existing.setTelecom(patient.getTelecom());
        if (patient.getAddress() != null)
            patient_existing.setAddress(patient.getAddress());
        return repository.save(patient_existing);
    }

    @DeleteMapping
    public void deleteAllPatients() {
        repository.deleteAll();
    }

    @DeleteMapping("/{id}")
    public void deletePatient(@PathVariable Long id) {
        Patient patient = getPatientById(id);
        repository.delete(patient);
    }
}