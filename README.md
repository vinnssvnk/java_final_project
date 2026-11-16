# EV Charging Scheduler

## Purpose

A console application for a network of electric-vehicle charging stations. A driver can estimate
how long and how much a charging session will cost, book a charger for a time slot, let the system
find the best charger, cancel or complete a reservation, and look at reports.

The project is the final assignment of an object-oriented programming course. The focus is a clean,
explainable object-oriented design with tested business rules, not a large feature set.

## How to run

Requirements: JDK 17 or newer. Maven does not need to be installed: the bundled Maven Wrapper
downloads the right version on first use.

```bash
./mvnw compile exec:java        # Linux / macOS / Git Bash
.\mvnw.cmd compile exec:java    # Windows PowerShell or cmd
```

With Maven installed you can use `mvn` instead of the wrapper.

The application loads the sample data from `src/main/resources/data/`, prints a summary and shows the menu.
Dates are typed as `yyyy-MM-dd HH:mm`, for example `2026-12-14 08:10`.

> The sample reservations are on 14–15 December 2026. "Now" is the real system clock, so if you run the
> program after those dates, edit the dates in `reservations.csv` to keep the conflict demo working.

Demo ideas:

| Try | Input | What happens |
|---|---|---|
| Conflict + suggestion | 4 → `C5`, `V3`, 20, 80, `2026-12-14 08:10` | Overlaps R001; the next free slot (08:39, after the adjacent R001/R002) is offered |
| Incompatible vehicle | 3 → `C5`, `V4` | The Renault Zoe has no CCS2/DC support → friendly error |
| Best charger | 5 → `V1`, 20, 80, `2026-12-14 08:00`, strategy 1 or 2 | Fastest finish picks the 300 kW charger, cheapest picks the time-of-use station |
| Peak pricing | 3 → `C5`, `V1`, 20, 80, at `10:00` and at `18:00` | Same energy, different price |

## How to run the tests

```bash
./mvnw test        # Windows: .\mvnw.cmd test   (or: clean test)
```

87 JUnit 5 tests in 14 test classes. They use a fixed `Clock`, in-memory repositories and no mocking framework.

## Main features

