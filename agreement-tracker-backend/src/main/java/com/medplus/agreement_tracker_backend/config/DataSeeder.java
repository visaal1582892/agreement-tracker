package com.medplus.agreement_tracker_backend.config;

import com.medplus.agreement_tracker_backend.entity.*;
import com.medplus.agreement_tracker_backend.enums.DiscountCalculationKind;
import com.medplus.agreement_tracker_backend.enums.RightCode;
import com.medplus.agreement_tracker_backend.enums.RoleName;
import com.medplus.agreement_tracker_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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
    private final StateMasterRepository stateMasterRepository;
    private final StoreMasterRepository storeMasterRepository;
    private final CompanyMasterRepository companyRepository;
    private final ManufacturerMasterRepository manufacturerRepository;
    private final DivisionMasterRepository divisionRepository;
    private final ProductMasterRepository productRepository;
    private final VendorMasterRepository vendorRepository;
    private final VendorProductMappingRepository vendorProductMappingRepository;
    private final RightRepository rightRepository;
    private final RoleRightRepository roleRightRepository;
    private final ChannelMasterRepository channelRepository;
    private final DiscountTypeMasterRepository discountTypeRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String TEST_PASSWORD = "Test@123";

    @Override
    public void run(String... args) {
        try {
            seedRoles();
            seedTestUsers();
            seedLookups();
            seedAgreementTypes();
            seedStateMaster();
            seedChannelMaster();
            seedDiscountTypeMaster();
            seedStoresMaster();
            seedRights();
            seedRoleRights();
            seedMockMasterData();
            seedVendorProductMappings();
            log.info("Data seeding complete");
        } catch (Exception ex) {
            log.error("Data seeding failed — login and master data may be unavailable", ex);
            throw ex;
        }
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
        List<String> incomeTypes = List.of(
                "Data Fee",
                "Commercial Contracts",
                "Asset Rentals",
                "Ad-Hoc Activities"
        );
        for (String name : incomeTypes) {
            if (!incomeTypeRepository.existsByNameIgnoreCase(name)) {
                incomeTypeRepository.save(IncomeType.builder().name(name).build());
            }
        }

    }

    private void seedAgreementTypes() {
        List<String> allowedTypes = List.of("Agreement", "MOU", "Email Confirmation");
        for (String name : allowedTypes) {
            agreementTypeRepository.findAll().stream()
                    .filter(type -> type.getName().equalsIgnoreCase(name))
                    .findFirst()
                    .ifPresentOrElse(
                            existing -> {
                                if (!existing.isActive()) {
                                    existing.setActive(true);
                                    agreementTypeRepository.save(existing);
                                }
                            },
                            () -> agreementTypeRepository.save(AgreementType.builder().name(name).build())
                    );
        }
        agreementTypeRepository.findAll().forEach(type -> {
            boolean allowed = allowedTypes.stream()
                    .anyMatch(name -> name.equalsIgnoreCase(type.getName()));
            if (!allowed && type.isActive()) {
                type.setActive(false);
                agreementTypeRepository.save(type);
            }
        });
    }

    private void seedStateMaster() {
        List<Map.Entry<String, String>> allowedStates = List.of(
                Map.entry("Andhra Pradesh", "AP"),
                Map.entry("Telangana", "TS"),
                Map.entry("Maharashtra", "MH"),
                Map.entry("Karnataka", "KA"),
                Map.entry("Tamil Nadu", "TN"),
                Map.entry("Delhi", "DL"),
                Map.entry("Gujarat", "GJ"),
                Map.entry("West Bengal", "WB"),
                Map.entry("Uttar Pradesh", "UP"),
                Map.entry("Rajasthan", "RJ")
        );
        for (Map.Entry<String, String> seed : allowedStates) {
            stateMasterRepository.findByStateCodeIgnoreCase(seed.getValue())
                    .ifPresentOrElse(
                            existing -> {
                                existing.setStateName(seed.getKey());
                                if (!existing.isActive()) {
                                    existing.setActive(true);
                                }
                                stateMasterRepository.save(existing);
                            },
                            () -> stateMasterRepository.save(StateMaster.builder()
                                    .stateName(seed.getKey())
                                    .stateCode(seed.getValue())
                                    .build())
                    );
        }
        var allowedCodes = allowedStates.stream().map(Map.Entry::getValue).map(String::toUpperCase).toList();
        stateMasterRepository.findAll().forEach(state -> {
            if (!allowedCodes.contains(state.getStateCode().toUpperCase()) && state.isActive()) {
                state.setActive(false);
                stateMasterRepository.save(state);
            }
        });
    }

    private void seedChannelMaster() {
        List<String[]> channels = List.of(
                new String[]{"RETAIL", "Retail Pharmacy"},
                new String[]{"ECOM", "E-Commerce"},
                new String[]{"B2B", "B2B Institutional"},
                new String[]{"HOSP", "Hospital Channel"}
        );
        for (String[] seed : channels) {
            channelRepository.findByChannelNameIgnoreCaseAndIsActiveTrue(seed[1])
                    .ifPresentOrElse(
                            existing -> {
                                existing.setChannelCode(seed[0]);
                                channelRepository.save(existing);
                            },
                            () -> channelRepository.save(ChannelMaster.builder()
                                    .channelCode(seed[0])
                                    .channelName(seed[1])
                                    .build()));
        }
    }

    private void seedDiscountTypeMaster() {
        List<Object[]> types = List.of(
                new Object[]{"PERCENTAGE", "Percentage Off", DiscountCalculationKind.PERCENTAGE},
                new Object[]{"FIXED_AMOUNT", "Fixed Amount Off", DiscountCalculationKind.FIXED_AMOUNT}
        );
        for (Object[] seed : types) {
            String code = (String) seed[0];
            String name = (String) seed[1];
            DiscountCalculationKind kind = (DiscountCalculationKind) seed[2];
            discountTypeRepository.findByDiscountCodeIgnoreCaseAndIsActiveTrue(code)
                    .ifPresentOrElse(
                            existing -> {
                                existing.setDiscountName(name);
                                existing.setCalculationKind(kind);
                                discountTypeRepository.save(existing);
                            },
                            () -> discountTypeRepository.save(DiscountTypeMaster.builder()
                                    .discountCode(code)
                                    .discountName(name)
                                    .calculationKind(kind)
                                    .build()));
        }
    }

    private void seedStoresMaster() {
        if (storeMasterRepository.count() > 0) {
            return;
        }

        StateMaster telangana = stateMasterRepository.findByStateName("Telangana").orElse(null);
        if (telangana != null) {
            storeMasterRepository.saveAll(List.of(
                    storeSeed("HYD-01", "Hyderabad Store 01", telangana),
                    storeSeed("HYD-02", "Hyderabad Store 02", telangana),
                    storeSeed("WAR-01", "Warangal Store 01", telangana)
            ));
        }

        StateMaster maharashtra = stateMasterRepository.findByStateName("Maharashtra").orElse(null);
        if (maharashtra != null) {
            storeMasterRepository.saveAll(List.of(
                    storeSeed("MUM-01", "Mumbai Store 01", maharashtra),
                    storeSeed("PUN-01", "Pune Store 01", maharashtra),
                    storeSeed("NAG-01", "Nagpur Store 01", maharashtra)
            ));
        }

        StateMaster andhraPradesh = stateMasterRepository.findByStateName("Andhra Pradesh").orElse(null);
        if (andhraPradesh != null) {
            storeMasterRepository.saveAll(List.of(
                    storeSeed("VIZ-01", "Visakhapatnam Store 01", andhraPradesh),
                    storeSeed("VJA-01", "Vijayawada Store 01", andhraPradesh),
                    storeSeed("GNT-01", "Guntur Store 01", andhraPradesh)
            ));
        }

        StateMaster karnataka = stateMasterRepository.findByStateName("Karnataka").orElse(null);
        if (karnataka != null) {
            storeMasterRepository.saveAll(List.of(
                    storeSeed("BLR-01", "Bengaluru Store 01", karnataka),
                    storeSeed("MYS-01", "Mysuru Store 01", karnataka),
                    storeSeed("HUB-01", "Hubballi Store 01", karnataka)
            ));
        }

        StateMaster tamilNadu = stateMasterRepository.findByStateName("Tamil Nadu").orElse(null);
        if (tamilNadu != null) {
            storeMasterRepository.saveAll(List.of(
                    storeSeed("CHN-01", "Chennai Store 01", tamilNadu),
                    storeSeed("CBE-01", "Coimbatore Store 01", tamilNadu),
                    storeSeed("MDU-01", "Madurai Store 01", tamilNadu)
            ));
        }

        StateMaster delhi = stateMasterRepository.findByStateName("Delhi").orElse(null);
        if (delhi != null) {
            storeMasterRepository.saveAll(List.of(
                    storeSeed("DEL-01", "Delhi Store 01", delhi),
                    storeSeed("DEL-02", "Delhi Store 02", delhi),
                    storeSeed("NCR-01", "NCR Store 01", delhi)
            ));
        }

        StateMaster gujarat = stateMasterRepository.findByStateName("Gujarat").orElse(null);
        if (gujarat != null) {
            storeMasterRepository.saveAll(List.of(
                    storeSeed("AHM-01", "Ahmedabad Store 01", gujarat),
                    storeSeed("SUR-01", "Surat Store 01", gujarat),
                    storeSeed("RAJ-01", "Rajkot Store 01", gujarat)
            ));
        }

        StateMaster westBengal = stateMasterRepository.findByStateName("West Bengal").orElse(null);
        if (westBengal != null) {
            storeMasterRepository.saveAll(List.of(
                    storeSeed("KOL-01", "Kolkata Store 01", westBengal),
                    storeSeed("SIL-01", "Siliguri Store 01", westBengal),
                    storeSeed("DUR-01", "Durgapur Store 01", westBengal)
            ));
        }

        StateMaster uttarPradesh = stateMasterRepository.findByStateName("Uttar Pradesh").orElse(null);
        if (uttarPradesh != null) {
            storeMasterRepository.saveAll(List.of(
                    storeSeed("LKO-01", "Lucknow Store 01", uttarPradesh),
                    storeSeed("GZB-01", "Ghaziabad Store 01", uttarPradesh),
                    storeSeed("VNS-01", "Varanasi Store 01", uttarPradesh)
            ));
        }

        StateMaster rajasthan = stateMasterRepository.findByStateName("Rajasthan").orElse(null);
        if (rajasthan != null) {
            storeMasterRepository.saveAll(List.of(
                    storeSeed("JPR-01", "Jaipur Store 01", rajasthan),
                    storeSeed("UDA-01", "Udaipur Store 01", rajasthan),
                    storeSeed("JOD-01", "Jodhpur Store 01", rajasthan)
            ));
        }

        log.info("Store master seeded: {} stores", storeMasterRepository.count());
    }

    private StoreMaster storeSeed(String storeCode, String storeName, StateMaster state) {
        return StoreMaster.builder()
                .storeCode(storeCode)
                .storeName(storeName)
                .state(state)
                .build();
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
                right(RightCode.ADMIN_USERS, "Manage Users", "ADMIN"),
                right(RightCode.PRICE_OFF_VIEW, "View Price Off Campaigns", "PRICE_OFFS"),
                right(RightCode.PRICE_OFF_MANAGE, "Manage Price Off Campaigns", "PRICE_OFFS"),
                right(RightCode.PRICE_OFF_APPROVE, "Approve Price Off Campaigns", "PRICE_OFFS")
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
                        RightCode.DASHBOARD_VIEW.name(),
                        RightCode.PRICE_OFF_VIEW.name(),
                        RightCode.PRICE_OFF_MANAGE.name()
                ),
                RoleName.APPROVER, List.of(
                        RightCode.AGREEMENT_VIEW_ALL.name(),
                        RightCode.AGREEMENT_APPROVE.name(),
                        RightCode.MASTER_VIEW.name(),
                        RightCode.DASHBOARD_VIEW.name(),
                        RightCode.PRICE_OFF_APPROVE.name()
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

            productRepository.save(ProductMaster.builder().productCode("IBR001").productName("Ibrance").manufacturer(pfizer).division(oncology).l3Category("Oncology Oral").mrp(new java.math.BigDecimal("125000")).build());
            productRepository.save(ProductMaster.builder().productCode("XTD001").productName("Xtandi").manufacturer(pfizer).division(oncology).l3Category("Oncology Oral").mrp(new java.math.BigDecimal("98000")).build());
            productRepository.save(ProductMaster.builder().productCode("CVX001").productName("Prevnar").manufacturer(pfizer).division(vaccines).l3Category("Vaccines").mrp(new java.math.BigDecimal("4200")).build());
            productRepository.save(ProductMaster.builder().productCode("TLM001").productName("Telma").manufacturer(sun).division(cardio).l3Category("Cardiology").mrp(new java.math.BigDecimal("320")).build());
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
