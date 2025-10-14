package com.quejapp.quejapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PQRSStatisticsDTO {
    
    // Estadísticas generales
    private Long totalPqrs;
    private Long openPqrs;
    private Long closedPqrs;
    private Long inProgressPqrs;
    
    // Estadísticas por estado
    private Map<String, Long> pqrsByStatus;
    
    // Estadísticas por tipo
    private Map<String, Long> pqrsByType;
    
    // Estadísticas por categoría
    private Map<String, Long> pqrsByCategory;
    
    // Tiempos de resolución
    private Double averageDaysToResolve;
    private Integer minDaysToResolve;
    private Integer maxDaysToResolve;
    
    // Estadísticas de vencimiento
    private Long expiredPqrs;
    private Long nearExpirationPqrs; // Próximas a vencer (menos de 3 días)
    private Double averageDaysFromExpiration;
    
    // Estadísticas por empleado
    private List<EmployeeStatsDTO> topEmployees;
    
    // Tendencias mensuales
    private List<MonthlyTrendDTO> monthlyTrends;
    
    // Tasas de rendimiento
    private Double resolutionRate; // Porcentaje de PQRS resueltas
    private Double satisfactionRate; // Basado en tiempo de respuesta
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeStatsDTO {
        private String employeeId;
        private String employeeName;
        private String employeeEmail;
        private Long totalAssigned;
        private Long totalResolved;
        private Double averageResolutionTime;
        private Double resolutionRate;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyTrendDTO {
        private String month;
        private Long totalReceived;
        private Long totalResolved;
        private Double averageResolutionTime;
    }
}