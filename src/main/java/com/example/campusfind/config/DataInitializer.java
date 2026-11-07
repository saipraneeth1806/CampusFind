package com.example.campusfind.config;

import com.example.campusfind.dto.ReportForm;
import com.example.campusfind.model.CustodyStatus;
import com.example.campusfind.model.ItemCategory;
import com.example.campusfind.model.ReportType;
import com.example.campusfind.model.UserAccount;
import com.example.campusfind.repository.ReportRepository;
import com.example.campusfind.repository.UserAccountRepository;
import com.example.campusfind.service.ReportService;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "campusfind.seed-data", havingValue = "true")
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final UserAccountRepository accounts;
    private final ReportRepository reports;
    private final ReportService reportService;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserAccountRepository accounts, ReportRepository reports,
                           ReportService reportService, PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.reports = reports;
        this.reportService = reportService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (accounts.count() > 0 || reports.count() > 0) return;
        log.info("Seeding initial demo data for CampusFind...");

        UserAccount student = accounts.save(new UserAccount("Demo Student", "demo@campus.edu",
                passwordEncoder.encode("CampusFind-123")));
        UserAccount helper = accounts.save(new UserAccount("Campus Helper", "helper@campus.edu",
                passwordEncoder.encode("CampusFind-123")));

        createReport(ReportType.FOUND, "Hydro Flask Blue Bottle", ItemCategory.WATER_BOTTLES,
                "Blue insulated 32oz bottle with outdoor stickers found on table 4.", LocalDate.now(), "Main Library 1st Floor",
                CustodyStatus.LIBRARY_DESK, "What color sticker is near the base of the bottle?", null, null, helper);
        createReport(ReportType.LOST, "Casio Scientific Calculator", ItemCategory.ELECTRONICS,
                "Silver Casio fx-991EX calculator in black sliding case.", LocalDate.now().minusDays(1), "Science Block Room 204",
                CustodyStatus.WITH_FINDER, null, "₹200 or Canteen Coffee ☕", null, student);
        createReport(ReportType.FOUND, "College ID Card & Metro Pass", ItemCategory.ID_CARDS,
                "DSATM College ID card found near central cafeteria counter.", LocalDate.now().minusDays(2), "Cafeteria Main Hall",
                CustodyStatus.SECURITY_DESK, null, null, "1DS22CS104", helper);
        createReport(ReportType.LOST, "Keys with Red Lanyard", ItemCategory.KEYS,
                "Ring with dorm room key and red campus lanyard.", LocalDate.now().minusDays(3), "Gymnasium Locker Area",
                CustodyStatus.WITH_FINDER, null, "Coffee treat on me!", null, student);

        log.info("CampusFind demo data seeded successfully. Demo login: demo@campus.edu / CampusFind-123");
    }

    private void createReport(ReportType type, String name, ItemCategory category,
                              String description, LocalDate date, String location,
                              CustodyStatus custody, String secretQuestion, String reward, String studentId,
                              UserAccount owner) {
        ReportForm form = new ReportForm();
        form.setType(type);
        form.setItemName(name);
        form.setCategory(category);
        form.setDescription(description);
        form.setLastSeenOrFoundAt(date);
        form.setLocation(location);
        form.setCustodyStatus(custody);
        form.setSecretQuestion(secretQuestion);
        form.setReward(reward);
        form.setStudentId(studentId);
        reportService.create(form, owner);
    }
}
