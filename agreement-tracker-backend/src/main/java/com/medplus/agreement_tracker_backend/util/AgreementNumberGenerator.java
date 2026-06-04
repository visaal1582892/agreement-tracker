package com.medplus.agreement_tracker_backend.util;

import com.medplus.agreement_tracker_backend.repository.AgreementGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Year;

@Component
@RequiredArgsConstructor
public class AgreementNumberGenerator {

    private final AgreementGroupRepository agreementGroupRepository;

    @Value("${app.agreement-number-prefix}")
    private String prefix;

    public synchronized String generate() {
        int year = Year.now().getValue();
        String yearPrefix = prefix + "-" + year + "-";
        Integer maxSeq = agreementGroupRepository.findMaxSequenceForPrefix(yearPrefix);
        int next = (maxSeq == null ? 0 : maxSeq) + 1;
        return String.format("%s%04d", yearPrefix, next);
    }
}
