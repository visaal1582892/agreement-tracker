package com.medplus.agreement_tracker_backend.service;

import com.medplus.agreement_tracker_backend.dto.response.JbpStagedWorkbookDto;
import org.springframework.web.multipart.MultipartFile;

public interface JbpExcelParserService {

    JbpStagedWorkbookDto parseUpload(Long agreementVersionId, MultipartFile file, Long currentUserId);
}
