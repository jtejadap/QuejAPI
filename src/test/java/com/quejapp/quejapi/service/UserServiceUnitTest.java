package com.quejapp.quejapi.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import com.quejapp.quejapi.model.Complaint;
import com.quejapp.quejapi.model.User;
import com.quejapp.quejapi.repository.ComplaintRepository;
import com.quejapp.quejapi.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserService - Pruebas Unitarias")
class UserServiceUnitTest {

    @Mock
    private ComplaintRepository complaintsRepo;

    @Mock
    private UserRepository usersRepo;

    @Mock
    private WekaService wekaService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Complaint testComplaint;

    @BeforeEach
    void setUp() {
        // Configurar usuario de prueba
        testUser = new User();
        testUser.setId("user123");
        testUser.setFirstname("Juan");
        testUser.setLastname("Pérez");
        testUser.setEmail("juan.perez@test.com");

        // Configurar queja de prueba
        testComplaint = new Complaint();
        testComplaint.setType(1);
        testComplaint.setSubject("Asunto de prueba");
        testComplaint.setDescription("Descripción de prueba");

        // Configurar contexto de seguridad
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("juan.perez@test.com");
    }

    @Test
    @DisplayName("Crear queja - Usuario existe")
    void testCreateComplaintForUser_UserExists() throws Exception {
        // Arrange
        when(usersRepo.findByEmail("juan.perez@test.com")).thenReturn(Optional.of(testUser));
        when(wekaService.predict(anyString(), anyString(), anyString(), anyString(), eq(true)))
            .thenReturn("Medio");
        when(complaintsRepo.save(any(Complaint.class))).thenAnswer(invocation -> {
            Complaint saved = invocation.getArgument(0);
            saved.setId("complaint123");
            return saved;
        });

        // Act
        Complaint result = userService.createComplaintForUser(testComplaint);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getStatus());
        assertEquals("user123", result.getUser());
        assertNotNull(result.getReference());
        assertTrue(result.getReference().startsWith("TRC-"));
        assertNotNull(result.getRecievedDate());
        assertNotNull(result.getTraceability());
        assertEquals(1, result.getTraceability().size());
        assertEquals("PQRS Radicada", result.getTraceability().get(0).getStatus());
        assertNotNull(result.getUserProfile());
        assertEquals("Juan", result.getUserProfile().getName());
        assertEquals("Medio", result.getPrediction());

