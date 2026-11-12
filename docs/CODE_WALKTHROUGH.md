# Code Walkthrough — Uber Ride-Booking Backend

This document explains the code from top to bottom so that someone who has never opened the project can understand
**what every part does, why it is there, and how the parts work together**. It assumes basic Java knowledge; Spring
concepts are explained as they come up.

Read the [README](../README.md) first for the big picture, then use this file as a guided tour.

---

## Contents

1. [How a request travels through the app](#1-how-a-request-travels-through-the-app)
2. [Spring Boot basics used here](#2-spring-boot-basics-used-here)
3. [Entry point and configuration files](#3-entry-point-and-configuration-files)
4. [Entities — the database tables](#4-entities--the-database-tables)
5. [Geospatial data: Points, SRID 4326 and PostGIS](#5-geospatial-data-points-srid-4326-and-postgis)
6. [DTOs and ModelMapper](#6-dtos-and-modelmapper)
7. [Repositories — talking to the database](#7-repositories--talking-to-the-database)
8. [Security: sign-up, login and JWT](#8-security-sign-up-login-and-jwt)
9. [Controllers — the HTTP endpoints](#9-controllers--the-http-endpoints)
10. [Services — the business logic](#10-services--the-business-logic)
11. [Strategies — matching, fares and payments](#11-strategies--matching-fares-and-payments)
12. [Wallet and payment flow in detail](#12-wallet-and-payment-flow-in-detail)
13. [Response wrapping and error handling](#13-response-wrapping-and-error-handling)
14. [Email service](#14-email-service)
15. [Tests and build](#15-tests-and-build)
16. [End-to-end trace: one ride, line by line](#16-end-to-end-trace-one-ride-line-by-line)
17. [Glossary](#17-glossary)

---

## 1. How a request travels through the app

Every HTTP call follows the same path:

```
HTTP request
   │
   ▼
JWTAuthFilter            ← reads "Authorization: Bearer <token>", loads the User, marks them as logged in
   │
   ▼
Spring Security          ← checks the URL rule (/auth/** is public, everything else needs login)
   │
   ▼
Controller method        ← @Secured("ROLE_RIDER") etc. checks the role; JSON body → DTO
   │
   ▼
Service (interface → Impl) ← business rules: validations, state changes, strategy selection
   │
   ├──► Strategy classes  ← matching / fare / payment variations
   ├──► DistanceService   ← calls the external OSRM API
   ▼
Repository               ← Spring Data JPA turns method calls into SQL (some native PostGIS SQL)
   │
   ▼
PostgreSQL + PostGIS
   │
   ▼ (on the way back)
GlobalResponseHandler    ← wraps the return value as { timestamp, data, error }
GlobalExceptionHandler   ← if anything threw, builds { timestamp, data: null, error } with a status code
```

---

## 2. Spring Boot basics used here

If you are new to Spring, these annotations appear everywhere:

| Annotation | Meaning |
|---|---|
| `@SpringBootApplication` | Marks the main class; turns on auto-configuration and component scanning of this package and below |
| `@RestController` | A class whose methods handle HTTP requests and return JSON |
| `@RequestMapping("/riders")` | URL prefix for all methods in the controller |
| `@PostMapping`, `@GetMapping` | Map one method to an HTTP verb + path |
| `@PathVariable`, `@RequestBody`, `@RequestParam` | Take a value from the URL path, the JSON body, or the query string |
| `@Service`, `@Component`, `@Repository`, `@Configuration` | Tell Spring to create one instance (a *bean*) of this class and manage it |
| `@RequiredArgsConstructor` (Lombok) | Generates a constructor for all `final` fields — Spring uses it to **inject** dependencies |
| `@Bean` | A method inside a `@Configuration` class whose return value becomes a bean |
| `@Transactional` | Everything inside the method runs in **one database transaction**: all changes commit together or all roll back |
| `@Entity` | A class mapped to a database table |
| `@Secured("ROLE_X")` | Only users with that role may call the method/class |

**Dependency injection** in one sentence: instead of writing `new RideServiceImpl(...)`, a class declares
`private final RideService rideService;` and Spring hands it the right object when it builds the class.

---

## 3. Entry point and configuration files

### `UberAppApplication.java`

```java
@SpringBootApplication
public class UberAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(UberAppApplication.class, args);
    }
}
```

Starts the embedded Tomcat server on port 8080 and creates all beans.

### `pom.xml` — dependencies and what each is for

| Dependency | Used for |
|---|---|
| `spring-boot-starter-web` | REST controllers, JSON (Jackson), embedded Tomcat, `RestClient` |
| `spring-boot-starter-data-jpa` + `postgresql` | ORM (Hibernate) and the PostgreSQL driver |
| `hibernate-spatial` + `jts-core` | Storing and querying geometry (`Point`) columns |
| `spring-boot-starter-security` + `jjwt-*` | Authentication, role checks, creating and verifying JWTs |
| `modelmapper` | Copying fields between entities and DTOs |
| `spring-boot-starter-mail` | Sending email through SMTP |
| `springdoc-openapi-starter-webmvc-ui` | Generated API docs / Swagger UI |
| `spring-boot-starter-actuator` | Health and metrics endpoints (`/actuator/health`) |
| `lombok` | Generates getters, setters, constructors, builders at compile time |
| `testcontainers-postgresql` | Running a real Postgres in Docker during tests |

### `application.properties` (default profile)

- `spring.datasource.*` — where the database is (`localhost:5433`).
- `spring.jpa.hibernate.ddl-auto=create` — Hibernate **drops and recreates** tables from the entity classes at every
  start. Convenient while developing; production apps use migrations (Flyway/Liquibase) instead.
- `jwt.secretKey` — the secret used to sign tokens (read by `JWTService` via `@Value("${jwt.secretKey}")`).
- `spring.mail.*` — Gmail SMTP settings for `EmailSenderServiceImpl`.

### `application-prod.properties` (profile `prod`)

Same keys, but the database host, name, user and password come from environment variables
(`${DB_HOST_URL}`, `${DB_NAME}`, …). It also runs `data.sql` after Hibernate creates the tables:

- `spring.jpa.defer-datasource-initialization=true` — run `data.sql` **after** Hibernate builds the schema.
- `spring.sql.init.mode=always` — run it even for a non-embedded database.

### `data.sql`

Seed data: 10 users, their roles in `user_roles`, one rider, four drivers placed around north Delhi using
`ST_GeomFromText('POINT(77.1100 28.7000)', 4326)` and two wallets. Note the seeded passwords are plain text, so
these users cannot log in through BCrypt — they are for exploring the data and matching queries.

---

## 4. Entities — the database tables

Entities live in `entities/`. Each class is one table; each field is a column.

### `User` (table `app_user`)

```java
@Entity
@Table(name = "app_user", indexes = { @Index(name = "idx_user_email", columnList = "email") })
public class User implements UserDetails {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String name;
    @Column(unique = true) private String email;
    private String password;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles;
    ...
}
```

- The table is called `app_user` because `user` is a reserved word in PostgreSQL.
- `@ElementCollection` stores the set of roles in a separate table `user_roles (user_id, roles)`. `EAGER` means the
  roles are loaded with the user every time — needed because security checks roles on each request.
- `@Enumerated(EnumType.STRING)` stores `"RIDER"` rather than `0`, so reordering the enum never corrupts data.
- **`implements UserDetails`** makes this entity usable directly by Spring Security:
  - `getUsername()` returns the email (we log in with email),
  - `getAuthorities()` turns each role into `ROLE_RIDER`, `ROLE_DRIVER`, … which is the format `@Secured` expects.

### `Rider` and `Driver`

A `User` is the login account; `Rider` and `Driver` are **profiles** attached to it with `@OneToOne`. One person can
be both (the seed data has users with both roles).

```java
public class Driver {
    @OneToOne @JoinColumn(name = "user_id") private User user;
    private Double rating;
    private Boolean available;          // can this driver take a new ride?
    private String vehicleId;           // indexed (idx_driver_vehicle_id)
    @Column(columnDefinition = "Geometry(Point,4326)")
    Point currentLocation;              // where the driver is now
}
```

### `RideRequest`

What the rider asks for *before* a driver accepts: pickup and drop `Point`s, `paymentMethod`, the calculated
`fare`, `requestedTime` (filled automatically by `@CreationTimestamp`) and a status (`PENDING` → `CONFIRMED`).

### `Ride`

Created when a driver accepts. It copies the request's fields and adds `driver`, `otp`, `rideStatus`,
`createdTime`, `startedAt`, `endedAt`. `rider` and `driver` are `@ManyToOne(fetch = LAZY)` — they are loaded from
the database only when the code actually touches them, which keeps ride queries cheap.

### `Payment`

One per ride (`@OneToOne`). Created as `PENDING` when the ride starts; set to `CONFIRMED` once the payment
strategy runs at ride end.

### `Wallet` and `WalletTransaction`

Every user gets a `Wallet` with `balance` (default `0.0`). Each change to the balance writes a `WalletTransaction`
row: `amount`, `transactionType` (`CREDIT`/`DEBIT`), `transactionMethod` (`RIDE` or `BANKING`), the `ride` that
caused it and a timestamp — a simple ledger, so the balance can always be explained.

### Enums (`entities/enums/`)

| Enum | Values |
|---|---|
| `Role` | `ADMIN`, `DRIVER`, `RIDER` |
| `RideRequestStatus` | `PENDING`, `CANCELLED`, `CONFIRMED` |
| `RideStatus` | `CANCELLED`, `CONFIRMED`, `ENDED`, `ONGOING` |
| `PaymentMethod` | `CASH`, `WALLET` |
| `PaymentStatus` | `PENDING`, `CONFIRMED`, `REFUNDED` |
| `TransactionType` | `CREDIT`, `DEBIT` |
| `TransactionMethod` | `BANKING`, `RIDE` |

> `OnboardDriverDto` sits in the `entities` package but is a request DTO (`{ vehicleId }`), not a table.

---

## 5. Geospatial data: Points, SRID 4326 and PostGIS

Locations are stored as real geometry, not as two separate `lat`/`lng` numbers. This lets the database answer
"which drivers are near this point?" with an index-friendly spatial query.

- **JTS `Point`** (`org.locationtech.jts.geom.Point`) is the Java type for a point; `getX()` is **longitude**,
  `getY()` is **latitude**.
- **SRID 4326** is the identifier for WGS-84, the latitude/longitude system GPS uses.
- `@Column(columnDefinition = "Geometry(Point,4326)")` tells Hibernate to create a PostGIS geometry column.
- **`GeometryUtil.createPoint(PointDto)`** builds a `Point` from the DTO's `coordinates` array:

```java
GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
Coordinate coordinate = new Coordinate(pointDto.getCoordinates()[0],   // x = longitude
                                       pointDto.getCoordinates()[1]);  // y = latitude
return geometryFactory.createPoint(coordinate);
```

PostGIS functions used in `DriverRepository`:

| Function | Meaning |
|---|---|
| `ST_Distance(a, b)` | Distance between two geometries |
| `ST_DWithin(a, b, r)` | True if `a` and `b` are within distance `r` — can use a spatial index |

*Detail worth knowing:* on `geometry` columns in SRID 4326 these functions measure in **degrees**. To get metres,
cast to `geography` (`a::geography`). See "Known limitations" in the README.

---

## 6. DTOs and ModelMapper

**DTO (Data Transfer Object)** = the shape of JSON going in or out. Entities stay internal; DTOs are what clients
see. Examples: `SignupDto {name, email, password}`, `RideRequestDto`, `RideDto`, `DriverDto {user, rating}`.

`PointDto` is GeoJSON-shaped so front-ends and map libraries can use it directly:

```java
public class PointDto {
    private double[] coordinates;      // [longitude, latitude]
    private String type = "Point";
}
```

### `MapperConfig`

ModelMapper copies fields with the same names between objects. It does not know how to turn a `PointDto` into a
JTS `Point`, so two converters are registered:

```java
mapper.typeMap(PointDto.class, Point.class).setConverter(ctx -> GeometryUtil.createPoint(ctx.getSource()));
mapper.typeMap(Point.class, PointDto.class).setConverter(ctx -> {
    Point p = ctx.getSource();
    return new PointDto(new double[]{ p.getX(), p.getY() });
});
```

After this, `modelMapper.map(rideRequestDto, RideRequest.class)` correctly converts the nested locations too.

---

## 7. Repositories — talking to the database

Repositories are interfaces extending `JpaRepository<Entity, IdType>`. Spring generates the implementation, giving
`save`, `findById`, `findAll`, `delete` for free. Methods named like `findByEmail` are turned into SQL automatically
(**derived queries**).

| Repository | Custom methods |
|---|---|
| `UserRepository` | `findByEmail(email)` — used by login |
| `RiderRepository` | `findByUser(user)` — the rider profile of the logged-in user |
| `DriverRepository` | `findByUser(user)`, plus two PostGIS native queries below |
| `RideRepository` | `findByRider(rider, pageable)`, `findByDriver(driver, pageable)` — paginated history |
| `PaymentRepository` | `findByRide(ride)` |
| `WalletRepository` | `findByUser(user)` |
| `RideRequestRepository`, `WalletTransactionRepository` | only the inherited CRUD methods |

### The two matching queries

```sql
-- findTenNearestMatchingDrivers(pickupLocation)
SELECT d.*, ST_Distance(d.current_location, :pickupLocation) AS distance
FROM driver d
WHERE available = true AND ST_DWithin(d.current_location, :pickupLocation, 10000)
ORDER BY distance
LIMIT 10;

-- findTenNearbyTopRatedDrivers(pickupLocation)
SELECT d.*
FROM driver d
WHERE d.available = true AND ST_DWithin(d.current_location, :pickupLocation, 15000)
ORDER BY d.rating DESC
LIMIT 10;
```

`nativeQuery = true` means this is raw PostgreSQL rather than JPQL — required because JPQL does not know PostGIS
functions. `:pickupLocation` is bound from the method parameter.

### Pagination

Controllers build a `PageRequest`:

```java
PageRequest.of(pageOffset, pageSize, Sort.by(Sort.Direction.DESC, "createdTime", "id"));
```

Spring adds `ORDER BY created_time DESC, id DESC LIMIT … OFFSET …` and returns a `Page<Ride>` which includes the
items plus `totalElements`, `totalPages`, etc.

---

## 8. Security: sign-up, login and JWT

### The pieces

| Class | Job |
|---|---|
| `SecurityConfig` | Defines the `PasswordEncoder` (BCrypt) and exposes Spring's `AuthenticationManager` as a bean |
| `UserService` | Implements `UserDetailsService.loadUserByUsername(email)` so Spring can find a user by email; also `getUserById` |
| `WebSecurityConfig` | The security filter chain: stateless sessions, CSRF off, `/auth/**` public, everything else authenticated, `JWTAuthFilter` added before Spring's login filter. `@EnableMethodSecurity(securedEnabled = true)` turns on `@Secured` |
| `JWTService` | Creates and parses tokens |
| `JWTAuthFilter` | Runs once per request and logs the user in from the token |
| `AuthServiceImpl` | Sign-up, login, onboarding drivers, refreshing tokens |

### Sign-up (`AuthServiceImpl.signup`) — `@Transactional`

1. If a user with that email exists → throw `RuntimeConflictException` (→ 409).
2. Map `SignupDto` → `User`, set roles to `{RIDER}`, **hash the password** with BCrypt.
3. Save the user, then create a `Rider` profile (rating 0.0) and an empty `Wallet`.
4. Return a `UserDto`.

Because the method is transactional, if creating the wallet failed, the user and rider would be rolled back too —
no half-created accounts.

### Login (`AuthServiceImpl.login`)

```java
Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(email, password));
User user = (User) authentication.getPrincipal();
String accessToken  = jwtService.generateAccessToken(user);
String refreshToken = jwtService.generateRefreshToken(user);
```

`authenticationManager.authenticate` calls `UserService.loadUserByUsername`, compares the BCrypt hash, and throws
`BadCredentialsException` (→ 401) if wrong. The controller returns the access token in the body and puts the
refresh token in an **HTTP-only cookie** (JavaScript cannot read it, which protects it from XSS).

### Tokens (`JWTService`)

```java
Jwts.builder()
    .subject(user.getId().toString())         // who the token belongs to
    .claim("email", user.getEmail())
    .claim("roles", user.getRoles().toString())
    .issuedAt(new Date())
    .expiration(new Date(System.currentTimeMillis() + 1000*60*10))   // 10 minutes
    .signWith(getSecretKey())                 // HMAC-SHA with jwt.secretKey
    .compact();
```

- **Access token**: 10 minutes, carries id, email and roles.
- **Refresh token**: about 6 months (`1000L*60*60*24*30*6` ms), carries only the id.
- `getUserIdFromToken` verifies the signature and expiry (throwing `JwtException` if invalid) and returns the id.

A JWT is three Base64 parts — header, payload, signature. Anyone can *read* the payload, but nobody can *change* it
without the secret key, because the signature would no longer match.

### Per-request authentication (`JWTAuthFilter`)

```java
String header = request.getHeader("Authorization");
if (header == null || !header.startsWith("Bearer")) { filterChain.doFilter(request, response); return; }
String token = header.split("Bearer ")[1];
Long userId = jwtService.getUserIdFromToken(token);
if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
    User user = userService.getUserById(userId);
    var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(auth);
}
filterChain.doFilter(request, response);
```

- No header → let the request continue unauthenticated (public routes still work; protected ones get rejected).
- Valid token → load the `User` and put it into the **`SecurityContextHolder`**. From now on, any code can ask
  "who is calling?" — this is exactly what `getCurrentRider()` / `getCurrentDriver()` do.
- Exceptions inside a filter happen *before* controllers, so `@RestControllerAdvice` would not normally see them.
  The filter forwards them to Spring's `HandlerExceptionResolver`, so a bad token still produces the standard JSON
  error (401) from `GlobalExceptionHandler`.

### Refresh (`/auth/refresh`)

Reads the refresh token from the request cookies, extracts the user id, loads the user and issues a new access
token — so the client can stay logged in without re-entering the password every 10 minutes.

### Onboarding a driver (`AuthServiceImpl.onboardNewDriver`) — admin only

1. Load the user by id (404 if missing). If they are already a driver → 409.
2. Build a `Driver` (rating 0, `available = true`, given `vehicleId`).
3. Add `DRIVER` to the user's roles and save, then save the driver.

---

## 9. Controllers — the HTTP endpoints

Controllers are intentionally thin: read input, call one service method, return the result.

### `AuthController` — `/auth`

`signup` (201), `login`, `refresh`, and `onboardNewDriver/{userId}` (method-level `@Secured("ROLE_ADMIN")`).

### `RiderController` — `/riders`, class-level `@Secured("ROLE_RIDER")`

`requestRide`, `cancelRide/{rideId}`, `rateDriver`, `getMyProfile`, `getMyRides?pageOffset&pageSize`.

### `DriverController` — `/drivers`, class-level `@Secured("ROLE_DRIVER")`

`acceptRide/{rideRequestId}`, `startRide` (body `RideStartDto {rideId, otp}`), `endRide/{rideId}`,
`cancelRide/{rideId}`, `rateDriver` (rates the *rider*), `getMyProfile`, `getMyRides`.

Putting `@Secured` on the class applies it to every method — a user without `ROLE_DRIVER` gets 403 on all
driver endpoints.

---

## 10. Services — the business logic

Every service is an interface in `services/` with an implementation in `services/implementations/`.

### `RiderServiceImpl`

**`requestRide(dto)`** — `@Transactional`

```java
Rider rider = getCurrentRider();
RideRequest rideRequest = modelMapper.map(rideRequestDto, RideRequest.class);
rideRequest.setRideRequestStatus(RideRequestStatus.PENDING);
rideRequest.setRider(rider);

Double fare = rideStrategyManager.rideFareCalculationStrategy().calculateFare(rideRequest);
rideRequest.setFare(fare);
RideRequest saved = rideRequestRepository.save(rideRequest);

List<Driver> drivers = rideStrategyManager
        .driverMatchingStrategy(getCurrentRider().getRating())
        .findMatchingDriver(rideRequest);
// TODO: notify these drivers
return modelMapper.map(saved, RideRequestDto.class);
```

1. Find who is calling (from the security context).
2. Convert the DTO (with its `PointDto`s) into an entity.
3. Ask the strategy manager for the right **fare** strategy (surge or default) and compute the fare.
4. Save the request as `PENDING`.
5. Ask for the right **matching** strategy based on the rider's rating and get candidate drivers.

**`cancelRide(rideId)`** — the ride must belong to this rider and be `CONFIRMED`; it becomes `CANCELLED` and the
driver is set back to available.

**`getAllMyRides(pageRequest)`** — paginated rides of the current rider, each mapped to `RideDto`.

**`createRider(user)`** — used during sign-up.

**`getCurrentRider()`** — reads the `User` from `SecurityContextHolder` and finds their `Rider` profile.

### `DriverServiceImpl`

**`acceptRide(rideRequestId)`** — `@Transactional`

1. Request must be `PENDING`; driver must be `available`.
2. Mark the driver unavailable.
3. `rideService.createNewRide(request, driver)` → request becomes `CONFIRMED`, a `Ride` is created with an OTP.

**`startRide(rideId, otp)`**

1. The ride must belong to this driver and be `CONFIRMED`.
2. The OTP must match — this proves the driver has actually met the rider.
3. Set `startedAt`, status `ONGOING`, and create a `PENDING` `Payment`.

**`endRide(rideId)`** — `@Transactional`

1. The ride must belong to this driver and be `ONGOING`.
2. Set `endedAt`, status `ENDED`, driver available again.
3. `paymentService.processPayment(ride)` — runs the payment strategy (see section 12).

Because `endRide` is transactional, if the payment fails the ride is **not** marked as ended either.

**`cancelRide(rideId)`** — like the rider version: only a `CONFIRMED` ride of this driver.

**`getCurrentDriver()`**, **`updateDriverAvailability`**, **`createNewDriver`**, **`getAllMyRides`** — helpers.

### `RideServiceImpl`

- **`createNewRide(request, driver)`** — `@Transactional`: sets the request to `CONFIRMED`, maps it into a new
  `Ride` (resetting `id` to `null` so a new row is inserted rather than overwriting), sets status `CONFIRMED`,
  assigns the driver and a random OTP, saves both.
- **`generateRandomOtp()`** — `String.format("%04d", random.nextInt(10000))` → always 4 digits, e.g. `"0042"`.
- `getRideById`, `updateRideStatus`, `getAllRidesOfRider`, `getAllRidesOfDriver`.

### `RideRequestServiceImpl`

`findRideRequestById` (404 if missing) and `update` (checks it exists, then saves).

### `DistanceServiceOSRMImpl`

Calls the free OSRM routing server to get the **driving** distance (not the straight-line distance):

```
GET https://router.project-osrm.org/route/v1/driving/{srcLng},{srcLat};{destLng},{destLat}
```

The JSON response has `routes[0].distance` in metres; the method returns kilometres (`/ 1000.0`). Two small
package-private classes, `OSRMResponseDto` and `OSRMRoute`, describe just the part of the JSON that is needed.
Any failure is rethrown as a `RuntimeException`.

### `PaymentServiceImpl`

- `createNewPayment(ride)` — `Payment` with the ride's method and fare, status `PENDING`.
- `processPayment(ride)` — finds the payment for this ride and hands it to
  `paymentStrategyManager.paymentStrategy(payment.getPaymentMethod()).processPayment(payment)`.
- `updatePaymentStatus`.

### `WalletServiceImpl` and `WalletTransactionServiceImpl`

- `createNewWallet(user)` — balance 0.
- `addMoneyToWallet(...)` / `deductMoneyFromWallet(...)` — `@Transactional`; change the balance and record a
  `WalletTransaction` (`CREDIT` or `DEBIT`) linked to the ride.
- `findByUser`, `findWalletById`. `withdrawAllMyMoneyFromWallet` is a placeholder.

---

## 11. Strategies — matching, fares and payments

### Why strategies?

Without them, `requestRide` would contain `if (surge) {...} else {...}` and `if (rating >= 4.8) {...}`, and
`processPayment` would have `if (WALLET) {...} else if (CASH) {...}`. Every new rule would mean editing those
methods. With strategies, each variation is its own small class, and a manager picks one.

### Interfaces

```java
public interface DriverMatchingStrategy {
    List<Driver> findMatchingDriver(RideRequest rideRequest);
}

public interface RideFareCalculationStrategy {
    double RIDE_FARE_MULTIPLIER = 10;          // ₹10 per km
    double calculateFare(RideRequest rideRequest);
}

public interface PaymentStrategy {
    Double PLATFORM_COMMISSION = 0.3;          // platform keeps 30 %
    void processPayment(Payment payment);
}
```

### `RideStrategyManager`

```java
public DriverMatchingStrategy driverMatchingStrategy(double riderRating) {
    return riderRating >= 4.8 ? highestRatedDriverStrategy : nearestDriverStrategy;
}

public RideFareCalculationStrategy rideFareCalculationStrategy() {
    LocalTime now = LocalTime.now();
    boolean isSurgeTime = now.isAfter(LocalTime.of(18, 0)) && now.isBefore(LocalTime.of(21, 0));
    return isSurgeTime ? surgePricingFareCalculationStrategy : defaultFareCalculationStrategy;
}
```

- **Rewarding good riders**: a rider rated 4.8+ is matched with the best-rated drivers nearby.
- **Surge pricing**: evening peak (6–9 pm) doubles the fare.

### Implementations

| Class | What it does |
|---|---|
| `DriverMatchingNearestDriverStrategy` | `driverRepository.findTenNearestMatchingDrivers(pickup)` |
| `DriverMatchingHighestRatedDriverStrategy` | `driverRepository.findTenNearbyTopRatedDrivers(pickup)` |
| `RideFareDefaultFareCalculationStrategy` | `distanceKm × 10` |
| `RideFareSurgePricingFareCalculationStrategy` | `distanceKm × 10 × 2` (`SURGE_FACTOR = 2`) |
| `WalletPaymentStrategy` | Moves money rider → driver through wallets (below) |
| `CODPaymentStrategy` | Cash ride: takes the commission from the driver's wallet (below) |

### `PaymentStrategyManager`

```java
return switch (paymentMethod) {
    case WALLET -> walletPaymentStrategy;
    case CASH   -> codPaymentStrategy;
};
```

A `switch` expression over an enum — the compiler insists every enum value is handled.

---

## 12. Wallet and payment flow in detail

Assume a fare of **₹200**.

### Wallet payment (`WalletPaymentStrategy`) — `@Transactional`

| Step | Effect |
|---|---|
| Debit rider's wallet by the full fare | rider −₹200 (DEBIT, RIDE) |
| Credit driver's wallet by fare − 30 % | driver +₹140 (CREDIT, RIDE) |
| Mark payment `CONFIRMED` | platform has kept ₹60 |

### Cash payment (`CODPaymentStrategy`)

The rider paid the driver ₹200 in cash, so the driver is holding the platform's share.

| Step | Effect |
|---|---|
| Debit driver's wallet by 30 % | driver −₹60 (DEBIT, RIDE) |
| Mark payment `CONFIRMED` | platform has its ₹60 |

The driver's wallet can therefore go negative after cash rides; that negative balance is what the driver owes the
platform.

---

## 13. Response wrapping and error handling

### `ApiResponse<T>` and `ApiError`

```java
public class ApiResponse<T> {
    @JsonFormat(pattern = "hh:mm:ss dd-MM-yy") private LocalDateTime timestamp;
    private T data;
    private ApiError error;
}
public class ApiError { private HttpStatus httpStatus; private String errorMessage; }
```

### `GlobalResponseHandler` (`ResponseBodyAdvice`)

Runs after every controller method and before JSON is written. If the body is not already an `ApiResponse` it is
wrapped as `new ApiResponse<>(body)`. Paths used by tools (`/v3/api-docs`, `/swagger-ui`, `/swagger-resources`,
`/actuator`) are left untouched so those tools still understand their responses.

### `GlobalExceptionHandler` (`@RestControllerAdvice`)

Each `@ExceptionHandler` converts one exception type into an `ApiResponse` with an `ApiError` and status:

| Exception | Status |
|---|---|
| `ResourceNotFoundException` | 404 |
| `RuntimeConflictException` | 409 |
| `AuthenticationException` | 401 |
| `JwtException` | 401 |
| `AccessDeniedException` | 403 |
| `Exception` (anything else) | 409 |

The two custom exceptions in `exceptions/` simply extend `RuntimeException`, so services can throw them without
`throws` clauses.

---

## 14. Email service

`EmailSenderServiceImpl` uses Spring's `JavaMailSender` with a `SimpleMailMessage` (to, subject, text). There are
two overloads: one recipient or an array of recipients. Failures are logged rather than thrown, so a mail outage
never breaks a ride. SMTP settings come from `spring.mail.*` (Gmail with STARTTLS on port 587).

---

## 15. Tests and build

- `UberAppApplicationTests` starts the full application context (`@SpringBootTest`) and sends a test email to one
  and to several recipients.
- `TestContainerConfiguration` is an empty `@TestConfiguration`, ready for a `@ServiceConnection`
  `PostgreSQLContainer` so tests can use a throw-away Postgres in Docker.
- `buildspec.yml` is the AWS CodeBuild recipe (Corretto 21 → `mvn package -DskipTests` → `target/*.jar`, caching
  `~/.m2`).

---

## 16. End-to-end trace: one ride, line by line

1. **`POST /auth/signup`** → `AuthController.signUp` → `AuthServiceImpl.signup` → `User` (RIDER) + `Rider` +
   `Wallet` saved in one transaction.
2. **`POST /auth/login`** → `AuthenticationManager` checks the BCrypt hash → `JWTService` issues tokens.
3. **`POST /riders/requestRide`** with `Authorization: Bearer …`
   - `JWTAuthFilter` puts the `User` in the security context.
   - `@Secured("ROLE_RIDER")` passes.
   - `RiderServiceImpl.requestRide` → `RideStrategyManager` picks the fare strategy → `DistanceServiceOSRMImpl`
     asks OSRM for the distance → fare saved on a `PENDING` `RideRequest` → matching strategy runs the PostGIS
     query.
   - `GlobalResponseHandler` wraps the `RideRequestDto`.
4. **`POST /drivers/acceptRide/{id}`** → `DriverServiceImpl.acceptRide` → driver unavailable →
   `RideServiceImpl.createNewRide` → `Ride` `CONFIRMED` with OTP.
5. **`POST /drivers/startRide`** → OTP checked → `ONGOING` → `PaymentServiceImpl.createNewPayment` (`PENDING`).
6. **`POST /drivers/endRide/{id}`** → `ENDED`, driver available → `PaymentServiceImpl.processPayment` →
   `PaymentStrategyManager` → `WalletPaymentStrategy` or `CODPaymentStrategy` → wallets and transactions updated →
   payment `CONFIRMED`.
7. **`GET /riders/getMyRides`** → paginated `RideDto`s, newest first.

---

## 17. Glossary

| Term | Meaning |
|---|---|
| **Bean** | An object created and managed by Spring |
| **DTO** | Data Transfer Object — the JSON shape of a request or response |
| **Entity** | A Java class mapped to a database table |
| **JPA / Hibernate** | The Java standard for ORM / its most popular implementation |
| **JWT** | JSON Web Token — a signed token proving who the caller is |
| **BCrypt** | A slow, salted password-hashing algorithm |
| **SecurityContextHolder** | Where Spring Security stores the logged-in user for the current request |
| **PostGIS** | PostgreSQL extension adding geometry types and spatial functions |
| **SRID 4326** | The WGS-84 latitude/longitude coordinate system |
| **OSRM** | Open Source Routing Machine — computes road routes and distances |
| **Strategy pattern** | Put each variant of an algorithm in its own class behind a shared interface and pick one at runtime |
| **`@Transactional`** | Run a method's database work as one all-or-nothing unit |
| **Surge pricing** | Higher fares at busy times (here 6–9 pm, ×2) |
| **Platform commission** | The share of each fare the platform keeps (here 30 %) |
