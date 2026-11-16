package com.example.carshop.controller;

import com.example.carshop.repository.*;
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

@SpringBootTest
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
    long manufacturer, dealership, feature;
    CarShopApiTests(MockMvc mvc, CarRepository cars, ManufacturerRepository manufacturers,
                    DealershipRepository dealerships, FeatureRepository features) {
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
    @AfterAll static void cleanupFiles() throws Exception {
        try(var files=Files.list(UPLOADS)) { for(var file:files.toList()) Files.delete(file); }
        Files.deleteIfExists(UPLOADS);
    }
}
