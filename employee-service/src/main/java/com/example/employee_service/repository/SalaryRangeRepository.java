package com.example.employee_service.repository;

import com.example.employee_service.entity.SalaryRange;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaryRangeRepository extends JpaRepository<SalaryRange, String> {
}