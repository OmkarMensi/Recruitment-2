USE git_recruitment;

-- Run this once after switching Hibernate to CamelCaseToUnderscoresNamingStrategy.
-- These camelCase columns were created by the previous naming strategy and are
-- duplicates of the snake_case columns now used by the application.

ALTER TABLE users DROP COLUMN IF EXISTS applicationStatus;
ALTER TABLE users DROP COLUMN IF EXISTS approvedAt;
ALTER TABLE users DROP COLUMN IF EXISTS batchYear;
ALTER TABLE users DROP COLUMN IF EXISTS coverNote;
ALTER TABLE users DROP COLUMN IF EXISTS createdAt;
ALTER TABLE users DROP COLUMN IF EXISTS dateOfBirth;
ALTER TABLE users DROP COLUMN IF EXISTS firstName;
ALTER TABLE users DROP COLUMN IF EXISTS lastName;
ALTER TABLE users DROP COLUMN IF EXISTS linkedinUrl;
ALTER TABLE users DROP COLUMN IF EXISTS registrationStatus;
ALTER TABLE users DROP COLUMN IF EXISTS rollNumber;
ALTER TABLE users DROP COLUMN IF EXISTS updatedAt;

ALTER TABLE resumes DROP COLUMN IF EXISTS originalFileName;
ALTER TABLE resumes DROP COLUMN IF EXISTS storedFileName;
ALTER TABLE resumes DROP COLUMN IF EXISTS filePath;
ALTER TABLE resumes DROP COLUMN IF EXISTS fileType;
ALTER TABLE resumes DROP COLUMN IF EXISTS fileSize;
ALTER TABLE resumes DROP COLUMN IF EXISTS uploadedAt;
ALTER TABLE resumes DROP COLUMN IF EXISTS updatedAt;

ALTER TABLE jobs DROP COLUMN IF EXISTS applicationDeadline;
ALTER TABLE jobs DROP COLUMN IF EXISTS companyName;
ALTER TABLE jobs DROP COLUMN IF EXISTS createdAt;
ALTER TABLE jobs DROP COLUMN IF EXISTS eligibleBatches;
ALTER TABLE jobs DROP COLUMN IF EXISTS eligibleBranches;
ALTER TABLE jobs DROP COLUMN IF EXISTS jobType;
ALTER TABLE jobs DROP COLUMN IF EXISTS minimumCgpa;
ALTER TABLE jobs DROP COLUMN IF EXISTS numberOfRounds;
ALTER TABLE jobs DROP COLUMN IF EXISTS salaryPackage;
ALTER TABLE jobs DROP COLUMN IF EXISTS totalPositions;
ALTER TABLE jobs DROP COLUMN IF EXISTS updatedAt;

ALTER TABLE job_applications DROP COLUMN IF EXISTS appliedAt;
ALTER TABLE job_applications DROP COLUMN IF EXISTS aptitudeCleared;
ALTER TABLE job_applications DROP COLUMN IF EXISTS aptitudeScore;
ALTER TABLE job_applications DROP COLUMN IF EXISTS currentRound;
ALTER TABLE job_applications DROP COLUMN IF EXISTS finalSelected;
ALTER TABLE job_applications DROP COLUMN IF EXISTS interviewDate;
ALTER TABLE job_applications DROP COLUMN IF EXISTS interviewMode;
ALTER TABLE job_applications DROP COLUMN IF EXISTS interviewNotes;
ALTER TABLE job_applications DROP COLUMN IF EXISTS interviewResult;
ALTER TABLE job_applications DROP COLUMN IF EXISTS interviewTime;
ALTER TABLE job_applications DROP COLUMN IF EXISTS updatedAt;

ALTER TABLE interview_rounds DROP COLUMN IF EXISTS createdAt;
ALTER TABLE interview_rounds DROP COLUMN IF EXISTS interviewMode;
ALTER TABLE interview_rounds DROP COLUMN IF EXISTS roundNumber;
ALTER TABLE interview_rounds DROP COLUMN IF EXISTS roundType;
ALTER TABLE interview_rounds DROP COLUMN IF EXISTS scheduledDate;
ALTER TABLE interview_rounds DROP COLUMN IF EXISTS scheduledTime;
ALTER TABLE interview_rounds DROP COLUMN IF EXISTS updatedAt;