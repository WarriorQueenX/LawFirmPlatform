# Lexora — Integrated Digital Law Firm Management and Client Consultation Platform

Lexora is a database-driven web application designed to streamline law firm operations by organizing client records, lawyer information, legal cases, appointments, documents, branches, and notifications in a centralized platform.

The project demonstrates the practical application of Database Management System (DBMS) concepts through a web-based interface, a Java backend, and an Oracle relational database.

## Project Objectives

- Centralize law firm records in a structured relational database.
- Manage client and lawyer information efficiently.
- Support legal case management and appointment booking.
- Maintain relationships between clients, lawyers, cases, and other records.
- Implement authentication and role-based access control.
- Demonstrate database operations through a functional web application.

## Technology Stack

| Component | Technology |
|---|---|
| Frontend | HTML5, CSS3, JavaScript |
| Backend | Java 21 |
| HTTP Server | Java `HttpServer` |
| Database | Oracle Database |
| Database Connectivity | JDBC |
| Build and Dependency Management | Apache Maven |
| Development Environment | IntelliJ IDEA |
| Version Control | Git and GitHub |

## Core Database Entities

The database design includes the following entities:

1. **BRANCH** — Stores information about law firm branches.
2. **LAWYER** — Maintains lawyer records and professional information.
3. **CLIENT** — Stores client information.
4. **LEGAL_CASE** — Maintains legal case details, including category, budget, status, and complexity.
5. **CASE_TEAM** — Represents the association between legal cases and lawyers.
6. **APPOINTMENT** — Stores client consultation appointments with lawyers.
7. **NOTIFICATION** — Supports notification-related records.
8. **DOCUMENT** — Maintains document-related records associated with the law firm.

Primary keys, foreign keys, and other database constraints help maintain data integrity and relationships between entities.

## Application Features

### 1. Authentication and Authorization
- User login and logout.
- Session-based authentication.
- Role-based access for administrators, branch managers, lawyers, and clients.
- Backend authorization checks for protected endpoints.

### 2. Client Management
- Manage client records through the application.
- Support client-related database operations according to user permissions.

### 3. Lawyer Management
- Maintain lawyer records.
- Provide access to the lawyer directory.
- Support lawyer account authentication.

### 4. Legal Case Management
- Create and view legal case records.
- Update and delete cases for authorized roles.
- Restrict lawyers to viewing cases assigned to them.
- Protect case-management operations using backend authorization.

### 5. Appointment Management
- Book client consultations with lawyers.
- Display appointment information.
- Apply role-based restrictions to appointment-related information and operations.

### 6. Additional Modules
The application also includes sections for branches, documents, notifications, and lawyer recommendations.

The availability of individual operations depends on the module's implementation and the authenticated user's permissions.

## System Architecture

The application follows a layered architecture:

```text
User
 |
 v
Frontend
HTML + CSS + JavaScript
 |
 v
Java HTTP Server
 |
 v
Request Handlers
 |
 v
Data Access Objects (DAO)
 |
 v
JDBC
 |
 v
Oracle Database
```

**Frontend:** Collects user input and displays application data.

**Java HTTP Server:** Receives HTTP requests and routes them to the appropriate handlers.

**Request Handlers:** Process requests, validate input, and enforce access-control rules.

**Data Access Objects (DAO):** Encapsulate database operations using SQL and JDBC.

**Oracle Database:** Stores persistent application data and enforces relational constraints.

## Project Structure

The project follows a Maven-based Java structure. The principal application components include:

```text
LawFirmPlatform/
├── pom.xml
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── lawfirm/
│                   ├── Main.java
│                   ├── controller/
│                   ├── dao/
│                   └── util/
├── frontend/
│   ├── index.html
│   ├── style.css
│   └── app.js
└── README.md
```

Additional Java packages and database scripts may be present in the complete project.

## Prerequisites

Before running the application, ensure that you have:

- Java Development Kit (JDK) 21.
- Apache Maven.
- Oracle Database XE or a compatible Oracle Database installation.
- The required database schema, tables, sequences, and constraints.
- Valid database connection settings configured for the application.

## How to Run

### 1. Clone the Repository

```bash
git clone https://github.com/WarriorQueenX/LawFirmPlatform.git
cd LawFirmPlatform
```

### 2. Configure the Database

Start Oracle Database and ensure that the project schema has been created.

Configure the database connection settings used by `DBConnection.java` for your Oracle instance. Use your own database credentials and keep them out of public source control.

### 3. Build the Application

```bash
mvn clean package
```

### 4. Start the Backend

Run the application's main class, `com.lawfirm.Main`, from IntelliJ IDEA or your configured Java runtime.

The application uses HTTP port `8082` in the current development configuration. Ensure that the port is available.

### 5. Open the Frontend

Open the application through the local server's configured frontend route. If the frontend is served separately, configure it to communicate with the Java backend at the appropriate address.

## Database Management System Concepts

This project demonstrates the practical use of:

- Relational database design.
- Primary key and foreign key constraints.
- Entity relationships.
- SQL-based data retrieval and manipulation.
- JDBC connectivity.
- Data integrity and referential integrity.
- CRUD operations.
- Parameterized SQL queries.
- Authentication and authorization at the application layer.

## Security Considerations

The application incorporates password hashing and role-based access controls.

For a production deployment, further security measures would be required, including secure secret management, HTTPS, stronger session protections, comprehensive input validation, audit logging, and appropriate access controls for confidential legal information.

Demo credentials should not be reused in production.

## Project Purpose

Lexora was developed as an academic DBMS project to demonstrate how relational database concepts can be integrated with a Java backend and a web-based frontend to address a practical law firm management problem.

## Repository

GitHub: https://github.com/WarriorQueenX/LawFirmPlatform
