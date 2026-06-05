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

import java.util.Arrays;
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
    private final RightRepository rightRepository;
    private final RoleRightRepository roleRightRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        seedAdminUser();
        seedLookups();
        seedRights();
        seedRoleRights();
        seedMockMasterData();
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

    private void seedAdminUser() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = User.builder()
                    .username("admin")
                    .fullName("System Administrator")
                    .email("admin@medplus.com")
                    .employeeId("EMP001")
                    .passwordHash(passwordEncoder.encode("Admin@123"))
                    .isActive(true)
                    .build();
            admin = userRepository.save(admin);

            Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
            UserRole userRole = UserRole.builder().user(admin).role(adminRole).build();
            userRoleRepository.save(userRole);

            log.info("Admin user created: admin / Admin@123");
        }
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
        List<String> allRights = Arrays.stream(RightCode.values()).map(Enum::name).toList();

        Map<RoleName, List<String>> mappings = Map.of(
                RoleName.ADMIN, allRights,
                RoleName.ACCOUNT_MANAGER, List.of(
                        RightCode.DASHBOARD_VIEW.name(),
                        RightCode.AGREEMENT_VIEW.name(),
                        RightCode.AGREEMENT_CREATE.name(),
                        RightCode.AGREEMENT_EDIT.name()
                ),
                RoleName.APPROVER, List.of(
                        RightCode.DASHBOARD_VIEW.name(),
                        RightCode.AGREEMENT_VIEW.name(),
                        RightCode.AGREEMENT_APPROVE.name()
                ),
                RoleName.LEADERSHIP, List.of(
                        RightCode.DASHBOARD_VIEW.name(),
                        RightCode.AGREEMENT_VIEW.name(),
                        RightCode.AGREEMENT_VIEW_ALL.name()
                ),
                RoleName.FINANCE, List.of(
                        RightCode.DASHBOARD_VIEW.name(),
                        RightCode.AGREEMENT_VIEW.name(),
                        RightCode.AGREEMENT_VIEW_ALL.name()
                )
        );

        mappings.forEach((roleName, rightCodes) -> {
            Role role = roleRepository.findByName(roleName).orElseThrow();
            for (String code : rightCodes) {
                Right right = rightRepository.findByCode(code).orElseThrow();
                if (!roleRightRepository.existsByRoleIdAndRightId(role.getId(), right.getId())) {
                    roleRightRepository.save(RoleRight.builder().role(role).right(right).build());
                }
            }
        });
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
}
