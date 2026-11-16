# Car Shop

A Spring Boot backend for managing cars, manufacturers, features, dealerships, and car photos. It follows the supplied Library Catalog reference's Maven, Java 17, H2, and package structure, with a new car shop implementation.

## Run

Install JDK 17 or newer. In PowerShell, from this directory:

```powershell
.\mvnw.cmd spring-boot:run
```

Open http://localhost:8080/ for the browser demo. Create a manufacturer, a feature, and a dealership, then add a car. The demo supports creating/editing/deleting records, uploading/replacing photos, searching, price filtering, sorting, and pagination. The initial database is empty.

On macOS/Linux use `sh mvnw spring-boot:run`. The wrapper downloads Maven on its first run; no separate Maven installation is necessary. Internet access is needed to download dependencies.

In IntelliJ IDEA, open `pom.xml` as a Maven project, select JDK 17 or newer, and run `CarShopApplication`. Unlink the previous Gradle project if IntelliJ still shows the original starter configuration.

The application creates a persistent H2 database in `data/` and saves images in `uploads/`. Both folders are excluded from Git. Run from the project root so the same paths are used. Stop the app before starting another instance against the same database.

H2 console: http://localhost:8080/h2-console. JDBC URL: `jdbc:h2:file:./data/carshopdb`, user `sa`, password empty.

This is a local coursework application without authentication. Prices are in USD. JPEG and PNG photos are accepted, with a 5 MB file limit and a 20 megapixel image limit. Image contents must match the supplied MIME type. Client filenames are replaced with UUID names. The stored filename appears in `photoPath`; download photos from `/api/cars/{id}/photo`.

## Build and test

```powershell
.\mvnw.cmd clean verify
java -jar target/car-shop-0.0.1-SNAPSHOT.jar
```

On macOS/Linux use `sh mvnw clean verify`. Tests use in-memory H2 databases and temporary image storage. They cover CRUD, relationships, validation, duplicate VINs/names, protected deletes, combined filters, pagination/sorting, image upload/download/replacement/deletion, and fake or mismatched image rejection.

## Project structure

```text
.mvn/wrapper/                 Maven wrapper configuration
pom.xml                      Dependencies and Java version
src/main/java/com/example/carshop/
    CarShopApplication.java
    controller/              REST endpoints and pagination checks
    service/                 Business rules, transactions, mapping, photo storage
    repository/              Spring Data JPA repositories and queries
    entity/                  Car, Manufacturer, Feature, Dealership
    dto/                     Validated request objects and response objects
    config/                  Upload-directory configuration
    exception/               HTTP error handling
src/main/resources/
    application.properties
    static/index.html        Multipart HTML demo
src/test/java/com/example/carshop/
    controller/CarShopApiTests.java
```

Spring components use role annotations and constructor injection. Entities use `@Entity`; DTOs and ordinary helper classes are not Spring beans.

## Data model

- A **Manufacturer** has many cars; each car has one manufacturer (`@OneToMany` / `@ManyToOne`).
- A **Dealership** has many cars; each car has one dealership (`@OneToMany` / `@ManyToOne`).
- A **Car** has one or more features; each **Feature** can belong to many cars (`@ManyToMany`, `car_features` join table).
- Cars have a unique 17-character VIN, model, model year, positive price, optional description, and optional stored photo filename.
- Manufacturer, feature, and dealership names must be unique ignoring case. Dealerships require a valid email.

Deleting an assigned manufacturer, feature, or dealership returns 409. Delete or reassign its cars first. Deleting a car removes its join-table entries and photo while preserving catalog records. New image files are removed on database rollback; old photos are removed after a successful replacement or car deletion.

## API

All endpoints are under `/api`. Requests use JSON except car creation and photo replacement, which use `multipart/form-data`.

| Resource | Operations |
| --- | --- |
| `/manufacturers`, `/features`, `/dealerships` | GET paginated list; POST create |
| `/manufacturers/{id}`, `/features/{id}`, `/dealerships/{id}` | GET one; PUT full update; DELETE |
| `/cars` | GET paginated/filtered list; POST multipart create |
| `/cars/{id}` | GET one; PUT full JSON update; DELETE |
| `/cars/{id}/photo` | GET image; PUT multipart replacement using `photo` field |

Creates return 201 with a Location header; deletes return 204. Invalid requests return 400, missing records return 404, duplicates/assigned-record deletes return 409, and multipart size overflows return 413. Validation responses include a `message` and an `errors` map keyed by field.

Manufacturer/feature JSON: `name` (required, max 80), `description` (optional, max 500). Dealership JSON additionally requires `email` (max 120).

Car fields: `model` (required, max 100), `vin` (17 VIN characters excluding I/O/Q, normalized to uppercase), `modelYear` (1886–2100), `price` (0.01–9999999999.99, at most two decimals), `description` (optional, max 1000), `manufacturerId`, `dealershipId`, and `featureIds` (1–30 existing feature IDs). Multipart creation optionally accepts `photo`; JSON updates preserve the current photo. PUT replaces all editable details; send all required fields.

Every list accepts zero-based `page`, `size` (default 10, maximum 100), and `sort=field,asc` or `sort=field,desc`. Repeat `sort` for multiple fields. Default sorting is by ID; ID also breaks ties. Allowed car sort fields: `id`, `model`, `modelYear`, `price`, `vin`. Other lists allow `id`, `name`. Responses contain `content`, `totalElements`, `totalPages`, `number`, and `size`.

