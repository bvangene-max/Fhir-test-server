package com.adyen.demo.RandomTesting.controller;

import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.adyen.demo.RandomTesting.dto.PatientRequest;
import com.adyen.demo.RandomTesting.entity.InternalPatient;
import com.adyen.demo.RandomTesting.service.PatientService;
import com.adyen.demo.RandomTesting.entity.patientSubclass.PatientAddress;
import com.adyen.demo.RandomTesting.entity.patientSubclass.Name;
import com.adyen.demo.RandomTesting.entity.patientSubclass.PhoneNumber;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.Validator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping ("/patients")
public class PatientController {
    private final PatientService service;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public PatientController(PatientService service, ObjectMapper objectMapper, Validator validator) {
        this.service = service;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @GetMapping
    public List<InternalPatient> getAllPatients() {
        return service.getAllPatients();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InternalPatient createPatient(@Valid @RequestBody PatientRequest request) {
        InternalPatient p = new InternalPatient();
        p.setName(request.getName());
        p.setAge(request.getAge());
        p.getNames().addAll(request.getNames());
        p.getAddresses().addAll(request.getAddresses());
        p.getPhoneNumbers().addAll(request.getPhoneNumbers());
        try {
            return service.savePatient(p);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A patient with that fhirId already exists", e);
        }
    }

    @PostMapping("/sync/{fhirId}")
    public InternalPatient syncPatient(@PathVariable String fhirId) {
        try {
            return service.syncPatientFromFhir(fhirId);
        } catch (ResourceNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FHIR patient not found", e);
        } catch (FhirClientConnectionException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not reach FHIR server", e);
        } catch (BaseServerResponseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "FHIR server error: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/{id}")
    public InternalPatient getPatient(@PathVariable long id) {
        InternalPatient patient = service.getPatientByID(id);
        if (patient == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
        }
        return patient;
    }

    @PatchMapping("/{id}")
    public InternalPatient patchPatient(@PathVariable long id, @RequestBody Map<String, Object> patch) {
        patch.remove("id");
        patch.remove("version");
        patch.remove("fhirId");
        for (int attempt = 1; attempt <= 5; attempt++) {
            InternalPatient existing = service.getPatientByID(id);
            if (existing == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
            }
            try {
                objectMapper.updateValue(existing, patch);
            } catch (tools.jackson.core.JacksonException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid patch body", e);
            }
            var violations = validator.validate(existing);
            if (!violations.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, violations.iterator().next().getMessage());
            }
            existing.setId(id);
            try {
                return service.savePatient(existing);
            } catch (OptimisticLockingFailureException e) {
                if (attempt == 5) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Patient was modified by another request, please retry");
                }
            }
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Patient was modified by another request, please retry");
    }

    @DeleteMapping("/{id}")
    public void deletePatient(@PathVariable long id) {
        try {
            service.deletePatientByID(id);
        } catch (OptimisticLockingFailureException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Patient was modified by another request, please retry");
        }
    }

    @PostMapping("/{id}/names")
    public InternalPatient addName(@PathVariable long id, @RequestBody Name name) {
        try {
            InternalPatient patient = service.addName(id, name);
            if (patient == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
            return patient;
        } catch (OptimisticLockingFailureException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Patient was modified by another request, please retry");
        }
    }

    @DeleteMapping("/{id}/names/{index}")
    public InternalPatient removeName(@PathVariable long id, @PathVariable int index) {
        try {
            InternalPatient patient = service.removeName(id, index);
            if (patient == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
            return patient;
        } catch (IndexOutOfBoundsException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Name index out of bounds");
        } catch (OptimisticLockingFailureException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Patient was modified by another request, please retry");
        }
    }

    @PostMapping("/{id}/addresses")
    public InternalPatient addAddress(@PathVariable long id, @RequestBody PatientAddress address) {
        try {
            InternalPatient patient = service.addAddress(id, address);
            if (patient == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
            return patient;
        } catch (OptimisticLockingFailureException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Patient was modified by another request, please retry");
        }
    }

    @DeleteMapping("/{id}/addresses/{index}")
    public InternalPatient removeAddress(@PathVariable long id, @PathVariable int index) {
        try {
            InternalPatient patient = service.removeAddress(id, index);
            if (patient == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
            return patient;
        } catch (IndexOutOfBoundsException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Address index out of bounds");
        } catch (OptimisticLockingFailureException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Patient was modified by another request, please retry");
        }
    }

    @PostMapping("/{id}/phone-numbers")
    public InternalPatient addPhoneNumber(@PathVariable long id, @RequestBody PhoneNumber phoneNumber) {
        try {
            InternalPatient patient = service.addPhoneNumber(id, phoneNumber);
            if (patient == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
            return patient;
        } catch (OptimisticLockingFailureException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Patient was modified by another request, please retry");
        }
    }

    @DeleteMapping("/{id}/phone-numbers/{index}")
    public InternalPatient removePhoneNumber(@PathVariable long id, @PathVariable int index) {
        try {
            InternalPatient patient = service.removePhoneNumber(id, index);
            if (patient == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
            return patient;
        } catch (IndexOutOfBoundsException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Phone number index out of bounds");
        } catch (OptimisticLockingFailureException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Patient was modified by another request, please retry");
        }
    }
}
