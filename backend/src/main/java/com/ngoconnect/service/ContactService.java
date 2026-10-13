package com.ngoconnect.service;

import com.ngoconnect.dto.ContactInquiryRequest;
import com.ngoconnect.dto.ContactInquiryResponse;

import java.util.List;

public interface ContactService {
    ContactInquiryResponse submitInquiry(ContactInquiryRequest request);
    List<ContactInquiryResponse> getAllInquiries();
    ContactInquiryResponse getInquiryById(Long id);
}
