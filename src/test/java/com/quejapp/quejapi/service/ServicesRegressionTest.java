package com.quejapp.quejapi.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.quejapp.quejapi.dto.ComplaintSearch;
import com.quejapp.quejapi.dto.ComplaintUpdate;
import com.quejapp.quejapi.model.Complaint;
import com.quejapp.quejapi.model.User;
import com.quejapp.quejapi.repository.ComplaintRepository;
import com.quejapp.quejapi.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Servicios - Pruebas de Regresión")
class ServicesRegressionTest {

    @Autowired
    private UserService userService;

    @Autowired
    private AdministrationService adminService;

    @Autowired
    private WekaService wekaService;

    @Autowired
    private UserRepository usersRepo;

    @Autowired
    private ComplaintRepository complaintsRepo;

    private User testUser;
    private User testEmployee;

    @BeforeEach
    void setUp() {
        // Limpiar datos
        complaintsRepo.deleteAll();
        usersRepo.deleteAll();

        // Crear usuario de prueba
        testUser = new User();
        testUser.setFirstname("Test");
        testUser.setLastname("User");
        testUser.setEmail("test.user@regression.com");
        testUser.setPassword("password123");
        testUser = usersRepo.save(testUser);

        // Crear empleado de prueba
        testEmployee = new User();
        testEmployee.setFirstname("Admin");
        testEmployee.setLastname("Employee");
        testEmployee.setEmail("admin@regression.com");
        testEmployee.setPassword("admin123");
        testEmployee = usersRepo.save(testEmployee);

        // Configurar autenticación
        Authentication auth = new UsernamePasswordAuthenticationToken(
            testUser.getEmail(), null, new ArrayList<>()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }


    @Test
    @DisplayName("Regresión: Flujo completo de creación de queja")
    void testCompleteComplaintCreationFlow() {
        // Arrange
        Complaint complaint = new Complaint();
        complaint.setType(1);
        complaint.setSubject("Queja de regresión");
        complaint.setDescription("Descripción de prueba de regresión");

        // Act
        Complaint created = userService.createComplaintForUser(complaint);

        // Assert
        assertNotNull(created.getId());
        assertEquals(0, created.getStatus());
        assertEquals(testUser.getId(), created.getUser());
        assertNotNull(created.getReference());
        assertTrue(created.getReference().startsWith("TRC-"));
        assertNotNull(created.getUserProfile());
        assertEquals("Test", created.getUserProfile().getName());
        assertNotNull(created.getTraceability());
        assertEquals(1, created.getTraceability().size());
        assertEquals("PQRS Radicada", created.getTraceability().get(0).getStatus());

        // Verificar persistencia
        Optional<Complaint> found = complaintsRepo.findById(created.getId());
        assertTrue(found.isPresent());
        assertEquals(created.getReference(), found.get().getReference());
    }

    @Test
    @DisplayName("Regresión: Múltiples quejas del mismo usuario")
    void testMultipleComplaintsFromSameUser() {
        // Arrange & Act
        Complaint complaint1 = createComplaint(0, "Primera queja");
        Complaint complaint2 = createComplaint(1, "Segunda queja");
        Complaint complaint3 = createComplaint(2, "Tercera queja");

        // Assert
        assertNotEquals(complaint1.getReference(), complaint2.getReference());
        assertNotEquals(complaint2.getReference(), complaint3.getReference());

        ComplaintSearch search = new ComplaintSearch();
        search.setPage(0);
        search.setSize(10);
        search.setSortBy("recievedDate");
        search.setSortDirection("desc");

        Page<Complaint> results = userService.searchComplaints(search);
        assertEquals(3, results.getTotalElements());
    }

    @Test
    @DisplayName("Regresión: Búsqueda con filtros combinados")
    void testSearchWithCombinedFilters() {
        // Arrange
        Complaint c1 = createComplaint(0, "Búsqueda test A");
        Complaint c2 = createComplaint(1, "Búsqueda test B");
        Complaint c3 = createComplaint(0, "Otra queja C");

        // Cambiar estado de c1
        changeComplaintStatus(c1.getId(), 1);

        // Act: Buscar por término y estado
        ComplaintSearch search = new ComplaintSearch();
        search.setSearchTerm("test");
        search.setStatus(1);
        search.setPage(0);
        search.setSize(10);
        search.setSortBy("recievedDate");
        search.setSortDirection("desc");

        Page<Complaint> results = userService.searchComplaints(search);

        // Assert
        assertEquals(1, results.getTotalElements());
        assertTrue(results.getContent().get(0).getSubject().contains("test"));
        assertEquals(1, results.getContent().get(0).getStatus());
    }

    @Test
    @DisplayName("Regresión: Actualización completa de queja a estado Resuelto")
    void testCompleteComplaintUpdateToResolved() {
        // Arrange
        Complaint complaint = createComplaint(0, "Queja para resolver");
        
        // Cambiar autenticación a empleado
        Authentication auth = new UsernamePasswordAuthenticationToken(
            testEmployee.getEmail(), null, new ArrayList<>()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        ComplaintUpdate update = new ComplaintUpdate();
        update.setId(complaint.getId());
        update.setStatus(2);
        update.setResponse("Problema resuelto satisfactoriamente");
        update.setDepartment("Soporte Técnico");

        // Act
        Complaint updated = adminService.updateComplaint(update);

        // Assert
        assertEquals(2, updated.getStatus());
        assertEquals("Problema resuelto satisfactoriamente", updated.getResponse());
        assertEquals(testEmployee.getId(), updated.getEmployee());
        assertNotNull(updated.getEmployeeProfile());
        assertEquals("Admin", updated.getEmployeeProfile().getName());
        assertEquals("Soporte Técnico", updated.getDepartment());
        assertNotNull(updated.getResponseDate());
        assertNotNull(updated.getUpdatedDate());
        assertNotNull(updated.getDaysToResolve());
        assertTrue(updated.getDaysToResolve() >= 0);

        // Verificar trazabilidad
        assertTrue(updated.getTraceability().size() >= 2);
        assertTrue(updated.getTraceability().stream()
            .anyMatch(t -> t.getStatus().contains("Resuelto")));
    }

    @Test
    @DisplayName("Regresión: Cambio de estado de Nuevo a En Proceso y luego a Resuelto")
    void testStatusProgressionFlow() {
        // Arrange
        Complaint complaint = createComplaint(0, "Flujo de estados");
        
        Authentication auth = new UsernamePasswordAuthenticationToken(
            testEmployee.getEmail(), null, new ArrayList<>()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act 1: Cambiar a En Proceso
        ComplaintUpdate update1 = new ComplaintUpdate();
        update1.setId(complaint.getId());
        update1.setStatus(1);
        Complaint inProcess = adminService.updateComplaint(update1);

        // Assert 1
        assertEquals(1, inProcess.getStatus());
        assertNull(inProcess.getResponse());
        assertNull(inProcess.getResponseDate());

        // Act 2: Cambiar a Resuelto
        ComplaintUpdate update2 = new ComplaintUpdate();
        update2.setId(complaint.getId());
        update2.setStatus(2);
        update2.setResponse("Resuelto después de procesamiento");
        update2.setDepartment("IT");
        Complaint resolved = adminService.updateComplaint(update2);

        // Assert 2
        assertEquals(2, resolved.getStatus());
        assertEquals("Resuelto después de procesamiento", resolved.getResponse());
        assertNotNull(resolved.getResponseDate());
        assertEquals("IT", resolved.getDepartment());
        assertTrue(resolved.getTraceability().size() >= 3);
    }

    @Test
    @DisplayName("Regresión: Actualización sin cambio de estado mantiene datos")
    void testUpdateWithoutStatusChange() {
        // Arrange
        Complaint complaint = createComplaint(1, "Queja en proceso");
        
        Authentication auth = new UsernamePasswordAuthenticationToken(
            testEmployee.getEmail(), null, new ArrayList<>()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        ComplaintUpdate update = new ComplaintUpdate();
        update.setId(complaint.getId());
        update.setStatus(2);
        update.setResponse("Actualización de respuesta");
        update.setDepartment("Ventas");
        Complaint updated = adminService.updateComplaint(update);

        // Act       
        update.setStatus(2);
        update.setResponse("Actualización de respuesta -v2");
        updated = adminService.updateComplaint(update);

        // Assert
        assertEquals(2, updated.getStatus());
        assertEquals("Actualización de respuesta -v2", updated.getResponse());
        assertEquals("Ventas", updated.getDepartment());
        assertEquals(testEmployee.getId(), updated.getEmployee());
    }

    @Test
    @DisplayName("Regresión: Rollback de estado limpia campos de respuesta")
    void testStatusRollbackClearsResponseFields() {
        // Arrange
        Complaint complaint = createComplaint(2, "Queja resuelta");
        
        // Simular que fue resuelta
        Authentication auth = new UsernamePasswordAuthenticationToken(
            testEmployee.getEmail(), null, new ArrayList<>()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        ComplaintUpdate resolveUpdate = new ComplaintUpdate();
        resolveUpdate.setId(complaint.getId());
        resolveUpdate.setStatus(2);
        resolveUpdate.setResponse("Respuesta inicial");
        resolveUpdate.setDepartment("IT");
        Complaint resolved = adminService.updateComplaint(resolveUpdate);

        // Act: Volver a estado En Proceso
        ComplaintUpdate rollbackUpdate = new ComplaintUpdate();
        rollbackUpdate.setId(complaint.getId());
        rollbackUpdate.setStatus(1);
        Complaint rolledBack = adminService.updateComplaint(rollbackUpdate);

        // Assert
        assertEquals(1, rolledBack.getStatus());
        assertNull(rolledBack.getResponse());
        assertNull(rolledBack.getEmployee());
        assertNull(rolledBack.getEmployeeProfile());
        assertNull(rolledBack.getDepartment());
        assertNull(rolledBack.getResponseDate());
        assertNull(rolledBack.getDaysToResolve());
        assertNull(rolledBack.getDaysfromExpiration());
    }

    @Test
    @DisplayName("Regresión: Referencias únicas por queja")
    void testUniqueReferencesPerComplaint() {
        // Arrange & Act
        Complaint c1 = createComplaint(0, "Ref test 1");
        Complaint c2 = createComplaint(0, "Ref test 2");
        Complaint c3 = createComplaint(1, "Ref test 3");

        // Assert
        assertNotEquals(c1.getReference(), c2.getReference());
        assertNotEquals(c2.getReference(), c3.getReference());
        assertNotEquals(c1.getReference(), c3.getReference());

        // Verificar formato
        assertTrue(c1.getReference().matches("TRC-\\d{8}-[A-Z0-9]{6}-\\d+"));
        assertTrue(c2.getReference().matches("TRC-\\d{8}-[A-Z0-9]{6}-\\d+"));
        assertTrue(c3.getReference().matches("TRC-\\d{8}-[A-Z0-9]{6}-\\d+"));
    }

    @Test
    @DisplayName("Regresión: Paginación funciona correctamente")
    void testPaginationWorks() {
        // Arrange: Crear 15 quejas
        for (int i = 0; i < 15; i++) {
            createComplaint(i % 3, "Queja paginación " + i);
        }

        // Act: Primera página
        ComplaintSearch search1 = new ComplaintSearch();
        search1.setPage(0);
        search1.setSize(5);
        search1.setSortBy("recievedDate");
        search1.setSortDirection("desc");
        Page<Complaint> page1 = userService.searchComplaints(search1);

        // Act: Segunda página
        ComplaintSearch search2 = new ComplaintSearch();
        search2.setPage(1);
        search2.setSize(5);
        search2.setSortBy("recievedDate");
        search2.setSortDirection("desc");
        Page<Complaint> page2 = userService.searchComplaints(search2);

        // Assert
        assertEquals(15, page1.getTotalElements());
        assertEquals(3, page1.getTotalPages());
        assertEquals(5, page1.getContent().size());
        assertEquals(5, page2.getContent().size());
        
        // Verificar que no hay duplicados
        String ref1 = page1.getContent().get(0).getReference();
        String ref2 = page2.getContent().get(0).getReference();
        assertNotEquals(ref1, ref2);
    }

    @Test
    @DisplayName("Regresión: Ordenamiento ascendente y descendente")
    void testSortingAscendingAndDescending() {
        // Arrange
        Complaint c1 = createComplaint(0, "AAA Primera");
        sleep(100);
        Complaint c2 = createComplaint(1, "BBB Segunda");
        sleep(100);
        Complaint c3 = createComplaint(2, "CCC Tercera");

        // Act: Ordenar descendente
        ComplaintSearch searchDesc = new ComplaintSearch();
        searchDesc.setPage(0);
        searchDesc.setSize(10);
        searchDesc.setSortBy("recievedDate");
        searchDesc.setSortDirection("desc");
        Page<Complaint> descResults = userService.searchComplaints(searchDesc);

        // Act: Ordenar ascendente
        ComplaintSearch searchAsc = new ComplaintSearch();
        searchAsc.setPage(0);
        searchAsc.setSize(10);
        searchAsc.setSortBy("recievedDate");
        searchAsc.setSortDirection("asc");
        Page<Complaint> ascResults = userService.searchComplaints(searchAsc);

        // Assert
        assertEquals(c3.getId(), descResults.getContent().get(0).getId());
        assertEquals(c1.getId(), ascResults.getContent().get(0).getId());
    }

    @Test
    @DisplayName("Regresión: Cálculo de métricas de tiempo")
    void testTimeMetricsCalculation() throws InterruptedException {
        // Arrange
        Date pastDate = new Date(System.currentTimeMillis() - (20 * 24 * 60 * 60 * 1000L));
        Complaint complaint = createComplaint(0, "Métrica tiempo");
        
        // Forzar fecha pasada
        complaint.setRecievedDate(pastDate);
        complaintsRepo.save(complaint);

        Authentication auth = new UsernamePasswordAuthenticationToken(
            testEmployee.getEmail(), null, new ArrayList<>()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        ComplaintUpdate update = new ComplaintUpdate();
        update.setId(complaint.getId());
        update.setStatus(2);
        update.setResponse("Respuesta con métricas");
        update.setDepartment("IT");
        Complaint resolved = adminService.updateComplaint(update);

        // Assert
        assertNotNull(resolved.getDaysToResolve());
        assertNotNull(resolved.getDaysfromExpiration());
        assertTrue(resolved.getDaysToResolve() >= 19); // ~20 días
        assertTrue(resolved.getDaysfromExpiration() > 0); // Pasó de los 15 días
    }

    // Métodos auxiliares
    private Complaint createComplaint(int type, String subject) {
        Complaint complaint = new Complaint();
        complaint.setType(type);
        complaint.setSubject(subject);
        complaint.setDescription("Descripción: " + subject);
        return userService.createComplaintForUser(complaint);
    }

    private void changeComplaintStatus(String complaintId, int newStatus) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
            testEmployee.getEmail(), null, new ArrayList<>()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        ComplaintUpdate update = new ComplaintUpdate();
        update.setId(complaintId);
        update.setStatus(newStatus);
        adminService.updateComplaint(update);

        // Restaurar autenticación del usuario
        auth = new UsernamePasswordAuthenticationToken(
            testUser.getEmail(), null, new ArrayList<>()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
