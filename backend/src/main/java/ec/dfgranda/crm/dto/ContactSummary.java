package ec.dfgranda.crm.dto;

public record ContactSummary(Long id, Long companyId, String companyName, String fullName, String email,
                             String phone, String jobTitle, boolean commercialContactAllowed) {
}
