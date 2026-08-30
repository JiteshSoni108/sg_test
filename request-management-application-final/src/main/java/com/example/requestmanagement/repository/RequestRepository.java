package com.example.requestmanagement.repository;

import com.example.requestmanagement.domain.RequestEntity;
import com.example.requestmanagement.domain.RequestState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RequestRepository extends JpaRepository<RequestEntity, Long>, JpaSpecificationExecutor<RequestEntity> {
}
