package cz.cas.lib.bankid_registrator.dao.oracle;

import cz.cas.lib.bankid_registrator.configurations.AlephServiceConfig;
import cz.cas.lib.bankid_registrator.configurations.MainConfiguration;
import cz.cas.lib.bankid_registrator.exceptions.AmbiguousPatronMatchException;
import cz.cas.lib.bankid_registrator.entities.entity.Address;
import cz.cas.lib.bankid_registrator.entities.entity.AddressType;
import cz.cas.lib.bankid_registrator.model.patron.Patron;
import cz.cas.lib.bankid_registrator.product.Connect;
import cz.cas.lib.bankid_registrator.product.Identify;
import cz.cas.lib.bankid_registrator.services.ProductionAlephService;
import java.util.Arrays;
import java.util.Collections;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PatronMatchingTest {
    private final EntityManager em = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final AlephServiceConfig config = mock(AlephServiceConfig.class);
    private final OracleRepository repository = new OracleRepository();

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(repository, "entityManager", em);
        ReflectionTestUtils.setField(repository, "alephServiceConfig", config);
        when(config.getPatronidPrefixes()).thenReturn(new String[]{"KNBD"});
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(
            new Object[]{"KNBD12345   ", "Popovič Karenina Eva"}));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Popovič karenina Eva", "POPOVIČ KARENINA EVA",
        "  Popovič  Karenina\tEva  ", "Popovič\u00a0Karenina\u202fEva",
        "Popovic\u030c Karenina Eva"})
    void formattingDifferencesResolveSamePatron(String name) {
        assertEquals("KNBD12345", repository.getPatronIdByNameAndBirth(name, "20010101").orElseThrow());
        assertEquals(1, repository.getPatronRowsCount(name, "20010101"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Popovic Karenina Eva", "Popovič Eva Karenina",
        "Popovič Karenina-Eva", "Popovič Karina Eva"})
    void substantiveDifferencesDoNotMatch(String name) {
        assertFalse(repository.getPatronIdByNameAndBirth(name, "20010101").isPresent());
        assertEquals(0, repository.getPatronRowsCount(name, "20010101"));
    }

    @Test
    void duplicateRowsAreOnePatronButDistinctIdsAreAmbiguous() {
        when(query.getResultList()).thenReturn(Arrays.asList(
            new Object[]{"KNBD12345  ", "Popovič Karenina Eva"},
            new Object[]{"KNBD12345", "POPOVIČ KARENINA EVA"}));
        assertEquals("KNBD12345", repository.getPatronIdByNameAndBirth("Popovič Karenina Eva", "20010101").orElseThrow());
        when(query.getResultList()).thenReturn(Arrays.asList(
            new Object[]{"KNBD12345", "Popovič Karenina Eva"},
            new Object[]{"KNBD00999", "POPOVIČ KARENINA EVA"}));
        assertThrows(AmbiguousPatronMatchException.class,
            () -> repository.getPatronIdByNameAndBirth("Popovič Karenina Eva", "20010101"));
        assertEquals(2, repository.getPatronRowsCount("Popovič Karenina Eva", "20010101"));
    }

    @Test
    void birthDateRemainsAnExactDatabaseRestriction() {
        when(query.getResultList()).thenReturn(Collections.emptyList());
        assertFalse(repository.getPatronIdByNameAndBirth("Popovič Karenina Eva", "19800102").isPresent());
        verify(query).setParameter("birthDate", "19800102");
        verify(em).createNativeQuery(argThat(sql -> sql.contains("A.Z303_BIRTH_DATE = :birthDate")
            && sql.contains("KNA50.Z305") && sql.contains("KNA50.Z308") && !sql.contains("ROWNUM")));
    }

    @Test
    void failedLookupAndMissingIdentityDataCannotBecomeNewRegistration() {
        when(query.getResultList()).thenThrow(new IllegalStateException("Oracle unavailable"));
        assertThrows(IllegalStateException.class,
            () -> repository.getPatronIdByNameAndBirth("Popovič Karenina Eva", "20010101"));
        assertThrows(IllegalArgumentException.class,
            () -> repository.getPatronIdByNameAndBirth("Popovič Karenina Eva", null));
        assertThrows(IllegalArgumentException.class,
            () -> repository.getPatronIdByNameAndBirth(" ", "20010101"));
    }

    @Test
    void productionBankCallbackSelectsExistingAccountAndPreservesSpelling() {
        ProductionAlephService service = new ProductionAlephService(
            mock(MainConfiguration.class), config, null, repository, null);
        Connect info = mock(Connect.class);
        Identify profile = mock(Identify.class);
        when(info.getGiven_name()).thenReturn("Eva");
        when(info.getMiddle_name()).thenReturn("karenina");
        when(info.getFamily_name()).thenReturn("Popovič");
        when(info.getBirthdate()).thenReturn("1980-01-01");
        Address address = mock(Address.class);
        when(address.getType()).thenReturn(AddressType.PERMANENT_RESIDENCE);
        when(address.getCity()).thenReturn("Praha");
        when(address.getZipcode()).thenReturn("11000");
        when(profile.getAddresses()).thenReturn(new java.util.ArrayList<>(Collections.singletonList(address)));
        Patron patron = (Patron) service.newPatron(info, profile).get("patron");
        assertFalse(patron.isNew());
        assertEquals("KNBD12345", patron.getPatronId());
        assertEquals("Popovič karenina Eva", patron.getName());
    }
}