1. List stations & chargers (with each station's tariff)
2. List vehicles
3. Estimate a session: duration, energy, price
4. Book a charger; on a conflict the next free slot on that charger is offered
5. Find the best charger with a chosen strategy (fastest finish / cheapest)
6. Cancel a reservation (the fee is shown before confirming)
7. Complete a reservation
8. Show the schedule of a charger
9. Reports: utilization per charger for a day, revenue per station, top 3 vehicles by energy

## Main classes

| Class | Responsibility |
|---|---|
| `Charger` (abstract) | Shared charger state, compatibility check, template method `estimateDuration` |
| `AcCharger`, `DcFastCharger` | Their charging curve (`powerAt`) |
| `Vehicle` (record) | Battery size, AC/DC power limits, connectors; energy needed for a charge range |
| `Station` (record) | Name, city and the station's `PricingPolicy` |
| `TimeSlot` (record) | Half-open interval `[start, end)`: overlap and overlap length |
| `Reservation` | A booked session with its status life-cycle and cancellation fee |
| `ChargerSchedule` | Active reservations of one charger in a `TreeMap`; conflict check and next free slot |
| `PricingPolicy`, `FlatPricing`, `TimeOfUsePricing` | How a station prices energy |
| `CancellationPolicy` | Whether a reservation may be cancelled and the fee |
| `ChargerSelectionStrategy`, `FastestFinishStrategy`, `CheapestStrategy` | Choose the best `Offer` |
| `ReservationService` | Estimate, book, suggest next slot, find best charger, complete |
| `CancellationService` | Preview fee and cancel |
| `ReportService` | Utilization, revenue, top vehicles (streams) |
| `CatalogService` | Read-only listings for the UI |
| `CsvLoader<T>`, mappers, `DataInitializer` | Load CSV files into repositories |
| `InMemoryRepository<T, ID>`, `InMemoryReservationRepository` | Storage implementation |
| `ConsoleMenu`, `InputReader` | Console interaction |
| `Application` | Composition root: creates and wires all objects |

## Architecture

```
          ┌──────────────────────────┐
          │ ui                       │  ConsoleMenu, InputReader
          └────────────┬─────────────┘
                       │ calls
          ┌────────────▼─────────────┐
          │ service                  │  ReservationService, CancellationService,
          └────────────┬─────────────┘  ReportService, CatalogService
                       │ uses
          ┌────────────▼─────────────┐
          │ domain                   │  model, pricing, selection,
          │  (repository interfaces) │  repository (interfaces only)
          └────────────▲─────────────┘
                       │ implements
          ┌────────────┴─────────────┐
          │ infrastructure           │  io (CSV loading), persistence (in-memory)
          └──────────────────────────┘

  Application (composition root) knows all layers and wires them together.
  exception: shared exception types, used by every layer, depends on nothing.
```

Inside the domain, the model is grouped by concept:

```
domain/
├── model/
│   ├── charger/      Charger, AcCharger, DcFastCharger, ChargerSchedule, ChargerType, ConnectorType
│   ├── reservation/  Reservation, ReservationStatus, TimeSlot, Offer
│   ├── station/      Station
│   ├── vehicle/      Vehicle
│   └── Validation    shared constructor guards
├── pricing/          PricingPolicy, FlatPricing, TimeOfUsePricing, CancellationPolicy, Money
├── selection/        ChargerSelectionStrategy, FastestFinishStrategy, CheapestStrategy
└── repository/       Repository, ReservationRepository
```

**Dependency rule:** `ui → service → domain ← infrastructure`. The domain depends on nothing outside
itself (apart from the plain exception classes). Repository *interfaces* live in the domain and the
in-memory *implementations* live in infrastructure, so the domain and services never know how data is stored.
The UI receives domain objects from services to display them, but never touches repositories or infrastructure.
Only `ui` uses `System.out` / `Scanner` (plus `Application` for the start-up summary and fatal error).

## Technical decisions

1. **`TreeMap` per charger schedule.** Reservations are keyed by start time. Because active slots never
   overlap, a new slot can only clash with the reservation at/just before its start (`floorEntry`) or
   the first one after it (`ceilingEntry`). Conflict detection is O(log n) instead of scanning every
   reservation, and "next free slot" walks the gaps in order.
2. **Half-open time slots `[start, end)`.** A session ending at 10:26 and one starting at 10:26 do not
   overlap, so back-to-back bookings work without fiddling with "end minus one minute".
3. **Template method for the charging curve.** `Charger.estimateDuration` is `final` and contains the whole
   algorithm (validate, check compatibility, sum 1 % steps, round up to minutes). Subclasses only answer
   "what power at this percentage?". A new charger type cannot get the algorithm wrong.
4. **`BigDecimal` for money**, scale 2, `HALF_UP`, EUR (constants in `Money`). `double` cannot represent
   0.10 exactly and rounding errors would add up in revenue reports. Time-of-use prices are rounded once,
   at the end.
5. **Injected `java.time.Clock`.** Services ask the clock for "now" instead of calling
   `LocalDateTime.now()`. Tests use a fixed clock, so "59 minutes before start" is exactly reproducible.
6. **Unchecked business exceptions vs. checked load exception.** Rule violations (`SlotConflictException`, ...)
   extend the unchecked `EvChargingException`: callers cannot fix them locally, and the menu catches them in one
   place. `DataLoadException` is checked because the program cannot work without its data, so the compiler
   forces `Application` to decide what to do (print a message and exit with code 1).

## Where inheritance is used

- `AcCharger` and `DcFastCharger` extend the abstract `Charger` (they *are* chargers and reuse its state and algorithm).
- `InMemoryReservationRepository` extends the generic `InMemoryRepository<Reservation, String>` and only adds two queries.
- All business exceptions extend `EvChargingException`, which extends `RuntimeException`.
- `ReservationRepository` extends the generic `Repository<Reservation, String>` interface.

## Where polymorphism is used

- `Charger.powerAt(...)`: the same `estimateDuration` call gives a constant curve for AC and a tapering curve for DC.
- `PricingPolicy.priceFor(...)` / `describe()`: each `Station` holds *some* policy; services never check which one.
- `ChargerSelectionStrategy.select(...)`: `findBestCharger` takes any strategy; the menu builds its choice list
  from a map of strategies passed in by `Application`.
- `Repository<T, ID>`: services work with the interface; the in-memory classes are just one implementation.
- Mappers are all used as `Function<String[], T>` by the one generic `CsvLoader<T>`.

## Abstract class vs. interface

- **`Charger` is an abstract class** because chargers share *state* (id, station, connector, max power, schedule)
  and *behaviour* (compatibility check, the fixed estimation algorithm). An interface cannot hold fields or
  a `final` template method.
- **`PricingPolicy`, `ChargerSelectionStrategy` and `Repository` are interfaces** because they describe only a
  capability with no shared state. Implementations have nothing in common except the method signature,
  and a class could implement several of them.

## Custom exceptions

| Exception | Type | Thrown when |
|---|---|---|
| `EvChargingException` | unchecked base | — (base of all business exceptions) |
| `SlotConflictException` | unchecked | A slot overlaps an active reservation on the same charger |
| `IncompatibleConnectorException` | unchecked | Connector missing, or the vehicle does not support AC/DC |
| `ReservationNotFoundException` | unchecked | Unknown reservation id when cancelling/completing |
| `CancellationNotAllowedException` | unchecked | Reservation not ACTIVE or already started |
| `IllegalStatusTransitionException` | unchecked | Any status change other than ACTIVE → CANCELLED / COMPLETED |
| `DataLoadException` | **checked** | Data file missing or unreadable |

Invalid arguments (unknown vehicle/charger id, bad percentages, start not in the future) use the standard
`IllegalArgumentException`. The console menu catches both kinds, prints the message and keeps running.

## How data is loaded

Files in `src/main/resources/data/` are read from the classpath at start-up, in this order (each file may only
reference ids from files already loaded):

```
stations.csv      id,name,city,tariff,basePrice,peakPrice          tariff = FLAT | TIME_OF_USE (peakPrice empty for FLAT)
chargers.csv      id,stationId,type,connector,maxPowerKw           type = AC | DC; connector = TYPE2 | CCS2 | CHADEMO
vehicles.csv      id,model,batteryKwh,maxAcKw,maxDcKw,connectors   connectors separated by '|', e.g. TYPE2|CCS2
reservations.csv  id,vehicleId,chargerId,start,end,fromPct,targetPct,price,status   ISO-8601 date-times
```

`reservations.csv` contains `fromPct` in addition to `targetPct`: the energy of a session (needed for the
"top vehicles" report) is `batteryKwh * (targetPct - fromPct) / 100`, which cannot be computed from the target alone.

Error handling:

- `CsvLoader<T>.load(reader, mapper)` skips the header and blank lines. If the mapper rejects a line (bad
  number, bad date, unknown enum value, wrong column count, failed domain validation, unknown referenced id),
  the line is skipped and recorded as `file line N: reason`; loading continues.
- `DataInitializer` registers every loaded ACTIVE reservation in its charger's schedule; a reservation that
  conflicts with an earlier one is skipped and reported too.
- A missing file or I/O error raises `DataLoadException`; `Application` prints the reason and exits with code 1.
- The mappers decide which subclass to create (`AcCharger`/`DcFastCharger`, `FlatPricing`/`TimeOfUsePricing`);
  there are no hard-coded domain objects in the main code.

After loading, the program prints how many objects were loaded and lists any skipped lines.

## How SOLID is applied

- **S — Single responsibility.** `ChargerSchedule` only manages one charger's time slots; `CancellationPolicy`
  only decides fees; `CsvLoader` only reads lines while each mapper only converts one kind of row.
- **O — Open/closed.** A new tariff (e.g. a weekend tariff) is a new `PricingPolicy` class plus one line in
  `StationMapper`; no service changes. A new selection strategy is one class plus one entry in `Application`;
  the menu builds its choices from the map it receives.
- **L — Liskov substitution.** Wherever a `Charger` is expected, an `AcCharger` or `DcFastCharger` works:
  both honour the `powerAt` contract (positive power for a compatible vehicle), so `estimateDuration`,
  the schedule and `findBestCharger` work with any subclass.
- **I — Interface segregation.** `Repository` has just three methods; only reservations need extra queries,
  so they get their own `ReservationRepository` instead of forcing `findByCharger` onto every repository.
  `PricingPolicy` and `ChargerSelectionStrategy` each have a single job.
- **D — Dependency inversion.** `ReservationService` depends on the `Repository` / `ReservationRepository`
  interfaces declared in the domain and on an injected `Clock`; `Application` decides to plug in the
  in-memory implementations and the system clock (tests plug in a fixed clock).

## Simplifications

- Time-of-use pricing assumes the energy flows evenly over the slot; the slot is split into 15-minute blocks
  and each block is priced by its start time (documented in `TimeOfUsePricing`).
- Data lives in memory only; changes are not written back to the CSV files.
- Completing a reservation frees the rest of its slot on the charger.
