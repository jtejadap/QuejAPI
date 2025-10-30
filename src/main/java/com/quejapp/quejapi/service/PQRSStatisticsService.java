package com.quejapp.quejapi.service;

import com.quejapp.quejapi.dto.PQRSStatisticsDTO;
import com.quejapp.quejapi.model.Complaint;
import com.quejapp.quejapi.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PQRSStatisticsService {

        private final ComplaintRepository complaintRepository;
        private final MongoTemplate mongoTemplate;
        private int EXPIRATION_DAYS = 15;

        public PQRSStatisticsDTO getStatistics(LocalDate startDate, LocalDate endDate) {
                // Construir criterios de fecha
                Criteria dateCriteria = buildDateCriteria(startDate, endDate);

                // Obtener todas las PQRS según el filtro de fecha
                List<Complaint> complaints = getFilteredComplaints(dateCriteria);

                if (complaints.isEmpty()) {
                        return PQRSStatisticsDTO.builder()
                                        .totalPqrs(0L)
                                        .openPqrs(0L)
                                        .closedPqrs(0L)
                                        .inProgressPqrs(0L)
                                        .build();
                }

                return PQRSStatisticsDTO.builder()
                                .totalPqrs((long) complaints.size())
                                .openPqrs(countByStatus(complaints, 0))
                                .closedPqrs(countByStatus(complaints, 2, 3))
                                .inProgressPqrs(countByStatus(complaints, 1))
                                .pqrsByStatus(groupByStatus(complaints))
                                .pqrsByType(groupByType(complaints))
                                .pqrsByCategory(groupByCategory(complaints))
                                .averageDaysToResolve(calculateAverageResolutionDays(complaints))
                                .minDaysToResolve(calculateMinResolutionDays(complaints))
                                .maxDaysToResolve(calculateMaxResolutionDays(complaints))
                                .expiredPqrs(countExpired(complaints))
                                .nearExpirationPqrs(countNearExpiration(complaints))
                                .averageDaysFromExpiration(calculateAverageDaysFromExpiration(complaints))
                                .topEmployees(calculateEmployeeStats(complaints))
                                .monthlyTrends(calculateMonthlyTrends(complaints))
                                .resolutionRate(calculateResolutionRate(complaints))
                                .satisfactionRate(calculateSatisfactionRate(complaints))
                                .build();
        }

        private Criteria buildDateCriteria(LocalDate startDate, LocalDate endDate) {
                Criteria criteria = new Criteria();

                if (startDate != null && endDate != null) {
                        Date start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                        Date end = Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());
                        criteria.and("recievedDate").gte(start).lte(end);
                } else if (startDate != null) {
                        Date start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                        criteria.and("recievedDate").gte(start);
                } else if (endDate != null) {
                        Date end = Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());
                        criteria.and("recievedDate").lte(end);
                }

                return criteria;
        }

        private List<Complaint> getFilteredComplaints(Criteria dateCriteria) {
                if (dateCriteria.getCriteriaObject().isEmpty()) {
                        return complaintRepository.findAll();
                }
                return mongoTemplate.find(
                                org.springframework.data.mongodb.core.query.Query.query(dateCriteria),
                                Complaint.class);
        }

        private Long countByStatus(List<Complaint> complaints, Integer... statuses) {
                Set<Integer> statusSet = Set.of(statuses);
                return complaints.stream()
                                .filter(c -> statusSet.contains(c.getStatus()))
                                .count();
        }

        private Map<String, Long> groupByStatus(List<Complaint> complaints) {
                Map<Integer, String> statusNames = Map.of(
                                0, "Radicada",
                                1, "En Proceso",
                                2, "Resuelta",
                                3, "Cerrada");

                return complaints.stream()
                                .collect(Collectors.groupingBy(
                                                c -> statusNames.getOrDefault(c.getStatus(), "Desconocido"),
                                                Collectors.counting()));
        }

        private Map<String, Long> groupByType(List<Complaint> complaints) {
                Map<Integer, String> typeNames = Map.of(
                                0, "Petición",
                                1, "Queja",
                                2, "Reclamo",
                                3, "Sugerencia",
                                4, "Felicitación");

                return complaints.stream()
                                .collect(Collectors.groupingBy(
                                                c -> typeNames.getOrDefault(c.getType(), "Otro"),
                                                Collectors.counting()));
        }

        private Map<String, Long> groupByCategory(List<Complaint> complaints) {
                Map<Integer, String> categoryNames = Map.of(
                                0, "Servicio de Buses",
                                1, "Trarifas y Pagos",
                                2, "Horarios",
                                3, "Rutas",
                                4, "infraestructura",
                                5, "Atención al Cliente",
                                6, "Accesibilidad",
                                7, "Seguridad",
                                8, "Otros");

                return complaints.stream()
                                .collect(Collectors.groupingBy(
                                                c -> categoryNames.getOrDefault(c.getCategory(), "Sin Categoría"),
                                                Collectors.counting()));
        }

        private Double calculateAverageResolutionDays(List<Complaint> complaints) {
                return complaints.stream()
                                .filter(c -> c.getDaysToResolve() != null && isStatusClosed(c.getStatus()))
                                .mapToInt(Complaint::getDaysToResolve)
                                .average()
                                .orElse(0.0);
        }

        private boolean isStatusClosed(Integer status) {
                return status != null && (status == 3 || status == 2);
        }

        private Integer calculateMinResolutionDays(List<Complaint> complaints) {
                return complaints.stream()
                                .filter(c -> c.getDaysToResolve() != null && isStatusClosed(c.getStatus()))
                                .mapToInt(Complaint::getDaysToResolve)
                                .min()
                                .orElse(0);
        }

        private Integer calculateMaxResolutionDays(List<Complaint> complaints) {
                return complaints.stream()
                                .filter(c -> c.getDaysToResolve() != null && isStatusClosed(c.getStatus()))
                                .mapToInt(Complaint::getDaysToResolve)
                                .max()
                                .orElse(0);
        }

        private Long countExpired(List<Complaint> complaints) {
                return complaints.stream()
                                .filter(c -> isExpired(c))
                                .count();
        }

        private boolean isExpired(Complaint complaint) {
                if (complaint.getRecievedDate() == null) {
                        return false;
                }
                if (isStatusClosed(complaint.getStatus())) {
                        return false;
                }
                LocalDate recievedDate = complaint.getRecievedDate().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                LocalDate currentDate = LocalDate.now();
                int elapsedDays = (int) java.time.temporal.ChronoUnit.DAYS.between(recievedDate, currentDate);
                return elapsedDays > EXPIRATION_DAYS;
        }

        private Long countNearExpiration(List<Complaint> complaints) {
                return complaints.stream()
                                .filter(c -> c.getRecievedDate() != null && calculateElapsedDays(c))
                                .count();
        }

        private boolean calculateElapsedDays(Complaint complaint) {
                if (complaint.getRecievedDate() == null) {
                        return false;
                }
                if (isStatusClosed(complaint.getStatus())) {
                        return false;
                }
                LocalDate recievedDate = complaint.getRecievedDate().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                LocalDate currentDate = LocalDate.now();
                int elapsedDays = (int) java.time.temporal.ChronoUnit.DAYS.between(recievedDate, currentDate);
                return (EXPIRATION_DAYS - elapsedDays >= 0) && (EXPIRATION_DAYS - elapsedDays <= 3);

        }

        private Double calculateAverageDaysFromExpiration(List<Complaint> complaints) {
                return complaints.stream()
                                .filter(c -> c.getDaysfromExpiration() != null)
                                .mapToInt(Complaint::getDaysfromExpiration)
                                .average()
                                .orElse(0.0);
        }

        private List<PQRSStatisticsDTO.EmployeeStatsDTO> calculateEmployeeStats(List<Complaint> complaints) {
                Map<String, List<Complaint>> byEmployee = complaints.stream()
                                .filter(c -> c.getEmployee() != null && c.getEmployeeProfile() != null)
                                .collect(Collectors.groupingBy(Complaint::getEmployee));
                return byEmployee.entrySet().stream()
                                .map(entry -> {
                                        List<Complaint> empComplaints = entry.getValue();
                                        Complaint sample = empComplaints.get(0);

                                        long totalResolved = empComplaints.stream()
                                                        .filter(c -> isStatusClosed(c.getStatus()))
                                                        .count();

                                        double avgResTime = empComplaints.stream()
                                                        .filter(c -> c.getDaysToResolve() != null
                                                                        && isStatusClosed(c.getStatus()))
                                                        .mapToInt(Complaint::getDaysToResolve)
                                                        .average()
                                                        .orElse(0.0);

                                        double resolutionRate = empComplaints.isEmpty() ? 0.0
                                                        : (totalResolved * 100.0) / empComplaints.size();

                                        return PQRSStatisticsDTO.EmployeeStatsDTO.builder()
                                                        .employeeId(entry.getKey())
                                                        .employeeName(sample.getEmployeeProfile().getName() + " " +
                                                                        sample.getEmployeeProfile().getLastname())
                                                        .employeeEmail(sample.getEmployeeProfile().getEmail())
                                                        .totalAssigned((long) empComplaints.size())
                                                        .totalResolved(totalResolved)
                                                        .averageResolutionTime(Math.round(avgResTime * 100.0) / 100.0)
                                                        .resolutionRate(Math.round(resolutionRate * 100.0) / 100.0)
                                                        .build();
                                })
                                .sorted(Comparator.comparing(PQRSStatisticsDTO.EmployeeStatsDTO::getTotalResolved)
                                                .reversed())
                                .limit(10)
                                .collect(Collectors.toList());
        }

        private List<PQRSStatisticsDTO.MonthlyTrendDTO> calculateMonthlyTrends(List<Complaint> complaints) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

                Map<String, List<Complaint>> byMonth = complaints.stream()
                                .collect(Collectors.groupingBy(c -> {
                                        LocalDateTime dateTime = c.getRecievedDate().toInstant()
                                                        .atZone(ZoneId.systemDefault()).toLocalDateTime();
                                        return dateTime.format(formatter);
                                }));

                return byMonth.entrySet().stream()
                                .map(entry -> {
                                        List<Complaint> monthComplaints = entry.getValue();
                                        long resolved = monthComplaints.stream()
                                                        .filter(c -> isStatusClosed(c.getStatus()))
                                                        .count();

                                        double avgResTime = monthComplaints.stream()
                                                        .filter(c -> c.getDaysToResolve() != null
                                                                        && isStatusClosed(c.getStatus()))
                                                        .mapToInt(Complaint::getDaysToResolve)
                                                        .average()
                                                        .orElse(0.0);

                                        return PQRSStatisticsDTO.MonthlyTrendDTO.builder()
                                                        .month(entry.getKey())
                                                        .totalReceived((long) monthComplaints.size())
                                                        .totalResolved(resolved)
                                                        .averageResolutionTime(Math.round(avgResTime * 100.0) / 100.0)
                                                        .build();
                                })
                                .sorted(Comparator.comparing(PQRSStatisticsDTO.MonthlyTrendDTO::getMonth))
                                .collect(Collectors.toList());
        }

        private Double calculateResolutionRate(List<Complaint> complaints) {
                if (complaints.isEmpty())
                        return 0.0;

                long resolved = complaints.stream()
                                .filter(c -> isStatusClosed(c.getStatus()))
                                .count();

                return Math.round((resolved * 100.0 / complaints.size()) * 100.0) / 100.0;
        }

        private Double calculateSatisfactionRate(List<Complaint> complaints) {
                // Consideramos satisfactorio si se resolvió en menos del promedio de días
                // permitidos
                long satisfactory = complaints.stream()
                                .filter(c -> isStatusClosed(c.getStatus()) &&
                                                c.getDaysToResolve() != null &&
                                                c.getDaysToResolve() <= 15)
                                .count();

                long totalClosed = complaints.stream()
                                .filter(c -> isStatusClosed(c.getStatus()))
                                .count();

                if (totalClosed == 0)
                        return 0.0;

                return Math.round((satisfactory * 100.0 / totalClosed) * 100.0) / 100.0;
        }
}