-- ============================================================
-- init.sql
-- Auto-generated from application.properties
-- Runs automatically on first MySQL container startup.
-- ============================================================

-- Create database
CREATE DATABASE IF NOT EXISTS ${MYSQL_DATABASE}
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Create user
CREATE USER IF NOT EXISTS '${MYSQL_USER}'@'%' IDENTIFIED BY '${MYSQL_PASSWORD}';

-- Grant privileges
GRANT ALL PRIVILEGES ON ${MYSQL_DATABASE}.* TO '${MYSQL_USER}'@'%';

FLUSH PRIVILEGES;

USE ${MYSQL_DATABASE};

-- ============================================================
-- Drop legacy camelCase columns
-- (created by old Hibernate naming strategy,
--  replaced by snake_case via CamelCaseToUnderscoresNamingStrategy)
-- ============================================================

-- users
ALTER TABLE IF EXISTS users DROP COLUMN IF EXISTS applicationStatus;
ALTER TABLE IF EXISTS users DROP COLUMN IF EXISTS approvedAt;
ALTER TABLE IF EXISTS users DROP COLUMN IF EXISTS batchYear;
ALTER TABLE IF EXISTS users DROP COLUMN IF EXISTS coverNote;
ALTER TABLE IF EXISTS users DROP COLUMN IF EXISTS createdAt;
ALTER TABLE IF EXISTS users DROP COLUMN IF EXISTS dateOfBirth;
ALTER TABLE IF EXISTS users DROP COLUMN IF EXISTS firstName;
ALTER TABLE IF EXISTS users DROP COLUMN IF EXISTS lastName;
ALTER TABLE IF EXISTS users DROP COLUMN IF EXISTS linkedinUrl;
ALTER TABLE IF EXISTS users DROP COLUMN IF EXISTS registrationStatus;
ALTER TABLE IF EXISTS users DROP COLUMN IF EXISTS rollNumber;
ALTER TABLE IF EXISTS users DROP COLUMN IF EXISTS updatedAt;

-- resumes
ALTER TABLE IF EXISTS resumes DROP COLUMN IF EXISTS originalFileName;
ALTER TABLE IF EXISTS resumes DROP COLUMN IF EXISTS storedFileName;
ALTER TABLE IF EXISTS resumes DROP COLUMN IF EXISTS filePath;
ALTER TABLE IF EXISTS resumes DROP COLUMN IF EXISTS fileType;
ALTER TABLE IF EXISTS resumes DROP COLUMN IF EXISTS fileSize;
ALTER TABLE IF EXISTS resumes DROP COLUMN IF EXISTS uploadedAt;
ALTER TABLE IF EXISTS resumes DROP COLUMN IF EXISTS updatedAt;

-- jobs
ALTER TABLE IF EXISTS jobs DROP COLUMN IF EXISTS applicationDeadline;
ALTER TABLE IF EXISTS jobs DROP COLUMN IF EXISTS companyName;
ALTER TABLE IF EXISTS jobs DROP COLUMN IF EXISTS createdAt;
ALTER TABLE IF EXISTS jobs DROP COLUMN IF EXISTS eligibleBatches;
ALTER TABLE IF EXISTS jobs DROP COLUMN IF EXISTS eligibleBranches;
ALTER TABLE IF EXISTS jobs DROP COLUMN IF EXISTS jobType;
ALTER TABLE IF EXISTS jobs DROP COLUMN IF EXISTS minimumCgpa;
ALTER TABLE IF EXISTS jobs DROP COLUMN IF EXISTS numberOfRounds;
ALTER TABLE IF EXISTS jobs DROP COLUMN IF EXISTS salaryPackage;
ALTER TABLE IF EXISTS jobs DROP COLUMN IF EXISTS totalPositions;
ALTER TABLE IF EXISTS jobs DROP COLUMN IF EXISTS updatedAt;

-- job_applications
ALTER TABLE IF EXISTS job_applications DROP COLUMN IF EXISTS appliedAt;
ALTER TABLE IF EXISTS job_applications DROP COLUMN IF EXISTS aptitudeCleared;
ALTER TABLE IF EXISTS job_applications DROP COLUMN IF EXISTS aptitudeScore;
ALTER TABLE IF EXISTS job_applications DROP COLUMN IF EXISTS currentRound;
ALTER TABLE IF EXISTS job_applications DROP COLUMN IF EXISTS finalSelected;
ALTER TABLE IF EXISTS job_applications DROP COLUMN IF EXISTS interviewDate;
ALTER TABLE IF EXISTS job_applications DROP COLUMN IF EXISTS interviewMode;
ALTER TABLE IF EXISTS job_applications DROP COLUMN IF EXISTS interviewNotes;
ALTER TABLE IF EXISTS job_applications DROP COLUMN IF EXISTS interviewResult;
ALTER TABLE IF EXISTS job_applications DROP COLUMN IF EXISTS interviewTime;
ALTER TABLE IF EXISTS job_applications DROP COLUMN IF EXISTS updatedAt;

-- interview_rounds
ALTER TABLE IF EXISTS interview_rounds DROP COLUMN IF EXISTS createdAt;
ALTER TABLE IF EXISTS interview_rounds DROP COLUMN IF EXISTS interviewMode;
ALTER TABLE IF EXISTS interview_rounds DROP COLUMN IF EXISTS roundNumber;
ALTER TABLE IF EXISTS interview_rounds DROP COLUMN IF EXISTS roundType;
ALTER TABLE IF EXISTS interview_rounds DROP COLUMN IF EXISTS scheduledDate;
ALTER TABLE IF EXISTS interview_rounds DROP COLUMN IF EXISTS scheduledTime;
ALTER TABLE IF EXISTS interview_rounds DROP COLUMN IF EXISTS updatedAt;