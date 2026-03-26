package com.adyen.demo.RandomTesting.service;

// Shared Mockito setup for PatientService unit tests.
// All mocks and the service under test are declared here so subclasses stay focused on tests.

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.adyen.demo.RandomTesting.repository.PatientRepository;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
abstract class PatientServiceTestBase {

    @Mock
    PatientRepository repository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    IGenericClient fhirClient;

    @Mock
    PatientMapper patientMapper;

    @InjectMocks
    PatientService service;
}
