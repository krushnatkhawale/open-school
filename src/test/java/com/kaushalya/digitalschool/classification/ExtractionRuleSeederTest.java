package com.kaushalya.digitalschool.classification;

import com.kaushalya.digitalschool.storage.ExtractionRule;
import com.kaushalya.digitalschool.storage.ExtractionRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExtractionRuleSeederTest {

    @Mock
    private ExtractionRuleRepository repository;

    @InjectMocks
    private ExtractionRuleSeeder seeder;

    @Test
    void seedsAllMissingFieldsWithDefaults() {
        when(repository.existsById(anyString())).thenReturn(false);

        seeder.run(null);

        for (ExtractionField field : ExtractionField.values()) {
            verify(repository).save(argThat((ExtractionRule r) ->
                    r.getKey().equals(field.key())
                            && r.getRules().equals(field.defaultRules())
                            && r.isEnabled()));
        }
    }

    @Test
    void skipsFieldsThatAlreadyExist() {
        when(repository.existsById(anyString())).thenAnswer(invocation ->
                "classification".equals(invocation.getArgument(0)));

        seeder.run(null);

        verify(repository, never()).save(argThat((ExtractionRule r) ->
                r.getKey().equals("classification")));
    }
}
