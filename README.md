# ⬡ Sante Diagnostics LIMS

A robust, JavaFX-based Laboratory Information Management System (LIMS) engineered to digitalize medical laboratory operations. This desktop application replaces paper-based workflows by providing secure, role-based portals for administrators, laboratory staff, and patients. 

Built with a focus on administrative governance, operational precision, and customer transparency.

## 🚀 Features by Role

### 1. Super Admin (Governance)
* **Test Catalog Management:** Dynamically create and configure diagnostic test types (pricing, turnaround time, and result formats like PDF/Image).
* **User Provisioning:** Create staff and patient accounts with forced password-reset flags for initial login security.
* **Immutable Audit Trail:** View a system-wide, tamper-proof log of all critical database operations, state changes, and logins.

### 2. Lab Attendant (Operations)
* **Sample Lifecycle Tracking:** Advance test requests through `COLLECTED` → `PROCESSING` → `VALIDATED` stages.
* **Clinical Result Uploads:** Attach and associate PDF medical reports and medical imaging directly to patient records.
* **Verification Gate:** Manually verify validated results to instantly release them to the patient portal and trigger automated email notifications.
* **Payment Processing:** Confirm and mark pending test requests as `PAID`.

### 3. Patient / Customer (Transparency)
* **Self-Service Onboarding:** Register via the desktop app with automated SMTP email verification (6-digit expiring OTP).
* **Live Dashboard:** Track active orders with a real-time countdown timer reflecting the remaining turnaround time (TAT) for pending results.
* **Order Processing:** Browse the lab's test catalog, place diagnostic orders, and receive bank transfer instructions.
* **Secure Result Vault:** Download validated PDF reports and view medical images directly from the desktop client.

## 🛠️ Tech Stack

* **Language:** Java 17+
* **GUI Framework:** JavaFX (with custom modern CSS styling)
* **Database:** PostgreSQL (JDBC)
* **Security:** jBCrypt (Password Hashing)
* **Notifications:** Jakarta Mail API (SMTP)
* **Architecture:** MVC (Model-View-Controller) with DAO pattern for data persistence
* **Build System:** Apache Ant

## ⚙️ Local Setup & Installation

### Prerequisites
* JDK 17 or higher
* PostgreSQL installed and running locally
* Apache Ant (or an IDE like NetBeans/IntelliJ that supports Ant builds)

### 1. Clone the Repository
```bash
git clone [https://github.com/YOUR-USERNAME/sante-diagnostics-lims.git](https://github.com/YOUR-USERNAME/sante-diagnostics-lims.git)
cd sante-diagnostics-lims
