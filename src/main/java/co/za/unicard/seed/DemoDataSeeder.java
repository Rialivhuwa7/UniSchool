package co.za.unicard.seed;

import co.za.unicard.model.AccessRequest;
import co.za.unicard.model.Campus;
import co.za.unicard.model.FeeAccount;
import co.za.unicard.model.FeePlan;
import co.za.unicard.model.GroupMessage;
import co.za.unicard.model.Homework;
import co.za.unicard.model.Learner;
import co.za.unicard.model.LedgerSource;
import co.za.unicard.model.MenuItem;
import co.za.unicard.model.Notice;
import co.za.unicard.model.NoticeReceipt;
import co.za.unicard.model.ParentGroup;
import co.za.unicard.model.PocketMoneyRule;
import co.za.unicard.model.PreOrder;
import co.za.unicard.model.PreOrderLine;
import co.za.unicard.model.PrelovedListing;
import co.za.unicard.model.PublishStatus;
import co.za.unicard.model.Role;
import co.za.unicard.model.RoleAssignment;
import co.za.unicard.model.SavingsPot;
import co.za.unicard.model.SchoolCollection;
import co.za.unicard.model.SchoolLedgerEntry;
import co.za.unicard.model.SchoolSettings;
import co.za.unicard.model.SportOffer;
import co.za.unicard.model.SupplyList;
import co.za.unicard.model.SupplyListItem;
import co.za.unicard.model.TuckMode;
import co.za.unicard.model.TxType;
import co.za.unicard.model.UniformItem;
import co.za.unicard.model.UniformStock;
import co.za.unicard.model.UserAccount;
import co.za.unicard.model.WalletTransaction;
import co.za.unicard.repository.AccessRequestRepository;
import co.za.unicard.repository.FeeAccountRepository;
import co.za.unicard.repository.GroupMessageRepository;
import co.za.unicard.repository.HomeworkRepository;
import co.za.unicard.repository.LearnerRepository;
import co.za.unicard.repository.MenuItemRepository;
import co.za.unicard.repository.NoticeReceiptRepository;
import co.za.unicard.repository.NoticeRepository;
import co.za.unicard.repository.ParentGroupRepository;
import co.za.unicard.repository.PocketMoneyRuleRepository;
import co.za.unicard.repository.PreOrderRepository;
import co.za.unicard.repository.PrelovedListingRepository;
import co.za.unicard.repository.SavingsPotRepository;
import co.za.unicard.repository.SchoolCollectionRepository;
import co.za.unicard.repository.SchoolLedgerRepository;
import co.za.unicard.repository.SchoolSettingsRepository;
import co.za.unicard.repository.SportOfferRepository;
import co.za.unicard.repository.SupplyListRepository;
import co.za.unicard.repository.UniformItemRepository;
import co.za.unicard.repository.UniformStockRepository;
import co.za.unicard.repository.UserAccountRepository;
import co.za.unicard.repository.WalletTransactionRepository;
import co.za.unicard.service.SchoolCalendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static co.za.unicard.util.Money.of;

/**
 * Loads the people and data from the UniSchool prototype so every screen has something to show.
 * Runs once at startup when unischool.seed-demo=true and the database is empty.
 * Every demo account uses the password in unischool.demo-password.
 */
