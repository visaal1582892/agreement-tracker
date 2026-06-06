package com.medplus.agreement_tracker_backend.config;

import com.medplus.agreement_tracker_backend.entity.*;
import com.medplus.agreement_tracker_backend.enums.RightCode;
import com.medplus.agreement_tracker_backend.enums.RoleName;
import com.medplus.agreement_tracker_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final IncomeTypeRepository incomeTypeRepository;
    private final AgreementTypeRepository agreementTypeRepository;
    private final CompanyMasterRepository companyRepository;
    private final ManufacturerMasterRepository manufacturerRepository;
    private final DivisionMasterRepository divisionRepository;
    private final ProductMasterRepository productRepository;
    private final VendorMasterRepository vendorRepository;
    private final VendorProductMappingRepository vendorProductMappingRepository;
    private final RightRepository rightRepository;
    private final RoleRightRepository roleRightRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String TEST_PASSWORD = "Test@123";

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        seedTestUsers();
        seedLookups();
        seedRights();
        seedRoleRights();
        seedMockMasterData();
        seedVendorProductMappings();
        log.info("Data seeding complete");
    }

    private void seedRoles() {
        for (RoleName name : RoleName.values()) {
            if (roleRepository.findByName(name).isEmpty()) {
                Role role = Role.builder().name(name).description(name.name() + " role").build();
                roleRepository.save(role);
            }
        }
    }

    private void seedTestUsers() {
        seedUserWithRole("admin", "System Administrator", "admin@medplus.com", "EMP001", "Admin@123", RoleName.ADMIN);
        seedUserWithRole("amgr", "Ravi Kumar", "ravi.kumar@medplus.com", "EMP002", TEST_PASSWORD, RoleName.ACCOUNT_MANAGER);
        seedUserWithRole("approver", "Priya Sharma", "priya.sharma@medplus.com", "EMP003", TEST_PASSWORD, RoleName.APPROVER);
        seedUserWithRole("leader", "Anil Mehta", "anil.mehta@medplus.com", "EMP004", TEST_PASSWORD, RoleName.LEADERSHIP);
        seedUserWithRole("finance", "Sneha Patel", "sneha.patel@medplus.com", "EMP005", TEST_PASSWORD, RoleName.FINANCE);
    }

    private void seedUserWithRole(
            String username,
            String fullName,
            String email,
            String employeeId,
            String rawPassword,
            RoleName roleName) {
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }

        User user = User.builder()
                .username(username)
                .fullName(fullName)
                .email(email)
                .employeeId(employeeId)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .isActive(true)
                .build();
        user = userRepository.save(user);

        Role role = roleRepository.findByName(roleName).orElseThrow();
        userRoleRepository.save(UserRole.builder().user(user).role(role).build());

        log.info("Test user created: {} / {} ({})", username, rawPassword, roleName);
    }

    private void seedLookups() {
        List<String> incomeTypes = List.of("Data Fee", "Retro", "Listing Fee", "Margin", "Sample", "CME");
        for (String name : incomeTypes) {
            if (!incomeTypeRepository.existsByNameIgnoreCase(name)) {
                incomeTypeRepository.save(IncomeType.builder().name(name).build());
            }
        }

        List<String> agreementTypes = List.of("Annual", "Quarterly", "One-Time", "Perpetual");
        for (String name : agreementTypes) {
            if (!agreementTypeRepository.existsByNameIgnoreCase(name)) {
                agreementTypeRepository.save(AgreementType.builder().name(name).build());
            }
        }
    }

    private void seedRights() {
        List<Right> defaults = List.of(
                right(RightCode.DASHBOARD_VIEW, "View Dashboard", "DASHBOARD"),
                right(RightCode.AGREEMENT_VIEW, "View Agreements", "AGREEMENTS"),
                right(RightCode.AGREEMENT_VIEW_ALL, "View All Agreements", "AGREEMENTS"),
                right(RightCode.AGREEMENT_CREATE, "Create Agreements", "AGREEMENTS"),
                right(RightCode.AGREEMENT_EDIT, "Edit Agreements", "AGREEMENTS"),
                right(RightCode.AGREEMENT_APPROVE, "Approve Agreements", "AGREEMENTS"),
                right(RightCode.MASTER_VIEW, "View Master Data", "MASTER"),
                right(RightCode.MASTER_MANAGE, "Manage Master Data", "MASTER"),
                right(RightCode.ADMIN_USERS, "Manage Users", "ADMIN")
        );
        for (Right right : defaults) {
            if (rightRepository.findByCode(right.getCode()).isEmpty()) {
                rightRepository.save(right);
            }
        }
    }

    private Right right(RightCode code, String name, String module) {
        return Right.builder().code(code.name()).name(name).module(module).build();
    }

    private void seedRoleRights() {
        roleRightRepository.deleteAllInBatch();

        Map<RoleName, List<String>> mappings = Map.of(
                RoleName.ADMIN, List.of(
                        RightCode.MASTER_MANAGE.name(),
                        RightCode.MASTER_VIEW.name(),
                        RightCode.ADMIN_USERS.name()
                ),
                RoleName.ACCOUNT_MANAGER, List.of(
                        RightCode.AGREEMENT_VIEW.name(),
                        RightCode.AGREEMENT_CREATE.name(),
                        RightCode.AGREEMENT_EDIT.name(),
                        RightCode.MASTER_VIEW.name(),
                        RightCode.DASHBOARD_VIEW.name()
                ),
                RoleName.APPROVER, List.of(
                        RightCode.AGREEMENT_VIEW_ALL.name(),
                        RightCode.AGREEMENT_APPROVE.name(),
                        RightCode.MASTER_VIEW.name(),
                        RightCode.DASHBOARD_VIEW.name()
                )
        );

        mappings.forEach((roleName, rightCodes) -> {
            Role role = roleRepository.findByName(roleName).orElseThrow();
            for (String code : rightCodes) {
                Right right = rightRepository.findByCode(code).orElseThrow();
                roleRightRepository.save(RoleRight.builder().role(role).right(right).build());
            }
        });

        log.info("Role-rights seeded: ADMIN(3), ACCOUNT_MANAGER(5), APPROVER(4)");
    }

    private void seedMockMasterData() {
        if (!companyRepository.existsByCompanyNameIgnoreCase("Apollo Hospitals")) {
            companyRepository.save(CompanyMaster.builder().companyName("Apollo Hospitals").build());
            companyRepository.save(CompanyMaster.builder().companyName("Fortis Healthcare").build());
            companyRepository.save(CompanyMaster.builder().companyName("Max Healthcare").build());
        }

        if (manufacturerRepository.count() == 0) {
            ManufacturerMaster pfizer = manufacturerRepository.save(
                    ManufacturerMaster.builder().manufacturerCode("PFZ").manufacturerName("Pfizer").build());
            ManufacturerMaster sun = manufacturerRepository.save(
                    ManufacturerMaster.builder().manufacturerCode("SUN").manufacturerName("Sun Pharma").build());

            DivisionMaster oncology = divisionRepository.save(
                    DivisionMaster.builder().divisionCode("PFZ-ONC").divisionName("Oncology").manufacturer(pfizer).build());
            DivisionMaster vaccines = divisionRepository.save(
                    DivisionMaster.builder().divisionCode("PFZ-VAC").divisionName("Vaccines").manufacturer(pfizer).build());
            DivisionMaster cardio = divisionRepository.save(
                    DivisionMaster.builder().divisionCode("SUN-CAR").divisionName("Cardiology").manufacturer(sun).build());

            productRepository.save(ProductMaster.builder().productCode("IBR001").productName("Ibrance").manufacturer(pfizer).division(oncology).build());
            productRepository.save(ProductMaster.builder().productCode("XTD001").productName("Xtandi").manufacturer(pfizer).division(oncology).build());
            productRepository.save(ProductMaster.builder().productCode("CVX001").productName("Prevnar").manufacturer(pfizer).division(vaccines).build());
            productRepository.save(ProductMaster.builder().productCode("TLM001").productName("Telma").manufacturer(sun).division(cardio).build());
        }

        if (vendorRepository.count() == 0) {
            vendorRepository.save(VendorMaster.builder().vendorCode("V001").vendorName("MedPlus Pharmacy - Mumbai").build());
            vendorRepository.save(VendorMaster.builder().vendorCode("V002").vendorName("MedPlus Pharmacy - Delhi").build());
            vendorRepository.save(VendorMaster.builder().vendorCode("V003").vendorName("MedPlus Pharmacy - Hyderabad").build());
            vendorRepository.save(VendorMaster.builder().vendorCode("V004").vendorName("MedPlus Pharmacy - Chennai").build());
        }
    }

    private void seedVendorProductMappings() {
        List<String[]> mappings = List.of(
                new String[]{"V001", "IBR001"},
                new String[]{"V001", "XTD001"},
                new String[]{"V001", "CVX001"},
                new String[]{"V001", "TLM001"},
                new String[]{"V002", "IBR001"},
                new String[]{"V002", "XTD001"},
                new String[]{"V003", "CVX001"},
                new String[]{"V003", "TLM001"},
                new String[]{"V004", "IBR001"},
                new String[]{"V004", "TLM001"}
        );

        for (String[] mapping : mappings) {
            VendorMaster vendor = vendorRepository.findByVendorCode(mapping[0]).orElse(null);
            ProductMaster product = productRepository.findByProductCode(mapping[1]).orElse(null);
            if (vendor == null || product == null) {
                log.warn("Skipping vendor-product mapping {} -> {}: master data not found", mapping[0], mapping[1]);
                continue;
            }
            if (vendorProductMappingRepository.existsByVendorIdAndProductId(vendor.getId(), product.getId())) {
                continue;
            }
            vendorProductMappingRepository.save(
                    VendorProductMapping.builder().vendor(vendor).product(product).build());
        }
    }
}