        verify(usersRepo).findByEmail("juan.perez@test.com");
        verify(complaintsRepo).save(any(Complaint.class));
        verify(wekaService).predict(anyString(), anyString(), anyString(), anyString(), eq(true));
    }

    @Test
    @DisplayName("Crear queja - Usuario no encontrado")
    void testCreateComplaintForUser_UserNotFound() {
        // Arrange
        when(usersRepo.findByEmail("juan.perez@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.createComplaintForUser(testComplaint);
        });

        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(usersRepo).findByEmail("juan.perez@test.com");
        verify(complaintsRepo, never()).save(any(Complaint.class));
    }

    @Test
    @DisplayName("Crear queja - Error en predicción")
    void testCreateComplaintForUser_PredictionError() throws Exception {
        // Arrange
        when(usersRepo.findByEmail("juan.perez@test.com")).thenReturn(Optional.of(testUser));
        when(wekaService.predict(anyString(), anyString(), anyString(), anyString(), eq(true)))
            .thenThrow(new Exception("Error en Weka"));
        when(complaintsRepo.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Complaint result = userService.createComplaintForUser(testComplaint);

        // Assert
        assertNotNull(result);
        assertEquals("Desconocido", result.getPrediction());
        verify(wekaService).predict(anyString(), anyString(), anyString(), anyString(), eq(true));
    }

    @Test
    @DisplayName("Obtener queja por ID - Existe")
    void testGetComplaintBy_Exists() {
        // Arrange
        testComplaint.setId("complaint123");
        testComplaint.setUser("user123");
        when(complaintsRepo.findById("complaint123")).thenReturn(Optional.of(testComplaint));
        when(usersRepo.findById("user123")).thenReturn(Optional.of(testUser));

        // Act
        Complaint result = userService.getComplaintBy("complaint123");

        // Assert
        assertNotNull(result);
        assertEquals("complaint123", result.getId());
        verify(complaintsRepo).findById("complaint123");
        verify(usersRepo).findById("user123");
    }

    @Test
    @DisplayName("Obtener queja por ID - No existe")
    void testGetComplaintBy_NotFound() {
        // Arrange
        when(complaintsRepo.findById("complaint999")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.getComplaintBy("complaint999");
        });

        assertTrue(exception.getMessage().contains("Queja no encontrada"));
        verify(complaintsRepo).findById("complaint999");
        verify(usersRepo, never()).findById(anyString());
    }

    @Test
    @DisplayName("Buscar quejas - Con término de búsqueda y estado")
    void testSearchComplaints_WithSearchTermAndStatus() {
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

        when(usersRepo.findByEmail("juan.perez@test.com")).thenReturn(Optional.of(testUser));
        when(complaintsRepo.findByReferenceOrSubjectContainingIgnoreCaseAndStatus(
            eq("test"), eq(1), eq("user123"), any(Pageable.class)))
            .thenReturn(page);

        // Act
        Page<Complaint> result = userService.searchComplaints(request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(complaintsRepo).findByReferenceOrSubjectContainingIgnoreCaseAndStatus(
            eq("test"), eq(1), eq("user123"), any(Pageable.class));
    }

    @Test
    @DisplayName("Buscar quejas - Solo término de búsqueda")
    void testSearchComplaints_OnlySearchTerm() {
        // Arrange
        ComplaintSearch request = new ComplaintSearch();
        request.setSearchTerm("test");
        request.setPage(0);
        request.setSize(10);
        request.setSortBy("recievedDate");
        request.setSortDirection("desc");

        List<Complaint> complaints = new ArrayList<>();
        Page<Complaint> page = new PageImpl<>(complaints);

        when(usersRepo.findByEmail("juan.perez@test.com")).thenReturn(Optional.of(testUser));
        when(complaintsRepo.findByReferenceOrSubjectContainingIgnoreCase(
            eq("test"), eq("user123"), any(Pageable.class)))
            .thenReturn(page);

        // Act
        Page<Complaint> result = userService.searchComplaints(request);

        // Assert
        assertNotNull(result);
        verify(complaintsRepo).findByReferenceOrSubjectContainingIgnoreCase(
            eq("test"), eq("user123"), any(Pageable.class));
    }

    @Test
    @DisplayName("Buscar quejas - Solo estado")
    void testSearchComplaints_OnlyStatus() {
        // Arrange
        ComplaintSearch request = new ComplaintSearch();
        request.setStatus(2);
        request.setPage(0);
        request.setSize(10);
        request.setSortBy("recievedDate");
        request.setSortDirection("desc");

        List<Complaint> complaints = new ArrayList<>();
        Page<Complaint> page = new PageImpl<>(complaints);

        when(usersRepo.findByEmail("juan.perez@test.com")).thenReturn(Optional.of(testUser));
        when(complaintsRepo.findByStatus(eq(2), eq("user123"), any(Pageable.class)))
            .thenReturn(page);

        // Act
        Page<Complaint> result = userService.searchComplaints(request);

        // Assert
        assertNotNull(result);
        verify(complaintsRepo).findByStatus(eq(2), eq("user123"), any(Pageable.class));
    }

    @Test
    @DisplayName("Buscar quejas - Sin filtros")
    void testSearchComplaints_NoFilters() {
        // Arrange
        ComplaintSearch request = new ComplaintSearch();
        request.setPage(0);
        request.setSize(10);
        request.setSortBy("recievedDate");
        request.setSortDirection("desc");

        List<Complaint> complaints = new ArrayList<>();
        Page<Complaint> page = new PageImpl<>(complaints);

        when(usersRepo.findByEmail("juan.perez@test.com")).thenReturn(Optional.of(testUser));
        when(complaintsRepo.findByUserOrderByRecievedDateDesc(eq("user123"), any(Pageable.class)))
            .thenReturn(page);

        // Act
        Page<Complaint> result = userService.searchComplaints(request);

        // Assert
        assertNotNull(result);
        verify(complaintsRepo).findByUserOrderByRecievedDateDesc(eq("user123"), any(Pageable.class));
    }

    @Test
    @DisplayName("Validar formato de referencia")
    void testReferenceFormat() throws Exception {
        // Arrange
        when(usersRepo.findByEmail("juan.perez@test.com")).thenReturn(Optional.of(testUser));
        when(wekaService.predict(anyString(), anyString(), anyString(), anyString(), eq(true)))
            .thenReturn("Medio");
        when(complaintsRepo.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Complaint result = userService.createComplaintForUser(testComplaint);

        // Assert
        assertNotNull(result.getReference());
        assertTrue(result.getReference().matches("TRC-\\d{8}-[A-Z0-9]{6}-\\d+"));
    }
}

