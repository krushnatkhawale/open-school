package com.kaushalya.digitalschool.web;

import com.kaushalya.digitalschool.onboarding.AppUser;
import com.kaushalya.digitalschool.onboarding.AppUserRepository;
import com.kaushalya.digitalschool.onboarding.OnboardingStatus;
import com.kaushalya.digitalschool.onboarding.School;
import com.kaushalya.digitalschool.onboarding.SchoolRepository;
import com.kaushalya.digitalschool.onboarding.UserRole;
import com.kaushalya.digitalschool.shared.CommunicationType;
import com.kaushalya.digitalschool.shared.ContentType;
import com.kaushalya.digitalschool.shared.ScheduledEventType;
import com.kaushalya.digitalschool.storage.ClassificationRecord;
import com.kaushalya.digitalschool.storage.ClassificationRecordRepository;
import com.kaushalya.digitalschool.storage.ScheduledEvent;
import com.kaushalya.digitalschool.storage.ScheduledEventRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/test")
@ConditionalOnProperty(name = "app.test-seed-enabled", havingValue = "true")
public class TestSeedController {

    private final ClassificationRecordRepository classificationRepository;
    private final ScheduledEventRepository scheduledEventRepository;
    private final SchoolRepository schoolRepository;
    private final AppUserRepository appUserRepository;

    public TestSeedController(ClassificationRecordRepository classificationRepository,
                              ScheduledEventRepository scheduledEventRepository,
                              SchoolRepository schoolRepository,
                              AppUserRepository appUserRepository) {
        this.classificationRepository = classificationRepository;
        this.scheduledEventRepository = scheduledEventRepository;
        this.schoolRepository = schoolRepository;
        this.appUserRepository = appUserRepository;
    }