Catalog lists support case-insensitive `name` filtering. Car lists combine `keyword` (model/manufacturer), `manufacturerId`, `dealershipId`, `featureId`, `minPrice`, and `maxPrice` filters.

### PowerShell examples

Run the app in a separate terminal. IDs come from API responses:

```powershell
$base = 'http://localhost:8080/api'
$manufacturer = Invoke-RestMethod "$base/manufacturers" -Method Post -ContentType 'application/json' -Body (@{name='Toyota'} | ConvertTo-Json)
$feature = Invoke-RestMethod "$base/features" -Method Post -ContentType 'application/json' -Body (@{name='Heated seats'} | ConvertTo-Json)
$dealership = Invoke-RestMethod "$base/dealerships" -Method Post -ContentType 'application/json' -Body (@{name='Tbilisi Cars'; email='sales@example.com'} | ConvertTo-Json)

# Replace the image path. Use curl.exe to avoid the PowerShell curl alias.
$car = (curl.exe --fail-with-body -s -X POST "$base/cars" -F 'model=Camry' -F 'vin=1HGCM82633A004352' -F 'modelYear=2024' -F 'price=24000.00' -F "manufacturerId=$($manufacturer.id)" -F "dealershipId=$($dealership.id)" -F "featureIds=$($feature.id)" -F 'photo=@C:/path/to/car.jpg;type=image/jpeg') | ConvertFrom-Json

Invoke-RestMethod "$base/cars?keyword=toyota&minPrice=10000&maxPrice=30000&page=0&size=5&sort=price,asc"
Invoke-RestMethod "$base/cars/$($car.id)"

$update = @{model='Camry Hybrid'; vin='1HGCM82633A004352'; modelYear=2024; price=26000; manufacturerId=$manufacturer.id; dealershipId=$dealership.id; featureIds=@($feature.id)} | ConvertTo-Json
Invoke-RestMethod "$base/cars/$($car.id)" -Method Put -ContentType 'application/json' -Body $update
curl.exe --fail-with-body -X PUT "$base/cars/$($car.id)/photo" -F 'photo=@C:/path/to/new.png;type=image/png'
curl.exe --fail-with-body "$base/cars/$($car.id)/photo" --output downloaded-car.png
Invoke-RestMethod "$base/cars/$($car.id)" -Method Delete
```

Omit `photo` to create a car without an image. The browser demo offers a file picker.

## Requirements checklist

| Requirement | Implementation |
| --- | --- |
| Layered architecture and stereotypes | Separate controller, service, repository, entity, DTO, configuration, exception packages |
| Constructor injection | Constructor dependencies throughout; no field injection |
| Minimum three entities | Four: Car, Manufacturer, Feature, Dealership |
| Two relationship types | Many-to-one/one-to-many and many-to-many |
| Spring Data interfaces | Four `JpaRepository` interfaces |
| Two method-name queries | `findByVinIgnoreCase`, `findByNameContainingIgnoreCase`, `findByNameIgnoreCase`, relationship existence checks |
| Two custom queries | `CarRepository.search` (combined filters) and `findDetailedById` (fetch joins), using `@Query` |
| Transactions | Modifying business methods use `@Transactional`; read-only transactions for reads |
| Methods and request binding | GET/POST/PUT/DELETE; `@PathVariable`, `@RequestParam`, `@RequestBody`, `@ModelAttribute` |
| Validation | `@Valid`, `@NotBlank`, `@NotNull`, `@Size`, `@Min`, `@Max`, `@Email`, `@Pattern`, `@Positive`, `@DecimalMin`, `@Digits` |
| File upload | `MultipartFile`, HTML `enctype="multipart/form-data"`, local images, persisted `photoPath` |
| Pagination and sorting | Spring Data `Pageable`, `PageRequest`, `Sort` on every list |
| Two modest additions | Fourth entity (Dealership) and a usable HTML demo |

## Push to GitHub

The requested submission destination is **https://github.com/mishakakhniashvili/SpringBoot-midterm_project**. It differs from the screenshot/reference repository (`Spring-Boot_midterm_project`). These commands use the requested destination.

This folder initially inherited Git from its parent home directory. Initialize Git **inside this project** before staging so unrelated home files are excluded. Nothing has been committed or pushed automatically.

```powershell
Set-Location C:\Users\misha\IdeaProjects\carShop
git init -b main
# Confirm this prints C:/Users/misha/IdeaProjects/carShop:
git rev-parse --show-toplevel
git add .
git status --short
# Confirm Java sources are included and data/uploads/target are absent.
git commit -m "Build car shop backend with JPA, validation, pagination and photo uploads"
git remote add origin https://github.com/mishakakhniashvili/SpringBoot-midterm_project.git
git push -u origin main
```

GitHub may prompt you to sign in. If `origin` already exists, replace the add command with `git remote set-url origin https://github.com/mishakakhniashvili/SpringBoot-midterm_project.git`. If existing remote commits reject the push, preserve them with `git pull --no-rebase origin main --allow-unrelated-histories`, resolve conflicts, commit, and push again. Do not force-push over existing work.
