package com.quejapp.quejapi.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.quejapp.quejapi.dto.ComplaintSearch;
import com.quejapp.quejapi.dto.ComplaintUpdate;
import com.quejapp.quejapi.model.Complaint;
import com.quejapp.quejapi.model.Trace;
import com.quejapp.quejapi.model.User;
import com.quejapp.quejapi.repository.ComplaintRepository;
import com.quejapp.quejapi.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdministrationService - Pruebas Unitarias")
class AdministrationServiceUnitTest {

    @Mock
    private ComplaintRepository complaintsRepo;

    @Mock
    private UserRepository usersRepo;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AdministrationService adminService;

    private User testEmployee;
    private Complaint testComplaint;
    private ComplaintUpdate testUpdate;

    @BeforeEach
    void setUp() {
        testEmployee = new User();
        testEmployee.setId("emp123");
        testEmployee.setFirstname("Admin");
        testEmployee.setLastname("User");
        testEmployee.setEmail("admin@test.com");

        testComplaint = new Complaint();
        testComplaint.setId("complaint123");
        testComplaint.setStatus(0);
        testComplaint.setRecievedDate(new Date());
        testComplaint.setTraceability(new ArrayList<>());

        testUpdate = new ComplaintUpdate();
        testUpdate.setId("complaint123");
        testUpdate.setStatus(2);
        testUpdate.setResponse("Respuesta de prueba");
        testUpdate.setDepartment("IT");

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("admin@test.com");
    }

    @Test
    @DisplayName("Buscar quejas - Administrador con todos los filtros")
    void testSearchComplaints_WithAllFilters() {
        // Arrange
        ComplaintSearch request = new ComplaintSearch();
        request.setSearchTerm("test");
        request.setStatus(1);
        request.setPage(0);
        request.setSize(10);
        request.setSortBy("recievedDate");
        request.setSortDirection("desc");

        List<Complaint> complaints = new ArrayList<>();
        complaints.add(testComplaint);
        Page<Complaint> page = new PageImpl<>(complaints);

        when(complaintsRepo.findByReferenceOrSubjectContainingIgnoreCaseAndStatus(
            eq("test"), eq(1), any(Pageable.class)))
            .thenReturn(page);

        // Act
        Page<Complaint> result = adminService.searchComplaints(request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(complaintsRepo).findByReferenceOrSubjectContainingIgnoreCaseAndStatus(
            eq("test"), eq(1), any(Pageable.class));
    }

    @Test
    @DisplayName("Buscar quejas - Sin filtros retorna todas")
    void testSearchComplaints_NoFilters() {
        // Arrange
        ComplaintSearch request = new ComplaintSearch();
        request.setPage(0);
        request.setSize(10);
        request.setSortBy("recievedDate");
        request.setSortDirection("desc");

        List<Complaint> complaints = new ArrayList<>();
        Page<Complaint> page = new PageImpl<>(complaints);

        when(complaintsRepo.findAllByOrderByRecievedDateDesc(any(Pageable.class)))
            .thenReturn(page);

        // Act
        Page<Complaint> result = adminService.searchComplaints(request);

        // Assert
        assertNotNull(result);
        verify(complaintsRepo).findAllByOrderByRecievedDateDesc(any(Pageable.class));
    }

    @Test
    @DisplayName("Obtener queja por ID")
    void testGetComplaintBy() {
        // Arrange
        when(complaintsRepo.findById("complaint123")).thenReturn(Optional.of(testComplaint));

        // Act
        Optional<Complaint> result = adminService.getComplaintBy("complaint123");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("complaint123", result.get().getId());
        verify(complaintsRepo).findById("complaint123");
    }

    @Test
    @DisplayName("Actualizar queja - Cambio a estado Resuelto")
    void testUpdateComplaint_ToResolved() {
        // Arrange
        when(usersRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(testEmployee));
        when(complaintsRepo.findById("complaint123")).thenReturn(Optional.of(testComplaint));
        when(complaintsRepo.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Complaint result = adminService.updateComplaint(testUpdate);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getStatus());
        assertEquals("Respuesta de prueba", result.getResponse());
        assertEquals("emp123", result.getEmployee());
        assertNotNull(result.getEmployeeProfile());
        assertEquals("Admin", result.getEmployeeProfile().getName());
        assertEquals("IT", result.getDepartment());
        assertNotNull(result.getResponseDate());
        assertNotNull(result.getUpdatedDate());
        assertNotNull(result.getDaysToResolve());
        assertTrue(result.getTraceability().size() > 0);

        verify(complaintsRepo).save(any(Complaint.class));
    }

    @Test
    @DisplayName("Actualizar queja - Cambio a estado En Proceso limpia campos")
    void testUpdateComplaint_ToInProcess() {
        // Arrange
        testComplaint.setResponse("Respuesta anterior");
        testComplaint.setEmployee("emp999");
        testComplaint.setDepartment("HR");
        
        testUpdate.setStatus(1);
        testUpdate.setResponse(null);

        when(usersRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(testEmployee));
        when(complaintsRepo.findById("complaint123")).thenReturn(Optional.of(testComplaint));
        when(complaintsRepo.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Complaint result = adminService.updateComplaint(testUpdate);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getStatus());
        assertNull(result.getResponse());
        assertNull(result.getEmployee());
        assertNull(result.getEmployeeProfile());
        assertNull(result.getDepartment());
        assertNull(result.getResponseDate());
        assertNull(result.getDaysToResolve());
        assertNull(result.getDaysfromExpiration());

        verify(complaintsRepo).save(any(Complaint.class));
    }

    @Test
    @DisplayName("Actualizar queja - Solo respuesta sin cambio de estado")
    void testUpdateComplaint_ResponseOnlyNoStatusChange() {
        // Arrange
        testComplaint.setStatus(1);
        testUpdate.setStatus(1);

        when(usersRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(testEmployee));
        when(complaintsRepo.findById("complaint123")).thenReturn(Optional.of(testComplaint));
        when(complaintsRepo.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Complaint result = adminService.updateComplaint(testUpdate);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getStatus());
        assertEquals("Respuesta de prueba", result.getResponse());
        assertEquals("emp123", result.getEmployee());
        assertNotNull(result.getEmployeeProfile());
        assertEquals("IT", result.getDepartment());

        verify(complaintsRepo).save(any(Complaint.class));
    }

    @Test
    @DisplayName("Actualizar queja - Usuario no encontrado")
    void testUpdateComplaint_EmployeeNotFound() {
        // Arrange
        when(usersRepo.findByEmail("admin@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adminService.updateComplaint(testUpdate);
        });

        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(usersRepo).findByEmail("admin@test.com");
        verify(complaintsRepo, never()).save(any(Complaint.class));
    }

    @Test
    @DisplayName("Actualizar queja - Queja no encontrada")
    void testUpdateComplaint_ComplaintNotFound() {
        // Arrange
        when(usersRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(testEmployee));
        when(complaintsRepo.findById("complaint123")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adminService.updateComplaint(testUpdate);
        });

        assertEquals("Queja no encontrada", exception.getMessage());
        verify(complaintsRepo, never()).save(any(Complaint.class));
    }

