package com.example.carshop;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:smoke", "app.upload-dir=target/smoke-uploads"})
class CarShopApplicationTests {

    @Test
    void contextLoads() {
    }

}
