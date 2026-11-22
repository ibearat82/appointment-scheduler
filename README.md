# Doctor's Office Appointment Scheduling App

CST-323 Cloud Computing, CLC Project, **Group A2**: Imam Bearat and Eliezer Centeno Paz

This is a Spring Boot web app for a doctor's office. Patients register, see open times, book a visit, and can reschedule or cancel it. Office staff manage visit types, open time slots, and see every appointment.

**Milestone 3:** iteration 2, running in the cloud on **Microsoft Azure**.

**Live app:** https://appt-scheduler-a2.kindpond-56c86e8c.westus2.azurecontainerapps.io

## Tech stack
| Layer | Technology |
|---|---|
| Language | Java 21 (LTS) |
| Web framework | Spring Boot 3.5 (Spring MVC, DI) |
| Views | Thymeleaf 3.1 + Bootstrap 5.3 (responsive) |
| Security | Spring Security 6 (form login, BCrypt, roles) |
| Persistence | Spring Data JPA / Hibernate 6 (DAO pattern) |
| Database | MySQL 8 |
| Build | Maven 3.9 |
| Logging | SLF4J + Logback, request `LoggingInterceptor` |
| Tests | JUnit 5 + Mockito |
| Container | Docker (multi-stage build, Java 21 JRE image) |
| Cloud | Azure Container Apps + Azure Database for MySQL Flexible Server 8.4 |

## Architecture (N-Layer)
```
controller/  -> presentation: Spring MVC controllers + Thymeleaf views (templates/)
service/     -> business rules: AppointmentService, TimeSlotService, VisitTypeService, UserService
repository/  -> data access (DAO): Spring Data JPA interfaces
model/       -> JPA entities: User, VisitType, TimeSlot, Appointment
config/      -> SecurityConfig, WebConfig + LoggingInterceptor, ClockConfig, DataInitializer
dto/         -> form objects with Bean Validation
exception/   -> business exceptions, handled by controller/GlobalExceptionHandler
database/    -> appointment_scheduler.sql (DDL + seed data)
```

## Run it locally
**Prerequisites:** JDK 21, MySQL 8, and an IDE (Spring Tool Suite or IntelliJ). Maven 3.9 is only needed if you run from a terminal.

1. **Clone**
   ```
   git clone https://github.com/ibearat82/appointment-scheduler.git
   cd appointment-scheduler
   ```
2. **Create the database.** In MySQL Workbench, open `database/appointment_scheduler.sql` and run the whole script. From a terminal, you can run `mysql -u root -p < database/appointment_scheduler.sql` instead.
   The script creates the `appointment_scheduler` database, its 4 tables, 4 visit types, 3 weeks of weekday time slots, and two test accounts.
3. **Set the database login.** The defaults in `src/main/resources/application.properties` are user `root` and password `root`. If your MySQL password is different, set environment variables rather than editing the file:
   - `DB_USERNAME`, `DB_PASSWORD`, and optionally `DB_URL`

   In STS, set them under Run → Run Configurations → Environment. In IntelliJ, use Run → Edit Configurations → Environment variables.
4. **Run.** Start `AppointmentSchedulerApplication` from the IDE, or run `mvn spring-boot:run`.
5. Open **http://localhost:8080**

### Test accounts (test data only)
| Role | Email | Password |
|---|---|---|
| Office staff (admin) | admin@clinic.test | Admin123! |
| Patient | patient@clinic.test | Patient123! |

### Run the unit tests
```
mvn test
```

## Configuration
| Setting | Env variable | Default |
|---|---|---|
| JDBC URL | `DB_URL` | `jdbc:mysql://localhost:3306/appointment_scheduler?serverTimezone=America/Phoenix` |
| DB user | `DB_USERNAME` | `root` |
| DB password | `DB_PASSWORD` | `root` |
| HTTP port | `PORT` | `8080` |
| Default admin | `ADMIN_EMAIL` / `ADMIN_PASSWORD` | `admin@clinic.test` / `Admin123!` |

On Azure these same variables are set on the Container App (`DB_PASSWORD` references a Container Apps secret), so no real passwords are stored in Git or in the Docker image.

## Deploy to Azure (Milestone 3)
The app runs as a Docker container on **Azure Container Apps** and uses **Azure Database for MySQL Flexible Server**.

> Why Container Apps instead of App Service? Our Azure for Students subscription has an App Service quota of 0 in every region (B1 and F1), and the quota request was denied. Container Apps uses a separate quota and runs the same Spring Boot JAR inside a container.

### 1. Database: Azure Database for MySQL Flexible Server
1. Azure portal → **Azure Database for MySQL flexible servers → Create** (Advanced create).
2. Resource group `rg-appointment-scheduler`, server `appt-scheduler-db-a2`, region **West US 2**, MySQL **8.4**, workload **Dev/Test**, compute **Burstable B1ms**, 20 GB (free student offer), MySQL authentication.
3. Networking: public access, check **Allow public access from any Azure service**, and **Add current client IP address**.
4. In MySQL Workbench, connect to `appt-scheduler-db-a2.mysql.database.azure.com:3306` with SSL set to **Require**, then run `database/appointment_scheduler.sql`.

### 2. Container image: build and push to Docker Hub
From the project folder (Docker Desktop running):
```
docker build --platform linux/amd64 -t ibearat82/appointment-scheduler:0.3 .
docker login
docker push ibearat82/appointment-scheduler:0.3
```
Optional local test against the Azure database:
```
docker run --rm -p 8080:8080 -e DB_URL="<jdbc url>" -e DB_USERNAME="<user>" -e DB_PASSWORD="<password>" ibearat82/appointment-scheduler:0.3
```

### 3. App: Azure Container App
1. Azure portal → **Container Apps → Create**: resource group `rg-appointment-scheduler`, name `appt-scheduler-a2`, region **West US 2**, new Consumption environment `cae-appointment-scheduler`.
2. Container: image source **Docker Hub**, public, `docker.io`, image `ibearat82/appointment-scheduler:0.3`, 0.5 CPU / 1 Gi.
3. Ingress: enabled, accept traffic from anywhere, HTTP, target port **8080**.
4. **Security → Secrets**: add `db-password`.
5. **Containers → Environment variables**:

| Name | Value |
|---|---|
| `DB_URL` | `jdbc:mysql://appt-scheduler-db-a2.mysql.database.azure.com:3306/appointment_scheduler?serverTimezone=America/Phoenix&sslMode=REQUIRED` |
| `DB_USERNAME` | MySQL admin user |
| `DB_PASSWORD` | Reference a secret → `db-password` |

6. **Scale**: min 0, max **1** replica (logins are stored in server memory).
7. Open the **Application Url** from the Overview page.

### Releasing a new version
Change the code, build and push a new tag (for example `:0.4`), then in **Containers** update the image tag and **Save as a new revision**.