    @Test
    @DisplayName("Calcular días desde expiración - Dentro del período")
    void testCalculateDaysFromExpiration_WithinPeriod() throws Exception {
        // Arrange
        Date recentDate = new Date(System.currentTimeMillis() - (10 * 24 * 60 * 60 * 1000L)); // 10 días atrás
        testComplaint.setRecievedDate(recentDate);
        testUpdate.setStatus(2);

        when(usersRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(testEmployee));
        when(complaintsRepo.findById("complaint123")).thenReturn(Optional.of(testComplaint));
        when(complaintsRepo.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Complaint result = adminService.updateComplaint(testUpdate);

        // Assert
        assertNotNull(result.getDaysfromExpiration());
        assertEquals(0, result.getDaysfromExpiration());
    }

    @Test
    @DisplayName("Calcular días desde expiración - Fuera del período")
    void testCalculateDaysFromExpiration_BeyondPeriod() throws Exception {
        // Arrange
        Date oldDate = new Date(System.currentTimeMillis() - (20 * 24 * 60 * 60 * 1000L)); // 20 días atrás
        testComplaint.setRecievedDate(oldDate);
        testUpdate.setStatus(2);

        when(usersRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(testEmployee));
        when(complaintsRepo.findById("complaint123")).thenReturn(Optional.of(testComplaint));
        when(complaintsRepo.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Complaint result = adminService.updateComplaint(testUpdate);

        // Assert
        assertNotNull(result.getDaysfromExpiration());
        assertTrue(result.getDaysfromExpiration() >= 5); // ~5 días de retraso
    }

    @Test
    @DisplayName("Trazabilidad se actualiza correctamente")
    void testTraceabilityUpdated() {
        // Arrange
        when(usersRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(testEmployee));
        when(complaintsRepo.findById("complaint123")).thenReturn(Optional.of(testComplaint));
        when(complaintsRepo.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Complaint result = adminService.updateComplaint(testUpdate);

        // Assert
        assertNotNull(result.getTraceability());
        assertTrue(result.getTraceability().size() > 0);
        Trace lastTrace = result.getTraceability().get(result.getTraceability().size() - 1);
        assertEquals("admin@test.com", lastTrace.getPerformedBy());
        assertTrue(lastTrace.getStatus().contains("Resuelto"));
    }
}