@Component
@Order(1)
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final boolean enabled;
    private final String demoPassword;
    private final PasswordEncoder encoder;
    private final SchoolCalendar cal;
    private final UserAccountRepository accounts;
    private final AccessRequestRepository accessRequests;
    private final LearnerRepository learners;
    private final MenuItemRepository menu;
    private final PreOrderRepository preOrders;
    private final WalletTransactionRepository txs;
    private final SavingsPotRepository pots;
    private final PocketMoneyRuleRepository pocket;
    private final UniformItemRepository uniforms;
    private final SportOfferRepository sport;
    private final SupplyListRepository lists;
    private final PrelovedListingRepository preloved;
    private final UniformStockRepository stock;
    private final SchoolCollectionRepository collections;
    private final FeeAccountRepository fees;
    private final SchoolLedgerRepository ledger;
    private final SchoolSettingsRepository settings;
    private final HomeworkRepository homework;
    private final NoticeRepository notices;
    private final NoticeReceiptRepository receipts;
    private final ParentGroupRepository groups;
    private final GroupMessageRepository messages;

    public DemoDataSeeder(@Value("${unischool.seed-demo:false}") boolean enabled,
                          @Value("${unischool.demo-password:unischool}") String demoPassword,
                          PasswordEncoder encoder, SchoolCalendar cal, UserAccountRepository accounts,
                          AccessRequestRepository accessRequests, LearnerRepository learners, MenuItemRepository menu,
                          PreOrderRepository preOrders, WalletTransactionRepository txs, SavingsPotRepository pots,
                          PocketMoneyRuleRepository pocket, UniformItemRepository uniforms, SportOfferRepository sport,
                          SupplyListRepository lists, PrelovedListingRepository preloved, UniformStockRepository stock,
                          SchoolCollectionRepository collections, FeeAccountRepository fees, SchoolLedgerRepository ledger,
                          SchoolSettingsRepository settings, HomeworkRepository homework, NoticeRepository notices,
                          NoticeReceiptRepository receipts, ParentGroupRepository groups, GroupMessageRepository messages) {
        this.enabled = enabled;
        this.demoPassword = demoPassword;
        this.encoder = encoder;
        this.cal = cal;
        this.accounts = accounts;
        this.accessRequests = accessRequests;
        this.learners = learners;
        this.menu = menu;
        this.preOrders = preOrders;
        this.txs = txs;
        this.pots = pots;
        this.pocket = pocket;
        this.uniforms = uniforms;
        this.sport = sport;
        this.lists = lists;
        this.preloved = preloved;
        this.stock = stock;
        this.collections = collections;
        this.fees = fees;
        this.ledger = ledger;
        this.settings = settings;
        this.homework = homework;
        this.notices = notices;
        this.receipts = receipts;
        this.groups = groups;
        this.messages = messages;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled || accounts.count() > 0) return;
        LocalDateTime now = cal.now();
        LocalDate today = cal.today();
        String hash = encoder.encode(demoPassword);

        settings.save(new SchoolSettings());

        // ---------------- learners ----------------
        Learner kayla = learner("2026101", "Kayla", "Naidoo", "Grade 9", Campus.COLLEGE, "Eland", Set.of("Netball"),
                sizes("Shirt", "13", "Trousers", "30", "Shoes", "6", "Blazer", "34", "Jersey", "34"),
                120, 70, 0, TuckMode.FREE_CHOICE, Set.of("Fizzy drinks"), Set.of(), false);
        kayla.setGoal("Netball shoes", of(900), of(260));
        Learner lerato = learner("2026102", "Lerato", "Mokoena", "Grade 4", Campus.PREP, "Kudu", Set.of("Hockey", "Swimming"),
                sizes("Shirt", "9-10", "Shorts", "9-10", "Shoes", "3", "Blazer", "30", "Jersey", "30"),
                180, 60, 22, TuckMode.PREORDER_ONLY, Set.of("Fizzy drinks"), Set.of(), false);
        lerato.setGoal("Art set", of(350), of(120));
        Learner thabo = learner("2026103", "Thabo", "Mokoena", "Grade 8", Campus.COLLEGE, "Impala", Set.of("Rugby", "Cricket"),
                sizes("Shirt", "15", "Trousers", "32", "Shoes", "8", "Blazer", "38", "Jersey", "38"),
                95, 80, 0, TuckMode.APPROVED_ITEMS, Set.of(),
                Set.of("Toasted cheese", "Mince pie", "Chicken wrap", "Fruit cup", "Yoghurt", "Water 500 ml", "Fruit juice", "Muffin"), false);
        thabo.setGoal("Cricket bat", of(1450), of(610));
        Learner amara = learner("2026104", "Amara", "Khoza", "Grade 5", Campus.PREP, null, Set.of(), Map.of(),
                64, 50, 15, TuckMode.FREE_CHOICE, Set.of(), Set.of(), false);
        Learner kian = learner("2026105", "Kian", "Pillay", "Grade 9", Campus.COLLEGE, null, Set.of(), Map.of(),
                12, 70, 0, TuckMode.FREE_CHOICE, Set.of(), Set.of(), false);
        Learner zoe = learner("2026106", "Zoe", "Venter", "Grade 3", Campus.PREP, null, Set.of(), Map.of(),
                140, 40, 36, TuckMode.FREE_CHOICE, Set.of("Sweets"), Set.of(), false);
        Learner sipho = learner("2026107", "Sipho", "Ndlovu", "Grade 11", Campus.COLLEGE, null, Set.of(), Map.of(),
                220, 100, 0, TuckMode.FREE_CHOICE, Set.of(), Set.of(), true);

        // ---------------- accounts ----------------
        UserAccount naledi = new UserAccount("naledi", hash, "Naledi Mokoena", "Naledi M.", "Parent of two");
        naledi.setWalletBalance(of(2450));
        naledi.addRole(new RoleAssignment(Role.PARENT, "Parent: Lerato and Thabo", null, true));
        accounts.save(naledi);

        UserAccount naidoo = new UserAccount("naidoo", hash, "Priya Naidoo", "Mrs Naidoo", "Teacher and parent");
        naidoo.setWalletBalance(of(860));
        RoleAssignment naidooTeacher = naidoo.addRole(RoleAssignment.teacher("Grade 4 teacher, Highveld Ridge Prep", Campus.PREP, "Grade 4", "4B"));
        naidooTeacher.setVerifiedNote("Verified 2 Feb 2026");
        naidoo.addRole(new RoleAssignment(Role.PARENT, "Parent: Kayla, Highveld Ridge College", null, true));
        accounts.save(naidoo);

        UserAccount leratoAcc = new UserAccount("lerato", hash, "Lerato Mokoena", "Lerato", "Learner");
        leratoAcc.addRole(RoleAssignment.learner(lerato));
        accounts.save(leratoAcc);

        UserAccount khumalo = new UserAccount("khumalo", hash, "Thandeka Khumalo", "Mrs Khumalo", "Principal");
        khumalo.addRole(new RoleAssignment(Role.PRINCIPAL, "Principal", Campus.BOTH, true)).setVerifiedNote("School administrator");
        khumalo.addRole(new RoleAssignment(Role.SCHOOL_OFFICE, "School office, admin access", Campus.BOTH, true)).setVerifiedNote("School administrator");
        accounts.save(khumalo);

        UserAccount vanwyk = new UserAccount("vanwyk", hash, "Johan van Wyk", "Mr van Wyk", "Bursar");
        vanwyk.addRole(new RoleAssignment(Role.SCHOOL_OFFICE, "Bursar", Campus.BOTH, true)).setVerifiedNote("School administrator");
        accounts.save(vanwyk);

        UserAccount sithole = new UserAccount("sithole", hash, "Grace Sithole", "Grace", "Tuckshop operator");
        sithole.addRole(new RoleAssignment(Role.TUCKSHOP, "Highveld Ridge tuckshop", Campus.BOTH, true));
        accounts.save(sithole);

        UserAccount dlamini = new UserAccount("dlamini", hash, "Lwazi Dlamini", "Ms Dlamini", "Teacher");
        dlamini.addRole(RoleAssignment.teacher("Natural Sciences teacher, College", Campus.COLLEGE, "Grade 8", "8A")).setVerifiedNote("Verified 2 Feb 2026");
        accounts.save(dlamini);

        UserAccount botha = new UserAccount("botha", hash, "Pieter Botha", "Mnr Botha", "Teacher");
        botha.addRole(RoleAssignment.teacher("Grade 8 head, College", Campus.COLLEGE, "Grade 8", "8C")).setVerifiedNote("Verified 2 Feb 2026");
        accounts.save(botha);

        lerato.getGuardians().add(naledi);
        thabo.getGuardians().add(naledi);
        kayla.getGuardians().add(naidoo);
        learners.saveAll(List.of(kayla, lerato, thabo, amara, kian, zoe, sipho));

        // People waiting for the school to verify them
        UserAccount sibongile = new UserAccount("sibongile", hash, "Sibongile Mahlangu", "Mrs Mahlangu", "Staff");
        sibongile.addRole(new RoleAssignment(Role.TEACHER, "Grade 5 teacher, Highveld Ridge Prep", Campus.PREP, false));
        accounts.save(sibongile);
        accessRequests.save(new AccessRequest(sibongile, Role.TEACHER, "Grade 5 teacher, Highveld Ridge Prep",
                "Signed up with a staff invite code", Set.of(), now.minusHours(3)));
        UserAccount kabelo = new UserAccount("kabelo", hash, "Kabelo Dube", "Kabelo D.", "Parent");
        kabelo.addRole(new RoleAssignment(Role.PARENT, "Parent of Amara K., Grade 5", null, false));
        accounts.save(kabelo);
        accessRequests.save(new AccessRequest(kabelo, Role.PARENT, "Parent of Amara K., Grade 5",
                "ID matched to the enrolment record", Set.of(amara.getId()), now.minusHours(1)));

        // ---------------- tuckshop ----------------
        Map<String, MenuItem> m = new LinkedHashMap<>();
        for (Object[] row : new Object[][]{
                {"Toasted cheese", 22, "Hot food"}, {"Mince pie", 25, "Hot food"}, {"Chicken wrap", 32, "Hot food"},
                {"Fruit cup", 12, "Healthy"}, {"Yoghurt", 14, "Healthy"}, {"Water 500 ml", 10, "Drinks"},
                {"Fruit juice", 16, "Drinks"}, {"Fizzy drink", 15, "Fizzy drinks"}, {"Muffin", 14, "Snacks"},
                {"Chips", 12, "Snacks"}, {"Sweets", 8, "Sweets"}, {"Ice lolly", 9, "Sweets"}}) {
            MenuItem item = menu.save(new MenuItem((String) row[0], of((Integer) row[1]), (String) row[2]));
            m.put(item.getName(), item);
        }
        List<LocalDate> days = cal.upcomingSchoolDays(5);
        preOrders.save(new PreOrder(lerato, days.get(0), lines(m, "Toasted cheese", "Fruit cup", "Water 500 ml"),
                PreOrder.PayFrom.LEARNER, null, true, now.minusDays(2)));
        preOrders.save(new PreOrder(lerato, days.get(1), lines(m, "Chicken wrap", "Yoghurt"),
                PreOrder.PayFrom.LEARNER, null, false, now.minusDays(1)));
        preOrders.save(new PreOrder(thabo, days.get(0), lines(m, "Mince pie", "Fruit juice"),
                PreOrder.PayFrom.PARENT, naledi, false, now.minusDays(1)));
        preOrders.save(new PreOrder(amara, days.get(0), lines(m, "Chicken wrap"),
                PreOrder.PayFrom.LEARNER, null, false, now.minusDays(1)));
        PreOrder zoeOrder = new PreOrder(zoe, days.get(0), lines(m, "Toasted cheese", "Water 500 ml"),
                PreOrder.PayFrom.LEARNER, null, false, now.minusDays(1));
        zoeOrder.getLines().forEach(l -> l.markCollected(java.time.LocalTime.of(10, 5)));
        zoeOrder.refreshStatus(java.time.LocalTime.of(10, 5));
        preOrders.save(zoeOrder);

        // ---------------- wallet activity ----------------
        txs.save(WalletTransaction.forLearner(lerato, TxType.TUCKSHOP_SALE, "Toasted cheese", of(-22), now.minusHours(1)));
        txs.save(WalletTransaction.forAccount(naledi, TxType.TRANSFER, "Moved to Lerato's wallet", of(-100), now.minusDays(3)));
        txs.save(WalletTransaction.forLearner(lerato, TxType.TRANSFER, "From Naledi", of(100), now.minusDays(3)));
        txs.save(WalletTransaction.forLearner(kayla, TxType.TUCKSHOP_SALE, "Tuckshop: toasted cheese", of(-22), now.minusDays(4)));
        txs.save(WalletTransaction.forAccount(naidoo, TxType.TOP_UP, "Wallet top-up", of(500), now.minusDays(5)));
        txs.save(WalletTransaction.forLearner(thabo, TxType.POCKET_MONEY, "Pocket money", of(50), now.minusDays(3)));
        txs.save(WalletTransaction.forLearner(thabo, TxType.TUCKSHOP_SALE, "Tuckshop: pie, water", of(-35), now.minusDays(4)));
        txs.save(WalletTransaction.forAccount(naledi, TxType.TOP_UP, "Wallet top-up", of(1000), now.minusDays(4)));

        // ---------------- savings and pocket money ----------------
        LocalDate opens = LocalDate.of(today.getYear() + 1, 1, 2);
        SavingsPot naledipot = new SavingsPot(naledi, of(0), of(9600), of(650), opens);
        naledipot.contribute("Uncle Tumi", of(300), today.minusDays(53));
        naledipot.contribute("Debit order", of(650), today.minusDays(30));
        naledipot.contribute("Gogo Mavis", of(500), today.minusDays(13));
        naledipot.contribute("Saved before UniSchool", of(5700), today.minusDays(200));
        pots.save(naledipot);
        SavingsPot naidoopot = new SavingsPot(naidoo, of(0), of(5200), of(400), opens);
        naidoopot.contribute("Debit order", of(400), today.minusDays(30));
        naidoopot.contribute("Saved before UniSchool", of(2500), today.minusDays(200));
        pots.save(naidoopot);
        pocket.save(new PocketMoneyRule(naledi, thabo, of(50), DayOfWeek.MONDAY, true));
        pocket.save(new PocketMoneyRule(naledi, lerato, of(30), DayOfWeek.MONDAY, false));
        pocket.save(new PocketMoneyRule(naidoo, kayla, of(40), DayOfWeek.MONDAY, true));

        // ---------------- shop ----------------
        uniforms.saveAll(List.of(
                new UniformItem(Campus.PREP, "Short-sleeve shirt, white", of(149), UniformItem.Kind.GENERIC, "Shirt", null),
                new UniformItem(Campus.PREP, "Grey shorts", of(179), UniformItem.Kind.GENERIC, "Shorts", null),
                new UniformItem(Campus.PREP, "Black school shoes", of(449), UniformItem.Kind.GENERIC, "Shoes", null),
                new UniformItem(Campus.PREP, "School blazer with crest", of(1150), UniformItem.Kind.CRESTED, "Blazer", "Ridge Outfitters"),
                new UniformItem(Campus.PREP, "Crested V-neck jersey", of(465), UniformItem.Kind.CRESTED, "Jersey", "Ridge Outfitters"),
                new UniformItem(Campus.PREP, "House sports shirt (Kudu)", of(245), UniformItem.Kind.CRESTED, "Shirt", "Schoolwear Co."),
                new UniformItem(Campus.PREP, "Crested winter tracksuit", of(690), UniformItem.Kind.CRESTED, "Jersey", "Ridge Outfitters"),
                new UniformItem(Campus.PREP, "Grey school socks, 3 pairs", of(95), UniformItem.Kind.GENERIC, "Shoes", null),
                new UniformItem(Campus.COLLEGE, "Long-sleeve shirt, white", of(189), UniformItem.Kind.GENERIC, "Shirt", null),
                new UniformItem(Campus.COLLEGE, "Charcoal trousers", of(329), UniformItem.Kind.GENERIC, "Trousers", null),
                new UniformItem(Campus.COLLEGE, "Black lace-up shoes", of(599), UniformItem.Kind.GENERIC, "Shoes", null),
                new UniformItem(Campus.COLLEGE, "College blazer with crest", of(1390), UniformItem.Kind.CRESTED, "Blazer", "Ridge Outfitters"),
                new UniformItem(Campus.COLLEGE, "Crested jersey", of(520), UniformItem.Kind.CRESTED, "Jersey", "Ridge Outfitters"),
                new UniformItem(Campus.COLLEGE, "College tie", of(165), UniformItem.Kind.CRESTED, null, "Schoolwear Co."),
                new UniformItem(Campus.COLLEGE, "College winter tracksuit", of(760), UniformItem.Kind.CRESTED, "Jersey", "Ridge Outfitters"),
                new UniformItem(Campus.COLLEGE, "Grey school socks, 3 pairs", of(110), UniformItem.Kind.GENERIC, "Shoes", null)));
        sport.saveAll(List.of(
                new SportOffer("Hockey", SportOffer.Type.BUY, "Hockey bundle: stick, shin guards, mouthguard, socks", of(1290)),
                new SportOffer("Hockey", SportOffer.Type.RENT, "Composite hockey stick", of(85)),
                new SportOffer("Swimming", SportOffer.Type.BUY, "Swimming bundle: costume, cap, goggles, towel", of(640)),
                new SportOffer("Rugby", SportOffer.Type.BUY, "Rugby bundle: boots, mouthguard, socks, scrum cap", of(1150)),
                new SportOffer("Netball", SportOffer.Type.BUY, "Netball kit: dress, socks, bib set, ball", of(690)),
                new SportOffer("Cricket", SportOffer.Type.BUY, "Cricket whites: shirt, trousers, cap", of(780)),
                new SportOffer("Cricket", SportOffer.Type.RENT, "Junior cricket bat, grade-matched", of(120))));
        supplyList(Campus.PREP, "Grade 4", new Object[][]{
                {"A4 72-page exercise books (feint and margin)", 10, "9.50"}, {"HB pencils, box of 12", 1, "42"},
                {"Pritt glue stick 43 g", 3, "31"}, {"Staedtler eraser", 2, "12"}, {"Colouring pencils, 24", 1, "89"},
                {"30 cm ruler, clear", 1, "14"}, {"Scissors, round tip", 1, "36"}, {"A4 flip file, 30 pockets", 2, "38"},
                {"Homework diary (school issue)", 1, "95"}, {"Book covering, clear roll", 1, "55"}});
        supplyList(Campus.COLLEGE, "Grade 9", new Object[][]{
                {"A4 192-page hardcover books", 9, "39"}, {"Casio fx-82ZA Plus II calculator", 1, "329"},
                {"Blue and black pens, pack of 10", 2, "35"}, {"Geography atlas (school edition)", 1, "289"},
                {"A4 exam pad, 80 sheets", 3, "34"}, {"Lever arch file", 2, "49"}});
        supplyList(Campus.COLLEGE, "Grade 8", new Object[][]{
                {"A4 192-page hardcover books", 8, "39"}, {"Casio fx-82ZA Plus II calculator", 1, "329"},
                {"Maths set", 1, "88"}, {"Blue pens, pack of 10", 2, "35"}, {"Highlighters, 4", 1, "46"},
                {"A4 exam pad, 80 sheets", 3, "34"}, {"Lever arch file", 2, "49"}, {"Graph paper pad", 1, "29"},
                {"Afrikaans dictionary (school edition)", 1, "215"}});
        preloved.saveAll(List.of(
                new PrelovedListing(Campus.PREP, "Prep blazer, size 30", of(450), of(1150), "Blazer", "30", null, null, "Parent, Grade 6", now.minusDays(8)),
                new PrelovedListing(Campus.PREP, "Hockey stick 34in, used one season", of(220), of(690), null, null, "Hockey", null, "Parent, Grade 5", now.minusDays(7)),
                new PrelovedListing(Campus.PREP, "Prep jersey, size 26", of(180), of(465), "Jersey", "26", null, null, "Parent, Grade 2", now.minusDays(6)),
                new PrelovedListing(Campus.PREP, "Black school shoes, size 3", of(200), of(449), "Shoes", "3", null, null, "Parent, Grade 5", now.minusDays(5)),
                new PrelovedListing(Campus.COLLEGE, "College blazer, size 38", of(520), of(1390), "Blazer", "38", null, null, "Parent, Grade 10", now.minusDays(8)),
                new PrelovedListing(Campus.COLLEGE, "Rugby boots, size 8", of(300), of(899), "Shoes", "8", null, null, "Parent, Grade 9", now.minusDays(7)),
                new PrelovedListing(Campus.COLLEGE, "Cricket bat, size 6", of(380), of(1100), null, null, "Cricket", null, "Parent, Grade 11", now.minusDays(6)),
                new PrelovedListing(Campus.COLLEGE, "College jersey, size 42", of(210), of(520), "Jersey", "42", null, null, "Parent, Grade 12", now.minusDays(5))));
        stockLines("Prep blazer", "26", 6, "28", 9, "30", 2, "32", 7, "34", 4);
        stockLines("Prep jersey", "26", 11, "28", 14, "30", 3, "32", 8, "34", 6);
        stockLines("College blazer", "34", 5, "36", 8, "38", 1, "40", 6, "42", 3);
        stockLines("College tie", "One size", 42);

        // ---------------- collections and fees ----------------
        SchoolCollection camp = collections.save(new SchoolCollection("Grade 4 camp, Magaliesberg", of(1850), SchoolCollection.Kind.PAYMENT,
                Campus.PREP, Set.of("Grade 4"), today.plusDays(20), 3, now.minusDays(14)));
        SchoolCollection casual = collections.save(new SchoolCollection("Casual Friday", of(20), SchoolCollection.Kind.PAYMENT,
                Campus.BOTH, Set.of("All"), today.plusDays(6), 1, now.minusDays(10)));
        SchoolCollection tour = collections.save(new SchoolCollection("Grade 8 rugby tour, Durban", of(4200), SchoolCollection.Kind.PAYMENT,
                Campus.COLLEGE, Set.of("Grade 8"), today.plusDays(118), 4, now.minusDays(30)));
        collections.save(new SchoolCollection("Year-end concert tickets", of(60), SchoolCollection.Kind.TICKET,
                Campus.BOTH, Set.of("All"), today.plusDays(34), 1, now.minusDays(7)));
        collections.save(new SchoolCollection("Library fund", of(100), SchoolCollection.Kind.DONATION,
                Campus.BOTH, Set.of("All"), null, 1, now.minusDays(60)));

        int feeYear = today.getYear() + 1;
        fees.saveAll(List.of(
                new FeeAccount(lerato, feeYear, of(96000), null),
                new FeeAccount(thabo, feeYear, of(128400), FeePlan.TERMLY),
                new FeeAccount(kayla, feeYear, of(128400), FeePlan.MONTHLY_10),
                new FeeAccount(amara, feeYear, of(96000), null),
                new FeeAccount(kian, feeYear, of(128400), FeePlan.ANNUAL),
                new FeeAccount(zoe, feeYear, of(96000), FeePlan.MONTHLY_12),
                new FeeAccount(sipho, feeYear, of(128400), null)));

        // ---------------- school ledger (latest payments and payouts) ----------------
        LocalDateTime morning = today.atTime(8, 30);
        ledger.saveAll(List.of(
                new SchoolLedgerEntry(morning, "Grade 8 rugby tour: instalment 2 of 4", of(1050), "J. Mokoena", LedgerSource.COLLECTION),
                new SchoolLedgerEntry(morning.plusMinutes(21), "Fees: monthly debit order", of(9600), "K. Pillay", LedgerSource.FEES),
                new SchoolLedgerEntry(morning.plusMinutes(45), "Casual Friday", of(20), "M. Botha", LedgerSource.COLLECTION),
                new SchoolLedgerEntry(morning.plusMinutes(72), "Grade 4 camp: instalment 1 of 3", new BigDecimal("616.67"), "A. Dlamini", LedgerSource.COLLECTION),
                new SchoolLedgerEntry(today.minusDays(1).atTime(12, 0), "Collections and fees", of(118940), null, LedgerSource.FEES),
                new SchoolLedgerEntry(today.minusDays(1).atTime(12, 0), "Uniform shop handling fee", of(2310), null, LedgerSource.UNIFORM_HANDLING),
                new SchoolLedgerEntry(today.minusDays(3).atTime(12, 0), "Collections and fees", of(96420), null, LedgerSource.FEES),
                new SchoolLedgerEntry(today.withDayOfMonth(1).atTime(9, 0), "UniSchool give-back, last month", of(4120), null, LedgerSource.SHOP_GIVEBACK),
                new SchoolLedgerEntry(LocalDate.of(today.getYear(), 1, 31).atTime(9, 0), "UniSchool give-back, earlier this year", of(14330), null, LedgerSource.SHOP_GIVEBACK)));

        // ---------------- homework ----------------
        homework.saveAll(List.of(
                new Homework(Campus.PREP, "Grade 4", "Maths", "Fractions worksheet, page 12", "Complete questions 1 to 10. Show your working.",
                        days.get(1), "Fractions_p12.pdf", naidoo, "Mrs Naidoo", PublishStatus.PUBLISHED, now.minusHours(2)),
                new Homework(Campus.PREP, "Grade 4", "English", "Read chapter 3 of your class reader", "Write five sentences about your favourite character.",
                        days.get(3), null, naidoo, "Mrs Naidoo", PublishStatus.PUBLISHED, now.minusDays(1)),
                new Homework(Campus.COLLEGE, "Grade 8", "Natural Sciences", "Simple circuit project: plan", "Draw your circuit and list the parts you need. Bring it to class.",
                        days.get(4), "Circuit_project_brief.pdf", dlamini, "Ms Dlamini", PublishStatus.PUBLISHED, now.minusDays(3)),
                new Homework(Campus.COLLEGE, "Grade 8", "Afrikaans", "Leer woordeskatlys 4", "Woordtoets op Donderdag.",
                        days.get(1), null, botha, "Mnr Botha", PublishStatus.PUBLISHED, now.minusDays(3))));

        // ---------------- notices ----------------
        notices.save(new Notice(naidoo, "Mrs Naidoo, Grade 4 teacher", Campus.PREP, Set.of("Grade 4"), Set.of(), "Action needed",
                "Magaliesberg camp: kit list and consent",
                "Camp runs for three days next month. Please read the kit list and confirm consent for your child by Friday. Camp fees can be paid in three instalments.",
                "Camp kit list.pdf", true, false, camp, PublishStatus.PUBLISHED, now.minusHours(3)));
        notices.save(new Notice(naidoo, "Mrs Naidoo, Grade 4 teacher", Campus.PREP, Set.of("Grade 4"), Set.of(), "Event",
                "Grade 4 market day",
                "Learners can sell home-made snacks or crafts. Stalls cost R20, paid on the day through the tuckshop till. Please help your child plan what to sell.",
                null, false, false, null, PublishStatus.PENDING, now.minusHours(2)));
        notices.save(new Notice(null, "Sports office", Campus.PREP, Set.of(), Set.of("Hockey"), "Change",
                "U10 hockey trials moved to Thursday",
                "Trials are now on Thursday at 15:00 on the astro. Bring a water bottle and shin guards. Sticks are available to borrow.",
                null, false, false, null, PublishStatus.PUBLISHED, now.minusDays(1)));
        Notice early = notices.save(new Notice(khumalo, "Principal's office", Campus.BOTH, Set.of("All"), Set.of(), "Reminder",
                "Early closing on Friday",
                "School closes at 12:00 for staff development. Aftercare runs as normal. Casual Friday contributions go to the library fund.",
                null, false, false, casual, PublishStatus.PUBLISHED, now.minusDays(4)));
        notices.save(new Notice(botha, "Mnr Botha, Grade 8 head", Campus.COLLEGE, Set.of("Grade 8"), Set.of(), "Event",
                "Grade 8 subject choice evening",
                "Tuesday at 18:00 in the main hall. Learners and at least one parent should attend. Please let us know if you are coming.",
                "Subject guide.pdf", false, true, null, PublishStatus.PUBLISHED, now.minusDays(3)));
        Notice passports = notices.save(new Notice(null, "Coach Pretorius", Campus.COLLEGE, Set.of(), Set.of("Rugby"), "Action needed",
                "Durban tour: passport copies due Friday",
                "Upload a copy of your child's passport or ID through the link in this notice. Tour fees can be paid in four instalments.",
                null, true, false, tour, PublishStatus.PUBLISHED, now.minusDays(8)));
        receipts.save(new NoticeReceipt(early, naledi, null, NoticeReceipt.Type.READ, null, now.minusDays(4)));
        receipts.save(new NoticeReceipt(passports, naledi, null, NoticeReceipt.Type.READ, null, now.minusDays(7)));

        // ---------------- parent groups ----------------
        ParentGroup g1 = groups.save(new ParentGroup("Grade 4 parents", Campus.PREP, "Grade 4", null, "Thandi K., class rep", null, ParentGroup.Mode.OPEN));
        ParentGroup g2 = groups.save(new ParentGroup("U10 hockey parents", Campus.PREP, null, "Hockey", "Coach Daniels", null, ParentGroup.Mode.OPEN));
        ParentGroup g3 = groups.save(new ParentGroup("Grade 8 parents", Campus.COLLEGE, "Grade 8", null, "Sizwe M., class rep", null, ParentGroup.Mode.OPEN));
        ParentGroup g5 = groups.save(new ParentGroup("Grade 9 parents", Campus.COLLEGE, "Grade 9", null, "Lindiwe Z., class rep", null, ParentGroup.Mode.OPEN));
        ParentGroup g4 = groups.save(new ParentGroup("U14 rugby parents", Campus.COLLEGE, null, "Rugby", "Coach Pretorius", null, ParentGroup.Mode.ANNOUNCE));
        LocalDateTime t = today.atTime(7, 58);
        msg(g1, "Thandi K.", "Class rep", "Morning all. The camp kit list and consent form are on the notice board, so no need to forward photos of it.", t);
        msg(g1, "Priya S.", null, "Does anyone have a child-size sleeping bag we could borrow for camp?", t.plusMinutes(16));
        msg(g1, "Johan V.", null, "We have one. I'll send it with Ruan tomorrow.", t.plusMinutes(22));
        msg(g1, "Lindiwe M.", null, "Lift club for camp drop-off at 06:30? We're in Fourways and have two seats.", t.plusMinutes(64));
        LocalDateTime y = today.minusDays(1).atTime(16, 0);
        msg(g2, "Coach Daniels", "Coach", "Trials are now Thursday at 15:00. Details are on the notice board.", y);
        msg(g2, "Ayesha D.", null, "Will there be spare sticks for kids trying out?", y.plusMinutes(10));
        msg(g2, "Coach Daniels", "Coach", "Yes, a few. You can also rent one through UniSchool.", y.plusMinutes(25));
        LocalDateTime tu = today.minusDays(3).atTime(18, 0);
        msg(g3, "Sizwe M.", "Class rep", "Subject choice evening is Tuesday at 18:00 in the hall. Please RSVP on the notice board.", tu);
        msg(g3, "Karen B.", null, "Is the subject guide the same as last year's?", tu.plusMinutes(12));
        msg(g3, "Sizwe M.", "Class rep", "It's updated. The new version is attached to the notice.", tu.plusMinutes(20));
        LocalDateTime mo = today.minusDays(4).atTime(19, 0);
        msg(g5, "Lindiwe Z.", "Class rep", "Subject choices for Grade 10 open next month. The school will post details on the notice board.", mo);
        msg(g5, "Craig F.", null, "Anyone selling a Grade 9 atlas? Ours went missing.", mo.plusMinutes(30));
        msg(g4, "Coach Pretorius", "Coach", "Durban tour: passport copies are due Friday. Upload them on the notice board, not here please.", today.minusDays(8).atTime(17, 0));

        log.info("UniSchool demo data loaded. Sign in as naledi, naidoo, lerato, khumalo, vanwyk or sithole (password: {}).", demoPassword);
    }

    // ---------------- helpers ----------------
    private Learner learner(String number, String first, String last, String grade, Campus campus, String house,
                            Set<String> sports, Map<String, String> sizes, long wallet, long limit, long spentToday,
                            TuckMode mode, Set<String> blocked, Set<String> allowed, boolean frozen) {
        Learner l = new Learner(number, first, last, grade, campus, house);
        l.getSports().addAll(sports);
        l.getSizes().putAll(sizes);
        l.setWalletBalance(of(wallet));
        l.setDailyLimit(of(limit));
        if (spentToday > 0) l.recordSpend(of(spentToday), cal.today());
        l.setTuckMode(mode);
        l.getBlockedCategories().addAll(blocked);
        l.getAllowedItems().addAll(allowed);
        l.setFrozen(frozen);
        return learners.save(l);
    }

    private static Map<String, String> sizes(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    private static List<PreOrderLine> lines(Map<String, MenuItem> menu, String... names) {
        return Arrays.stream(names).map(menu::get)
                .map(i -> new PreOrderLine(i.getName(), i.getCategory(), i.getPrice(), 1)).toList();
    }

    private void supplyList(Campus campus, String grade, Object[][] rows) {
        SupplyList list = new SupplyList(campus, grade);
        list.replaceItems(Arrays.stream(rows)
                .map(r -> new SupplyListItem((String) r[0], (Integer) r[1], new BigDecimal((String) r[2]).setScale(2))).toList());
        list.setStatus(SupplyList.Status.PUBLISHED);
        lists.save(list);
    }

    private void stockLines(String item, Object... sizeQty) {
        for (int i = 0; i < sizeQty.length; i += 2) stock.save(new UniformStock(item, (String) sizeQty[i], (Integer) sizeQty[i + 1]));
    }

    private void msg(ParentGroup g, String who, String role, String body, LocalDateTime at) {
        messages.save(new GroupMessage(g, null, who, role, body, at, at));
    }
}
