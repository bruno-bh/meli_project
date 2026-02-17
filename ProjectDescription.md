# QUESTION DESCRIPTION

## Item Comparison - V2

### Objective

Build a backend API that supplies product details for use in an item comparison feature. Your implementation should follow established backend best practices, providing clear and efficient endpoints to retrieve the required data for product comparisons.

### Requirements

**Backend: API Development**

**API Endpoint:**

* Build a RESTful API that returns details for multiple items to be compared.
* The API should provide fields such as product name, image URL, description, price, rating, and specifications.
* Include basic error handling and inline comments to explain your logic.

**Stack:**

* You can use any backend technology or framework of your choice.
* Simulate data persistence using local JSON/CSV files or an in-memory database (e.g., SQLite, H2 Database) to represent the inventory. A real database is not required.

---

### Function requirements

The product model should encapsulate essential information, including but not limited to the following attributes: ID, name, description, price, size, weight, and color. Additionally, certain products may require specialized information. For example, a smartphone should include specific details such as battery capacity, camera specifications, memory, storage capacity, brand, model version, and operating system.

A user should be able to query specific comparisons between items and ignore other fields. This will help them focus on the most relevant details for their analysis.

### Non-functional requirements

Special consideration will be given to good practices in error handling, documentation, testing, and any other relevant non-functional aspects you choose to demonstrate.

### Documentation & strategic overview

Please include a brief README or Diagram (optional) that explains your API design, main endpoints, setup instructions, and any key architectural decisions you made during development.