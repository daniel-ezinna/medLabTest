Sante Diagnostics LIMS
Course: CSC310 Examination Project
Students: Udochukwu Ezinna, Ibe Onyinyechukwu, Victoria Falowo

PROJECT SETUP INSTRUCTIONS:

1. Database Configuration:
   - Open pgAdmin and create a new database named: medlabapp
   - Open the Query Tool for this database and run the provided 'schema.sql' file to create the tables and insert the sample data.
   
2. Application Configuration (IMPORTANT):
   - Open src/com/medlabapp/config/DatabaseConnection.java
   - Change the PASSWORD constant (currently set to "daniel") to match your local PostgreSQL password.

3. Dependencies:
   - All required libraries (PostgreSQL JDBC, jBCrypt, Jakarta Mail) are included in the dist/lib/ directory. If running via NetBeans, the project should resolve these automatically.

4. Default Test Accounts:
   - Super Admin: admin@sante.com
   - Lab Attendant: m.adebayo@sante.com
   - Customer: itzudochukwu@gmail.com
   - Password for ALL test accounts: Admin123!