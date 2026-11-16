package com.example.carshop.controller;

import com.example.carshop.repository.*;
import com.example.carshop.dto.CarRequest;
import com.example.carshop.entity.*;
import com.example.carshop.service.CarService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import java.nio.file.*;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import com.jayway.jsonpath.JsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "spring.jpa.properties.hibernate.query.fail_on_pagination_over_collection_fetch=true"
})
@AutoConfigureMockMvc
@TestConstructor(autowireMode=TestConstructor.AutowireMode.ALL)
class CarShopApiTests {
    static final Path UPLOADS;
    static { try { UPLOADS = Files.createTempDirectory("carshop-test-"); } catch(IOException e) { throw new ExceptionInInitializerError(e); } }
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:carshop-test;DB_CLOSE_DELAY=-1");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("app.upload-dir", () -> UPLOADS.toString());
    }
    private final MockMvc mvc;
    private final CarRepository cars;
    private final ManufacturerRepository manufacturers;
    private final DealershipRepository dealerships;
    private final FeatureRepository features;
    private final CarService carService;
    private final TransactionTemplate transactions;
    private final JdbcTemplate jdbc;
    private final EntityManagerFactory entityManagerFactory;
    long manufacturer, dealership, feature;
    CarShopApiTests(MockMvc mvc, CarRepository cars, ManufacturerRepository manufacturers,
                    DealershipRepository dealerships, FeatureRepository features, CarService carService,
                    PlatformTransactionManager transactionManager, JdbcTemplate jdbc, EntityManagerFactory entityManagerFactory) {
        this.carService=carService; this.transactions=new TransactionTemplate(transactionManager);
        this.jdbc=jdbc; this.entityManagerFactory=entityManagerFactory;
        this.mvc=mvc; this.cars=cars; this.manufacturers=manufacturers; this.dealerships=dealerships; this.features=features;
    }
    @BeforeEach
    void setup() throws Exception {
        cars.deleteAll(); features.deleteAll(); dealerships.deleteAll(); manufacturers.deleteAll();
        try(var files=Files.list(UPLOADS)) { for(var file:files.toList()) Files.delete(file); }
        manufacturer = create("manufacturers", "{\"name\":\"Toyota\"}");
        feature = create("features", "{\"name\":\"Heated seats\"}");
        dealership = create("dealerships", "{\"name\":\"Tbilisi Cars\",\"email\":\"sales@example.com\"}");
    }
    long create(String path, String json) throws Exception {
        String response=mvc.perform(post("/api/"+path).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn().getResponse().getContentAsString();
        return ((Number)JsonPath.read(response,"$.id")).longValue();
    }
    String createCar(String vin, byte[] photo) throws Exception {
        var request=multipart("/api/cars").param("model","Camry").param("vin",vin)
            .param("modelYear","2024").param("price","24000.00")
            .param("manufacturerId",""+manufacturer).param("dealershipId",""+dealership).param("featureIds",""+feature);
        if(photo!=null) request.file(new MockMultipartFile("photo","car.png","image/png",photo));
        return mvc.perform(request).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }
    String payload(String model) {
        return "{\"model\":\""+model+"\",\"vin\":\"1HGCM82633A004352\",\"modelYear\":2025,\"price\":22000,\"manufacturerId\":"
            +manufacturer+",\"dealershipId\":"+dealership+",\"featureIds\":["+feature+"]}";
    }
    byte[] png() throws Exception {
        var bytes=new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",bytes); return bytes.toByteArray();
    }
    @Test void fullCrudAndRelationships() throws Exception {
        String body=createCar("1HGCM82633A004352",null);
        Number id=JsonPath.read(body,"$.id");
        mvc.perform(get("/api/cars/"+id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.manufacturer.name").value("Toyota"))
            .andExpect(jsonPath("$.features[0].name").value("Heated seats"));
        mvc.perform(put("/api/cars/"+id).contentType(MediaType.APPLICATION_JSON).content(payload("Corolla")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.model").value("Corolla"));
        for(String path:new String[]{"manufacturers/"+manufacturer,"features/"+feature,"dealerships/"+dealership})
            mvc.perform(delete("/api/"+path)).andExpect(status().isConflict());
        mvc.perform(delete("/api/cars/"+id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/cars/"+id)).andExpect(status().isNotFound());
        for(String path:new String[]{"manufacturers/"+manufacturer,"features/"+feature,"dealerships/"+dealership})
            mvc.perform(delete("/api/"+path)).andExpect(status().isNoContent());
    }
    @Test void filtersPaginationAndSort() throws Exception {
        createCar("1HGCM82633A004352",null); createCar("1HGCM82633A004353",null);
        mvc.perform(get("/api/cars").param("keyword","toyota").param("featureId",""+feature)
            .param("manufacturerId",""+manufacturer).param("dealershipId",""+dealership)
            .param("minPrice","23000").param("maxPrice","25000").param("page","1").param("size","1").param("sort","vin,desc"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].vin").value("1HGCM82633A004352"));
        mvc.perform(get("/api/cars").param("maxPrice","100")).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/cars").param("sort","unknown,asc")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/cars").param("minPrice","500").param("maxPrice","100")).andExpect(status().isBadRequest());
    }
    @Test void validationDuplicateAndMissingRelationships() throws Exception {
        mvc.perform(post("/api/dealerships").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"A\",\"email\":\"bad\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.email").exists());
        mvc.perform(post("/api/features").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"heated seats\"}"))
            .andExpect(status().isConflict());
        mvc.perform(multipart("/api/cars").param("model"," ").param("price","-1"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.model").exists());
        createCar("1HGCM82633A004352",null);
        mvc.perform(multipart("/api/cars").param("model","X").param("vin","1HGCM82633A004352")
            .param("modelYear","2024").param("price","100").param("manufacturerId",""+manufacturer)
            .param("dealershipId",""+dealership).param("featureIds",""+feature)).andExpect(status().isConflict());
        mvc.perform(put("/api/cars/"+cars.findAll().get(0).getId()).contentType(MediaType.APPLICATION_JSON)
            .content(payload("X").replace("\"manufacturerId\":"+manufacturer,"\"manufacturerId\":999999")))
            .andExpect(status().isNotFound());
    }
    @Test void uploadsAreStoredReplacedAndDeleted() throws Exception {
        byte[] image=png(); String body=createCar("1HGCM82633A004352",image);
        Number id=JsonPath.read(body,"$.id"); String filename=JsonPath.read(body,"$.photoPath");
        assertArrayEquals(image,Files.readAllBytes(UPLOADS.resolve(filename)));
        mvc.perform(get("/api/cars/"+id+"/photo")).andExpect(status().isOk()).andExpect(content().bytes(image));
        mvc.perform(multipart("/api/cars/"+id+"/photo").file(new MockMultipartFile("photo","new.png","image/png",image))
            .with(req -> { req.setMethod("PUT");return req; })).andExpect(status().isOk());
        assertFalse(Files.exists(UPLOADS.resolve(filename)));
        mvc.perform(delete("/api/cars/"+id)).andExpect(status().isNoContent());
        try(var files=Files.list(UPLOADS)) { assertEquals(0,files.count()); }
    }
    @Test void invalidPhotoLeavesNoDatabaseRowOrFile() throws Exception {
        mvc.perform(multipart("/api/cars").file(new MockMultipartFile("photo","fake.png","image/png","not an image".getBytes()))
            .param("model","Camry").param("vin","1HGCM82633A004352").param("modelYear","2024").param("price","24000")
            .param("manufacturerId",""+manufacturer).param("dealershipId",""+dealership).param("featureIds",""+feature))
            .andExpect(status().isBadRequest());
        assertEquals(0,cars.count());
        try(var files=Files.list(UPLOADS)) { assertEquals(0,files.count()); }
        String body=createCar("1HGCM82633A004352",null);Number id=JsonPath.read(body,"$.id");
        mvc.perform(multipart("/api/cars/"+id+"/photo").file(new MockMultipartFile("photo","fake.jpg","image/jpeg",png()))
            .with(req -> { req.setMethod("PUT"); return req; })).andExpect(status().isBadRequest());
        mvc.perform(get("/api/cars/"+id+"/photo")).andExpect(status().isNotFound());
    }
    @Test void referenceUpdatesSearchAndGet() throws Exception {
        for(String path:new String[]{"manufacturers/"+manufacturer,"features/"+feature,"dealerships/"+dealership}) {
            mvc.perform(get("/api/"+path)).andExpect(status().isOk());
            mvc.perform(put("/api/"+path).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Updated\",\"description\":\"Updated description\",\"email\":\"new@example.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated"));
            String collection=path.substring(0,path.indexOf('/'));
            mvc.perform(get("/api/"+collection).param("name","updated").param("size","1").param("sort","name,desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        }
    }
    @Test void databaseRejectsConcurrentCaseVariantsAndConflictingUpdates() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (String table : List.of("manufacturer", "feature", "dealership")) {
                CountDownLatch start = new CountDownLatch(1);
                List<Future<Boolean>> outcomes = new ArrayList<>();
                for (String name : List.of("RaceMaker", "RACEMAKER")) {
                    outcomes.add(executor.submit(() -> {
                        assertTrue(start.await(5, TimeUnit.SECONDS));
                        try {
                            String sql = table.equals("dealership")
                                ? "insert into dealership(name, email) values (?, 'race@example.com')"
                                : "insert into " + table + "(name) values (?)";
                            jdbc.update(sql, name);
                            return true;
                        } catch (DataIntegrityViolationException expected) { return false; }
                    }));
                }
                start.countDown();
                int successes = 0;
                for (var outcome : outcomes) if (outcome.get(10, TimeUnit.SECONDS)) successes++;
                assertEquals(1, successes, table + " must accept exactly one concurrent insert");
                assertEquals(1, jdbc.queryForObject("select count(*) from " + table + " where normalized_name='racemaker'", Integer.class));
                String display = jdbc.queryForObject("select name from " + table + " where normalized_name='racemaker'", String.class);
                assertTrue(Set.of("RaceMaker", "RACEMAKER").contains(display));
                long id = switch (table) { case "manufacturer" -> manufacturer; case "feature" -> feature; default -> dealership; };
                assertThrows(DataIntegrityViolationException.class,
                    () -> jdbc.update("update " + table + " set name=' racemaker ' where id=?", id));
            }
        } finally { executor.shutdownNow(); }
    }

    private CarRequest validRequest() {
        CarRequest request = new CarRequest();
        request.setModel("Camry"); request.setVin("1HGCM82633A004352"); request.setModelYear(2024);
        request.setPrice(new BigDecimal("24000.00")); request.setManufacturerId(manufacturer);
        request.setDealershipId(dealership); request.setFeatureIds(Set.of(feature));
        return request;
    }
    private void forceDatabaseFailure() {
        // This bypasses application prechecks and fails in the database after the photo is stored.
        jdbc.update("insert into feature(name) values ('HEATED SEATS')");
    }
    private List<Path> storedFiles() throws IOException {
        try (var files = Files.list(UPLOADS)) { return files.sorted().toList(); }
    }
    @Test void failedCreationRollsBackDatabaseAndRemovesValidStoredPhoto() throws Exception {
        byte[] image = png();
        assertThrows(DataIntegrityViolationException.class, () -> transactions.executeWithoutResult(status -> {
            var created = carService.create(validRequest(), new MockMultipartFile("photo", "car.png", "image/png", image));
            assertTrue(Files.isRegularFile(UPLOADS.resolve(created.photoPath())));
            assertEquals(1, cars.count());
            forceDatabaseFailure();
        }));
        assertEquals(0, cars.count());
        assertTrue(storedFiles().isEmpty());
    }
    @Test void rolledBackReplacementPreservesOriginalPhotoAndRemovesReplacement() throws Exception {
        byte[] original = png();
        var created = carService.create(validRequest(), new MockMultipartFile("photo", "old.png", "image/png", original));
        Path oldPath = UPLOADS.resolve(created.photoPath());
        assertThrows(DataIntegrityViolationException.class, () -> transactions.executeWithoutResult(status -> {
            var updated = carService.updatePhoto(created.id(), new MockMultipartFile("photo", "new.png", "image/png", original));
            assertNotEquals(created.photoPath(), updated.photoPath());
            assertTrue(Files.isRegularFile(UPLOADS.resolve(updated.photoPath())));
            assertTrue(Files.isRegularFile(oldPath));
            forceDatabaseFailure();
        }));
        assertEquals(created.photoPath(), carService.get(created.id()).photoPath());
        assertEquals(List.of(oldPath), storedFiles());
        assertArrayEquals(original, Files.readAllBytes(oldPath));
    }
    @Test void paginatedListsUseBoundedQueriesWithDistinctRelationships() {
        // Distinct related records prevent first-level caching from hiding an N+1 regression.
        for (int i = 0; i < 21; i++) {
            Manufacturer maker = new Manufacturer(); maker.setName("Maker " + i); manufacturers.saveAndFlush(maker);
            Dealership dealer = new Dealership(); dealer.setName("Dealer " + i); dealer.setEmail("dealer" + i + "@example.com"); dealerships.saveAndFlush(dealer);
            Feature extra = new Feature(); extra.setName("Feature " + i); features.saveAndFlush(extra);
            CarRequest request = validRequest(); request.setVin("1HGCM82633A" + String.format("%06d", i));
            request.setManufacturerId(maker.getId()); request.setDealershipId(dealer.getId());
            request.setFeatureIds(Set.of(feature, extra.getId())); carService.create(request, null);
        }
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        Set<Long> seen = new HashSet<>();
        for (int page = 0; page < 2; page++) {
            statistics.clear();
            var result = carService.list("", null, null, feature, null, null, PageRequest.of(page, 10, Sort.by("id")));
            assertEquals(21, result.getTotalElements()); assertEquals(3, result.getTotalPages());
            assertEquals(10, result.getNumberOfElements());
            for (var car : result) {
                assertTrue(seen.add(car.id()), "Pages must not overlap");
                assertNotNull(car.manufacturer().name()); assertNotNull(car.dealership().email());
                assertEquals(2, car.features().size());
            }
            assertTrue(statistics.getPrepareStatementCount() <= 3,
                "Expected page, count, and batched features only; got " + statistics.getPrepareStatementCount());
        }
    }
    @AfterAll static void cleanupFiles() throws Exception {
        try(var files=Files.list(UPLOADS)) { for(var file:files.toList()) Files.delete(file); }
        Files.deleteIfExists(UPLOADS);
    }
}
