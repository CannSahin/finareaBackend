FinArea: AI-Powered Personal Finance Tracking & Budget Planning Application


🚀 Project Overview
FinArea is an AI-powered personal finance application designed to help users understand, track, and improve their spending habits. It provides intelligent solutions for individuals who want to gain full visibility over their financial behavior and make smarter decisions about their money.
By analyzing bank statements and transaction data, FinArea automatically categorizes expenses into clear and understandable groups such as groceries, dining, subscriptions, and bills. It leverages advanced natural language processing models, including Google Gemini, OpenAI GPT-4, and DeepSeek, to interpret financial data and produce personalized budget recommendations in natural language. These insights guide users on how to better manage their money from month to month.
The main purpose of FinArea is to simplify personal budgeting by eliminating manual tracking and replacing it with intelligent automation. With FinArea, users no longer need to rely on spreadsheets or generic budget templates. Instead, they receive tailored forecasts and recommendations based on their actual spending behavior. This makes budgeting more precise, relevant, and sustainable in the long term.
FinArea is especially suitable for:
•	University students who want to track daily expenses and avoid overspending,
•	Young professionals aiming to save more efficiently,
•	Families who seek a consolidated view of household expenses.
What makes FinArea unique is its clean and interactive dashboard that presents visual breakdowns of spending through bar charts, pie charts, and trend graphs. Additionally, it offers the ability to set custom budget goals and receive alerts if spending exceeds those limits. The application also supports encrypted data uploads (e.g., CSV, PDF) and ensures user privacy by following best practices in data security.
In the future, FinArea aims to integrate with live banking APIs (like Plaid) to automate statement retrieval, add debt tracking, and provide investment overviews — transforming it from a budgeting tool into a full-scale financial wellness platform.
With FinArea, personal finance becomes smarter, simpler, and more personalized than ever.

This backend automates this process by:
-Parsing PDF Bank Statements: Users can upload their bank statements in PDF format.
-Extracting Data with AI: The system extracts text from the PDF and uses a dynamically selectable AI model (like Google's Vertex AI Gemini or OpenAI's GPT) to intelligently parse transaction details and the statement period.
-Securely Storing Data: All financial data is categorized and stored securely in a PostgreSQL database, linked to the user's account.
-Providing Insights: It exposes a RESTful API for the frontend to query detailed, period-based financial summaries and user information.

✨ Key Features
Secure User Management: Full JWT-based authentication flow (Register, Login, Update Profile/Email/Password).
PDF Statement Processing: Upload and parse PDF bank statements using Apache PDFBox.
Dynamic AI Provider Selection: Choose the AI model (e.g., GEMINI, OPENAI) at runtime via an API parameter for flexible and cost-effective data extraction.
Intelligent Data Extraction with Spring AI: Leverages Spring AI to send prompts to LLMs and receive structured JSON data containing period and transaction information.
RESTful API: A well-structured API for managing users, periods, and financial data.
API Documentation: Integrated Swagger UI for easy API exploration and testing.
Period-Based Summaries: Provides aggregated financial summaries grouped by source (e.g., credit card, cash) and expense category.

🛠️ Tech Stack
Framework: Spring Boot 3.x
Language: Java 21
Security: Spring Security 6 (JWT Authentication)
Data: Spring Data JPA, Hibernate
Database: PostgreSQL
AI Integration: Spring AI (Vertex AI Gemini, OpenAI)
PDF Parsing: Apache PDFBox
Build Tool: Maven
API Documentation: SpringDoc OpenAPI (Swagger UI)
Utilities: Lombok

⚙️ Setup and Installation
Follow these steps to get the backend running on your local machine.
Prerequisites
Java 21: Make sure you have JDK 21 installed.
Maven 3.8+: For building the project.
PostgreSQL: A running instance of PostgreSQL.
Google Cloud SDK (gcloud CLI): Required for Vertex AI authentication.
An OpenAI API Key (optional, if you want to use the OpenAI provider).
1. Clone the Repository
-Generated bash
   git clone [BACKEND_REPO_URL]
   cd finareaBackend
2. Configure the Database
Create a new PostgreSQL database (e.g., finarea_db).
Update the spring.datasource.* properties in src/main/resources/application.properties with your database URL, username, and password.
3. Set Project ID: Configure your GCP Project ID in application.properties:
-Generated properties
spring.ai.vertex.ai.project-id=your-gcp-project-id
spring.ai.vertex.ai.location=us-central1
4. Configure AI Services
This project supports multiple AI providers. You need to configure credentials for the ones you intend to use.
For Google Vertex AI Gemini (Recommended Method)
Enable Vertex AI API: Go to your Google Cloud Console and ensure the "Vertex AI API" is enabled for your project.
Authenticate with gcloud: Run the following command in your terminal and follow the browser prompts to log in with your Google account. This account must have the "Vertex AI User" IAM role in your GCP project.
-Generated bash
gcloud auth application-default login

For OpenAI
Set your OpenAI API key in application.properties. It is highly recommended to use an environment variable instead of hardcoding the key.
Generated properties
spring.ai.openai.api-key=${OPENAI_API_KEY}

4. Build and Run the Application
Build the project using Maven:
-Generated bash
mvn clean install

Run the application:
-Generated bash
mvn spring-boot:run

📖 API Documentation
Once the application is running, you can access the interactive API documentation (Swagger UI) at:
http://localhost:8080/swagger-ui/index.html
