package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.StoreMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoreMasterRepository extends JpaRepository<StoreMaster, Long> {

    List<StoreMaster> findByStoreCodeInAndIsActiveTrue(Collection<String> storeCodes);

    Optional<StoreMaster> findByStoreCodeIgnoreCaseAndIsActiveTrue(String storeCode);
}
