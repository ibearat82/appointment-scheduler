# Doctor's Office Appointment Scheduling App

CST-323 Cloud Computing, CLC Project, **Group A2**: Imam Bearat and Eliezer Centeno Paz

This is a Spring Boot web app for a doctor's office. Patients register, see open times, book a visit, and can reschedule or cancel it. Office staff manage visit types, open time slots, and see every appointment.

**Milestone 2:** iteration 1, running locally on a laptop.

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

In Milestone 3 these same variables will be set as Azure App Service application settings, so no real passwords are stored in Git.