    @PostMapping("/seed")
    public String seed() {
        LocalDate today = LocalDate.now();
        Instant now = Instant.now();

        classificationRepository.deleteAll();
        scheduledEventRepository.deleteAll();
        appUserRepository.deleteAll();
        schoolRepository.deleteAll();

        School greenValley = schoolRepository.save(
                new School(UUID.randomUUID(), "Green Valley School", "Bengaluru", null, now));
        School sunrise = schoolRepository.save(
                new School(UUID.randomUUID(), "Sunrise International", "Mysuru", null, now));
        AppUser user1 = new AppUser(UUID.randomUUID(), 12345L, UserRole.SCHOOL, OnboardingStatus.ACTIVE, now);
        user1.setSchool(greenValley);
        appUserRepository.save(user1);
        AppUser user2 = new AppUser(UUID.randomUUID(), 67890L, UserRole.SCHOOL, OnboardingStatus.ACTIVE, now);
        user2.setSchool(sunrise);
        appUserRepository.save(user2);

        ClassificationRecord r1 = new ClassificationRecord(UUID.randomUUID(), 12345L,
                "Today we learned algebra and geometry. Topics covered: quadratic equations and proofs.",
                CommunicationType.DAILY_LESSON_UPDATE, null, today, now);
        r1.setContentType(ContentType.TEXT);
        r1.setTags("maths");
        classificationRepository.save(r1);

        ClassificationRecord r2 = new ClassificationRecord(UUID.randomUUID(), 12345L,
                "Parent-teacher meeting scheduled for tomorrow at 5 PM in the school auditorium. All parents are requested to attend and discuss their child's progress. Teachers will share report cards and homework feedback. Please reach the venue 15 minutes early and carry your child's report card. Meeting will cover academic performance, attendance and upcoming exams. Teachers from every subject will be available for one-on-one discussions in their classrooms after the main session. The school office will stay open until 6 PM to assist with fee receipts, bus route changes and uniform orders. Refreshments will be served in the cafeteria from 4:30 PM for parents while they wait. Kindly park in the designated visitors lot behind the sports block, as the front gate is reserved for buses. For parents unable to attend, a summary of the meeting notes will be shared in the WhatsApp class group the following day.",
                CommunicationType.REMINDER,
                "Parent-teacher meeting scheduled for tomorrow at 5 PM in the school auditorium. All parents are requested to attend and discuss their child's progress. Teachers will share report cards and homework feedback. Please reach the venue 15 minutes early and carry your child's report card. Meeting will cover academic performance, attendance and upcoming exams. Teachers from every subject will be available for one-on-one discussions in their classrooms after the main session. The school office will stay open until 6 PM to assist with fee receipts, bus route changes and uniform orders. Refreshments will be served in the cafeteria from 4:30 PM for parents while they wait. Kindly park in the designated visitors lot behind the sports block, as the front gate is reserved for buses. For parents unable to attend, a summary of the meeting notes will be shared in the WhatsApp class group the following day.",
                today.plusDays(1), now.minusSeconds(3600));
        r2.setImageData(samplePng());
        r2.setImageContentType("image/png");
        r2.setContentType(ContentType.IMAGE);
        r2.setTags("notice,hindi");
        classificationRepository.save(r2);

        ClassificationRecord r3 = new ClassificationRecord(UUID.randomUUID(), 67890L,
                "School will remain closed for Diwali from Oct 20 to Oct 30.",
                CommunicationType.GENERAL_INFORMATION, null, today.plusDays(2), now.minusSeconds(7200));
        r3.setContentType(ContentType.TEXT);
        r3.setTags("holiday,story");
        classificationRepository.save(r3);

        ClassificationRecord r4 = new ClassificationRecord(UUID.randomUUID(), 67890L,
                "Dear parents, this is a reminder that the Annual Science Exhibition will be held in the main auditorium on Friday. Students must submit their project models and charts to their class teachers by Wednesday morning. The exhibition will showcase projects on renewable energy, robotics, and organic farming. Parents are invited to attend from 10 AM to 2 PM. Please ensure your child brings their participation certificate and project logbook on the day. Judging will begin at 10:30 AM and winners will be announced at 1 PM during the closing ceremony. Each project must be accompanied by a one-page write-up explaining the working principle and materials used. Students from classes 6 to 10 will present their work in three batches, and class teachers will guide the arrangement of stalls. A small entry fee of twenty rupees per visitor will be collected at the entrance and donated to the school library fund. Tea and snacks will be available in the cafeteria, and the school transport department will run an extra bus at 3 PM for students who need to return home early. In case of rain, the exhibition will move indoors to the second-floor classrooms. Students are encouraged to visit every stall and record their observations in the feedback booklet provided at the registration desk, and teachers will award participation points to every student who completes the circuit before noon.",
                CommunicationType.CIRCULAR_NOTICE, null, today, now.minusSeconds(1));
        r4.setContentType(ContentType.TEXT);
        r4.setTags("science,exhibition");
        classificationRepository.save(r4);

        ClassificationRecord onboarding = new ClassificationRecord(UUID.randomUUID(), 12345L,
                "Welcome! What is the name of the school you represent?",
                CommunicationType.GENERAL_INFORMATION, null, today, now);
        onboarding.setContentType(ContentType.TEXT);
        onboarding.setTags("onboarding");
        onboarding.setFeedHidden(true);
        classificationRepository.save(onboarding);

        ScheduledEvent e1 = new ScheduledEvent(UUID.randomUUID(), today.plusDays(1),
                "Parent-Teacher Meeting", ScheduledEventType.PARENT_TEACHER_MEETING, 67890L,
                "Monthly planner", now);
        scheduledEventRepository.save(e1);

        ScheduledEvent e2 = new ScheduledEvent(UUID.randomUUID(), today.plusDays(2),
                "Diwali Break", ScheduledEventType.HOLIDAY, 12345L,
                "School holiday calendar", now);
        scheduledEventRepository.save(e2);

        ScheduledEvent e3 = new ScheduledEvent(UUID.randomUUID(), today.plusDays(14),
                "Mid-term Examinations", ScheduledEventType.EXAM, 12345L,
                "Exam schedule uploaded", now);
        scheduledEventRepository.save(e3);

        return "Seeded " + 5 + " classifications and " + 3 + " scheduled events and " + 2 + " schools";
    }

    private byte[] samplePng() {
        try {
            BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(Color.decode("#2b6cb0"));
            g.fillRect(0, 0, 32, 32);
            g.setColor(Color.WHITE);
            g.drawString("IMG", 4, 20);
            g.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }
}
